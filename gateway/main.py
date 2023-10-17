import json
import httpx
import redis
import datetime
import logging

from fastapi import FastAPI, Depends
from fastapi.encoders import jsonable_encoder
from httpx import ConnectError
from schemas import RequestRide, RequestRideResponse, Ride, ChangeRideState, Availability, CompleteRide
from datetime import timedelta
from logging.config import dictConfig

RIDES_SERVICE = "http://localhost:5050/api/rides"
DRIVERS_SERVICE = "http://localhost:6060/api/drivers"
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


def create_redis():
    return redis.ConnectionPool(
        host='localhost',
        port=6379,
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
    return instance_info["host"], instance_info["port"]


@app.get("/rides/{ride_id}")
async def get_ride(ride_id: int, cache=Depends(get_redis)) -> Ride | dict:
    host, port = get_instance_after_round_robin("rides")

    if (cached_ride := cache.get(f"ride_{ride_id}")) is not None:
        return json.loads(cached_ride)
    else:
        try:
            r = httpx.get(f"http://{host}:{port}/api/rides/{ride_id}")
            cache.set(f"ride_{ride_id}", json.dumps(r.json()), ex=timedelta(minutes=1))
            return r.json()
        except ConnectError:
            logger.error(f"Can't connect to current instance of RIDES")


@app.post("/rides")
async def request_ride(requestRide: RequestRide) -> RequestRideResponse | dict:
    host, port = get_instance_after_round_robin("rides")

    try:
        r = httpx.post(f"http://{host}:{port}/api/rides", json=jsonable_encoder(requestRide), headers=HEADERS)
        return r.json()
    except ConnectError:
        logger.error(f"Can't connect to current instance of RIDES")


@app.put("/rides/{ride_id}/state")
async def change_ride_state(ride_id: int, state: ChangeRideState, cache=Depends(get_redis)) -> ChangeRideState:
    host, port = get_instance_after_round_robin("rides")

    try:
        r = httpx.put(f"http://{host}:{port}/api/rides/{ride_id}/state",
                      json=jsonable_encoder(state), headers=HEADERS)

        if (cache.get(f"ride_{ride_id}")) is not None:
            cache.delete(f"ride_{ride_id}")
            return r.json()
        else:
            return r.json()
    except ConnectError:
        logger.error(f"Can't connect to current instance of RIDES")


@app.put("/drivers/{driver_id}/availability")
async def change_driver_availability(driver_id: int, availability: Availability) -> Availability:
    host, port = get_instance_after_round_robin("drivers")

    try:
        r = httpx.put(f"http://{host}:{port}/api/drivers/{driver_id}/availability",
                      json=jsonable_encoder(availability), headers=HEADERS)

        return r.json()
    except ConnectError:
        logger.error(f"Can't connect to current instance of DRIVERS")


@app.put("/drivers/{driver_id}/ride")
async def complete_ride(driver_id: int, state: CompleteRide):
    host, port = get_instance_after_round_robin("drivers")

    try:
        r = httpx.put(f"{DRIVERS_SERVICE}/{driver_id}/ride",
                      json=jsonable_encoder(state), headers=HEADERS)

        return r.json()
    except ConnectError:
        logger.error(f"Can't connect to current instance of DRIVERS")


@app.get("/status", status_code=200)
async def status_endpoint():
    return {"gateway": "ALIVE", "timestamp": datetime.datetime.now()}
