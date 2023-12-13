import json
import os
import time

import httpx
import redis
from uhashring import HashRing
import datetime
import logging

from fastapi import FastAPI, Depends, HTTPException
from fastapi.encoders import jsonable_encoder
from httpx import ConnectError

from schemas import RequestRide, RequestRideResponse, ChangeRideState, Availability, CompleteRide, NewDriver, Driver, \
    NewPassenger, Passenger
from datetime import timedelta
from logging.config import dictConfig
from apscheduler.schedulers.background import BackgroundScheduler

HEADERS = {"Content-Type": "application/json"}

service_counters = {"rides": 0, "drivers": 0}

log_config = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "default": {
            "()": "uvicorn.logging.DefaultFormatter",
            "fmt": "%(levelprefix)s %(asctime)s | %(message)s",
            "datefmt": "%H:%M:%S",

        },
    },
    "handlers": {
        "default": {
            "formatter": "default",
            "class": "logging.StreamHandler",
            "stream": "ext://sys.stderr",
        },
    },
    "loggers": {
        "foo-logger": {"handlers": ["default"], "level": "DEBUG"},
    },
}

dictConfig(log_config)
logger = logging.getLogger('foo-logger')

app = FastAPI()

REQUEST_TIMEOUT = os.getenv('REQUEST_TIMEOUT') or 1
MAX_FAILURES = os.getenv('MAX_FAILURES') or 3
MAX_REROUTES = os.getenv('MAX_REROUTES') or 3


# @app.middleware('http')
# async def timeout_middleware(request, call_next):
#     try:
#         return await asyncio.wait_for(call_next(request), timeout=REQUEST_TIMEOUT)
#     except asyncio.TimeoutError:
#         return JSONResponse(status_code=408, content={"Detail": "Request timeout"})


class CircuitBreaker:
    def __init__(self, max_failures=MAX_FAILURES, timeout=30):
        self.max_failures = max_failures
        self.timeout = timeout * 3.5
        self.host_failures = {}
        self.host_last_failure_time = {}

    def record_failure(self, host):
        if host not in self.host_failures:
            self.host_failures[host] = 1
            self.host_last_failure_time[host] = time.time()
        else:
            self.host_failures[host] += 1
            self.host_last_failure_time[host] = time.time()

    def is_open(self, host):
        if host in self.host_failures:
            if self.host_failures[host] < self.max_failures:
                return False
            if time.time() - self.host_last_failure_time[host] > self.timeout:
                del self.host_failures[host]
                del self.host_last_failure_time[host]
                return False
            else:
                del self.host_failures[host]
                del self.host_last_failure_time[host]
                return True
        else:
            return False


def check_service_status(service_url, service):
    try:
        response = httpx.get(f"http://{service_url}/api/{service}/health")
        return response.status_code == 200
    except Exception:
        return False


class Coordinator:
    def __init__(self, service1_url, service2_url):
        self.service1_url = service1_url
        self.service2_url = service2_url

    def check_before_commit(self):
        service1_status = check_service_status(self.service1_url, "rides")
        service2_status = check_service_status(self.service2_url, "drivers")

        if service1_status and service2_status:
            return True
        else:
            return False


circuit_breaker = CircuitBreaker()

REDIS_HOST = os.getenv('REDIS_HOST') or "localhost"
REDIS_PORT = os.getenv('REDIS_PORT') or 6379


def create_redis(host, port):
    return redis.ConnectionPool(
        host=host,
        port=port,
        db=0,
        decode_responses=True
    )


pool_1 = create_redis("redis-node-1", 7000)
pool_2 = create_redis("redis-node-2", 7001)
pool_3 = create_redis("redis-node-3", 7002)

nodes = {
    'node1': {
        'hostname': 'node-1',
        'instance': redis.Redis(connection_pool=pool_1),
        'port': 7000
    },
    'node2': {
        'hostname': 'node-2',
        'instance': redis.Redis(connection_pool=pool_2),
        'port': 7001,
    },
    'node3': {
        'hostname': 'node-3',
        'instance': redis.Redis(connection_pool=pool_3),
        'port': 7002
    }
}

hr = HashRing(nodes)


def get_redis():
    return hr


def send_heartbeat():
    nodes_to_delete = []
    for node in nodes:
        try:
            nodes[node]['instance'].ping()
        except Exception:
            logger.warning("A Redis instance is down!")
            get_redis().remove_node(node)
            nodes_to_delete.append(node)
    for i in nodes_to_delete:
        del nodes[i]


@app.on_event('startup')
def init_data():
    scheduler = BackgroundScheduler()
    scheduler.add_job(send_heartbeat, 'cron', second='*/1')
    scheduler.start()


instances_init = {"rides": [{"host": "localhost", "port": 3030, "identifier": "rides_1"},
                            {"host": "localhost", "port": 3031, "identifier": "rides_2"},
                            {"host": "localhost", "port": 3032, "identifier": "rides_3"},
                            {"host": "localhost", "port": 3033, "identifier": "rides_4"}],

                  "drivers": [{"host": "localhost", "port": 7070, "identifier": "drivers_1"},
                              {"host": "localhost", "port": 7071, "identifier": "drivers_2"},
                              {"host": "localhost", "port": 7072, "identifier": "drivers_3"},
                              {"host": "localhost", "port": 7073, "identifier": "drivers_3"}]}


# Load Balancer Round Robin
def get_instance_after_round_robin(service_name: str):
    instances = instances_init[service_name]

    counter = service_counters.get(service_name)
    instance_index = counter % len(instances)
    service_counters[service_name] = (counter + 1) % len(instances)

    instance_info = instances[instance_index]

    return "host.docker.internal", instance_info["port"], instance_info["identifier"]


@app.get("/rides/{ride_id}")
async def get_ride(ride_id: int, cache=Depends(get_redis)):
    if (cached_ride := cache[f"ride_{ride_id}"].get(f"ride_{ride_id}")) is not None:
        return json.loads(cached_ride)
    else:
        reroute_attempt = 0

        while reroute_attempt < MAX_REROUTES:
            host, port, service_identifier = get_instance_after_round_robin("rides")

            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            try:
                r = httpx.get(f"http://{host}:{port}/api/rides/{ride_id}")

                if r.status_code == 500 or r.status_code == 408:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')
                    continue
                elif r.status_code == 200:
                    cache[f"ride_{ride_id}"].set(f"ride_{ride_id}", json.dumps(r.json()), ex=timedelta(minutes=1))
                    return r.json()
                elif r.status_code == 404:
                    raise HTTPException(status_code=404, detail="No resource found with such ID")
                else:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

            except ConnectError:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

        logger.error('Maximum reroutes has been reached.')
        raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with ["
                                                    "RIDES] service")


@app.get("/passengers/{passenger_id}")
async def get_passenger(passenger_id: int, cache=Depends(get_redis)):
    reroute_attempt = 0

    if (cached_passenger := cache.get(f"passenger_{passenger_id}")) is not None:
        return json.loads(cached_passenger)
    else:
        while reroute_attempt < MAX_REROUTES:
            host, port, service_identifier = get_instance_after_round_robin("passengers")

            if (cached_passenger := cache.get(f"passenger_{passenger_id}")) is not None:
                return json.loads(cached_passenger)
            else:
                try:
                    if circuit_breaker.is_open(service_identifier):
                        raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

                    r = httpx.get(f"http://{host}:{port}/api/passengers/{passenger_id}")

                    if r.status_code == 500 or r.status_code == 408:
                        circuit_breaker.record_failure(service_identifier)
                        reroute_attempt += 1
                        logger.error(
                            f'Call to service [rides] failed. Rerouting... {service_identifier}')
                        continue
                    elif r.status_code == 200:
                        cache.set(f"passenger_{passenger_id}", json.dumps(r.json()), ex=timedelta(minutes=1))
                        return r.json()
                    elif r.status_code == 404:
                        raise HTTPException(status_code=404, detail="No resource found with such ID")
                    else:
                        circuit_breaker.record_failure(service_identifier)
                        reroute_attempt += 1
                        logger.error(
                            f'Call to service [rides] failed. Rerouting... {service_identifier}')

                except ConnectError:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

        logger.error('Maximum reroutes has been reached.')
        raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with ["
                                                    "RIDES] service")


@app.post("/passengers")
async def create_passenger(newPassenger: NewPassenger) -> Passenger | dict:
    reroute_attempt = 0

    while reroute_attempt < MAX_REROUTES:
        host, port, service_identifier = get_instance_after_round_robin("rides")

        try:
            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            r = httpx.post(f"http://{host}:{port}/api/rides/passengers", json=jsonable_encoder(newPassenger),
                           headers=HEADERS)
            if r.status_code == 500 or r.status_code == 408:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')
                continue
            elif r.status_code == 200:
                return r.json()
            else:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

        except ConnectError:
            circuit_breaker.record_failure(service_identifier)
            reroute_attempt += 1
            logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

    logger.error('Maximum reroutes has been reached.')
    raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with [RIDES] "
                                                "service")


@app.post("/rides")
async def request_ride(requestRide: RequestRide) -> RequestRideResponse | dict:
    host_rides, port_rides, __service_identifier = get_instance_after_round_robin("rides")
    host_drivers, port_drivers, __service_identifier = get_instance_after_round_robin("drivers")

    coordinator = Coordinator(f"{host_rides}:{port_rides}", f"{host_drivers}:{port_drivers}")

    if coordinator.check_before_commit():

        reroute_attempt = 0

        while reroute_attempt < MAX_REROUTES:
            host, port, service_identifier = get_instance_after_round_robin("rides")

            try:
                if circuit_breaker.is_open(service_identifier):
                    raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

                r = httpx.post(f"http://{host}:{port}/api/rides", json=jsonable_encoder(requestRide), headers=HEADERS)

                if r.status_code == 500 or r.status_code == 408:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')
                    continue
                elif r.status_code == 200:
                    return r.json()
                else:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

            except ConnectError:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error(f'Call to service [rides] failed. Rerouting... {service_identifier}')

        logger.error('Maximum reroutes has been reached.')
        raise HTTPException(status_code=500,
                            detail="Maximum reroutes has been reached. Something is broken with [RIDES] service")

    else:
        raise HTTPException(status_code=500, detail="2 Phase Commit FAILED... At least one database is DOWN")


@app.get("/drivers/{driver_id}")
async def get_driver(driver_id: int, cache=Depends(get_redis)):
    reroute_attempt = 0

    if (cached_driver := cache.get(f"driver_{driver_id}")) is not None:
        return json.loads(cached_driver)
    else:
        while reroute_attempt < MAX_REROUTES:
            host, port, service_identifier = get_instance_after_round_robin("rides")

            try:
                if circuit_breaker.is_open(service_identifier):
                    raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

                r = httpx.get(f"http://{host}:{port}/api/drivers/{driver_id}")

                if r.status_code == 500 or r.status_code == 408:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error('Call to service [DRIVERS] failed. Rerouting...')
                    continue
                elif r.status_code == 200:
                    cache.set(f"driver_{driver_id}", json.dumps(r.json()), ex=timedelta(minutes=1))
                    return r.json()
                elif r.status_code == 404:
                    raise HTTPException(status_code=404, detail="No resource found with such ID")
                else:
                    circuit_breaker.record_failure(service_identifier)
                    reroute_attempt += 1
                    logger.error('Call to service [DRIVERS] failed. Rerouting...')

            except ConnectError:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')

        logger.error('Maximum reroutes has been reached.')
        raise HTTPException(status_code=500,
                            detail="Maximum reroutes has been reached. Something is broken with [DRIVERS] service")


@app.post("/drivers")
async def create_driver(newDriver: NewDriver) -> Driver | dict:
    reroute_attempt = 0

    while reroute_attempt < MAX_REROUTES:
        host, port, service_identifier = get_instance_after_round_robin("drivers")

        try:
            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            r = httpx.post(f"http://{host}:{port}/api/drivers", json=jsonable_encoder(newDriver), headers=HEADERS)

            if r.status_code == 500 or r.status_code == 408:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')
                continue
            elif r.status_code == 200:
                return r.json()
            else:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')

        except ConnectError:
            circuit_breaker.record_failure(service_identifier)
            reroute_attempt += 1
            logger.error('Call to service [DRIVERS] failed. Rerouting...')

    logger.error('Maximum reroutes has been reached.')
    raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with ["
                                                "DRIVERS] service")


@app.put("/rides/{ride_id}/state")
async def change_ride_state(ride_id: int, state: ChangeRideState, cache=Depends(get_redis)) -> ChangeRideState:
    reroute_attempt = 0

    while reroute_attempt < MAX_REROUTES:
        host, port, service_identifier = get_instance_after_round_robin("rides")

        try:
            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            r = httpx.put(f"http://{host}:{port}/api/rides/{ride_id}/state",
                          json=jsonable_encoder(state), headers=HEADERS)

            if r.status_code == 500 or r.status_code == 408:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [RIDES] failed. Rerouting...')
                continue
            elif r.status_code == 200:
                if (cache.get(f"ride_{ride_id}")) is not None:
                    cache.delete(f"ride_{ride_id}")
                return r.json()
            else:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [RIDES] failed. Rerouting...')

        except ConnectError:
            circuit_breaker.record_failure(service_identifier)
            reroute_attempt += 1
            logger.error('Call to service [RIDES] failed. Rerouting...')

    raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with [RIDES] "
                                                "service")


@app.put("/drivers/{driver_id}/availability")
async def change_driver_availability(driver_id: int, availability: Availability) -> Availability:
    reroute_attempt = 0

    while reroute_attempt < MAX_REROUTES:
        host, port, service_identifier = get_instance_after_round_robin("drivers")

        try:
            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            r = httpx.put(f"http://{host}:{port}/api/drivers/{driver_id}/availability",
                          json=jsonable_encoder(availability), headers=HEADERS)

            if r.status_code == 500 or r.status_code == 408:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')
                continue
            elif r.status_code == 200:
                return r.json()
            else:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')

        except ConnectError:
            circuit_breaker.record_failure(service_identifier)
            reroute_attempt += 1
            logger.error('Call to service [DRIVERS] failed. Rerouting...')

    raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with ["
                                                "DRIVERS] service")


@app.put("/drivers/{driver_id}/ride")
async def complete_ride(driver_id: int, state: CompleteRide):
    reroute_attempt = 0

    while reroute_attempt < MAX_REROUTES:
        host, port, service_identifier = get_instance_after_round_robin("drivers")

        try:
            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            r = httpx.put(f"http://{host}:{port}/api/drivers/{driver_id}/ride",
                          json=jsonable_encoder(state), headers=HEADERS)

            if r.status_code == 500 or r.status_code == 408:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')
                continue
            elif r.status_code == 200:
                return r.json()
            else:
                circuit_breaker.record_failure(service_identifier)
                reroute_attempt += 1
                logger.error('Call to service [DRIVERS] failed. Rerouting...')

        except ConnectError:
            circuit_breaker.record_failure(service_identifier)
            reroute_attempt += 1
            logger.error('Call to service [DRIVERS] failed. Rerouting...')

    raise HTTPException(status_code=500, detail="Maximum reroutes has been reached. Something is broken with ["
                                                "DRIVERS] service")


@app.get("/status", status_code=200)
async def status_endpoint():
    return {"gateway": "ALIVE", "timestamp": datetime.datetime.now()}


@app.get("/redis-cluster", status_code=200)
async def redis_get(cache=Depends(get_redis)):
    for i in range(10):
        key = f'rides_{i}'
        logger.info(f'[{cache.get_node(key)}] -- {key} -- @port {cache.get_node_port(key)}')

    return "Done"


@app.post("/redis-cluster", status_code=200)
async def redis_post(cache=Depends(get_redis)):
    for i in range(10):
        cache[f'rides_{i}'].set(f'rides_{i}', i)
    return "Done"
