import json
import os
import time

import httpx
import redis
import datetime
import logging

from fastapi import FastAPI, Depends, HTTPException
from fastapi.encoders import jsonable_encoder
from httpx import ConnectError
from schemas import RequestRide, RequestRideResponse, ChangeRideState, Availability, CompleteRide
from datetime import timedelta
from logging.config import dictConfig

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


class CircuitBreaker:
    def __init__(self, max_failures=3, timeout=30):
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


circuit_breaker = CircuitBreaker()


REDIS_HOST = os.getenv('REDIS_HOST') or "localhost"
REDIS_PORT = os.getenv('REDIS_PORT') or 6379


def create_redis():
    return redis.ConnectionPool(
        host=REDIS_HOST,
        port=REDIS_PORT,
        db=0,
        decode_responses=True
    )


pool = create_redis()


def get_redis():
    return redis.Redis(connection_pool=pool)


# Load Balancer Round Robin
def get_instance_after_round_robin(service_name: str):
    instances = get_redis().keys(f'service:{service_name}:*')

    counter = service_counters.get(service_name)
    instance_index = counter % len(instances)
    service_counters[service_name] = (counter + 1) % len(instances)

    instance_info = json.loads(get_redis().get(instances[instance_index]))
    return instance_info["host"], instance_info["port"], instances[instance_index]


@app.get("/rides/{ride_id}")
async def get_ride(ride_id: int, cache=Depends(get_redis)):
    host, port, service_identifier = get_instance_after_round_robin("rides")

    if (cached_ride := cache.get(f"ride_{ride_id}")) is not None:
        return json.loads(cached_ride)
    else:
        try:
            if circuit_breaker.is_open(service_identifier):
                raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

            r = httpx.get(f"http://{host}:{port}/api/rides/{ride_id}")

            if r.status_code != 200:
                circuit_breaker.record_failure(service_identifier)
                raise HTTPException(status_code=r.status_code, detail=r.text)
            else:
                cache.set(f"ride_{ride_id}", json.dumps(r.json()), ex=timedelta(minutes=1))
                return r.json()

        except ConnectError:
            circuit_breaker.record_failure(service_identifier)
            raise HTTPException(status_code=500, detail="Request timeout to RIDES service")

@app.post("/rides")
async def request_ride(requestRide: RequestRide) -> RequestRideResponse | dict:
    host, port, service_identifier = get_instance_after_round_robin("rides")

    try:
        if circuit_breaker.is_open(service_identifier):
            raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

        r = httpx.post(f"http://{host}:{port}/api/rides", json=jsonable_encoder(requestRide), headers=HEADERS)

        if r.status_code != 200:
            circuit_breaker.record_failure(service_identifier)
            raise HTTPException(status_code=r.status_code, detail=r.text)
        else:
            return r.json()

    except ConnectError:
        circuit_breaker.record_failure(service_identifier)
        raise HTTPException(status_code=500, detail="Request timeout to RIDES service")


@app.put("/rides/{ride_id}/state")
async def change_ride_state(ride_id: int, state: ChangeRideState, cache=Depends(get_redis)) -> ChangeRideState:
    host, port, service_identifier = get_instance_after_round_robin("rides")

    try:
        if circuit_breaker.is_open(service_identifier):
            raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

        r = httpx.put(f"http://{host}:{port}/api/rides/{ride_id}/state",
                      json=jsonable_encoder(state), headers=HEADERS)

        if r.status_code != 200:
            circuit_breaker.record_failure(service_identifier)
            raise HTTPException(status_code=r.status_code, detail=r.text)
        else:
            if (cache.get(f"ride_{ride_id}")) is not None:
                cache.delete(f"ride_{ride_id}")
            return r.json()

    except ConnectError:
        circuit_breaker.record_failure(service_identifier)
        raise HTTPException(status_code=500, detail="Request timeout to RIDES service")


@app.put("/drivers/{driver_id}/availability")
async def change_driver_availability(driver_id: int, availability: Availability) -> Availability:
    host, port, service_identifier = get_instance_after_round_robin("drivers")

    try:
        if circuit_breaker.is_open(service_identifier):
            raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

        r = httpx.put(f"http://{host}:{port}/api/drivers/{driver_id}/availability",
                      json=jsonable_encoder(availability), headers=HEADERS)

        if r.status_code != 200:
            circuit_breaker.record_failure(service_identifier)
            raise HTTPException(status_code=r.status_code, detail=r.text)
        else:
            return r.json()

    except ConnectError:
        circuit_breaker.record_failure(service_identifier)
        raise HTTPException(status_code=500, detail="Request timeout to DRIVERS service")


@app.put("/drivers/{driver_id}/ride")
async def complete_ride(driver_id: int, state: CompleteRide):
    host, port, service_identifier = get_instance_after_round_robin("drivers")

    try:
        if circuit_breaker.is_open(service_identifier):
            raise HTTPException(status_code=500, detail="Service failed too many times, please wait")

        r = httpx.put(f"http://{host}:{port}/api/drivers/{driver_id}/ride",
                      json=jsonable_encoder(state), headers=HEADERS)

        if r.status_code != 200:
            circuit_breaker.record_failure(service_identifier)
            raise HTTPException(status_code=r.status_code, detail=r.text)
        else:
            return r.json()

    except ConnectError:
        circuit_breaker.record_failure(service_identifier)
        raise HTTPException(status_code=500, detail="Request timeout to DRIVERS service")


@app.get("/status", status_code=200)
async def status_endpoint():
    return {"gateway": "ALIVE", "timestamp": datetime.datetime.now()}
