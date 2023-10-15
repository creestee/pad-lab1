import json

from fastapi import FastAPI, Depends
import httpx
from fastapi.encoders import jsonable_encoder
from schemas import RequestRide, RequestRideResponse, Ride, ChangeRideState, Availability, CompleteRide
import redis
from datetime import timedelta

app = FastAPI()

rides_service = "http://localhost:5050/api/rides"
drivers_service = "http://localhost:6060/api/drivers"

headers = {"Content-Type": "application/json"}


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


@app.get("/rides/{ride_id}", response_model=Ride)
async def get_ride(ride_id: int, cache=Depends(get_redis)):
    if (cached_ride := cache.get(f"ride_{ride_id}")) is not None:
        print(cached_ride)
        return json.loads(cached_ride)
    else:
        r = httpx.get(f"{rides_service}/{ride_id}")
        cache.set(f"ride_{ride_id}", json.dumps(r.json()), ex=timedelta(minutes=1))
        return r.json()


@app.post("/rides", response_model=RequestRideResponse)
async def request_ride(requestRide: RequestRide):
    r = httpx.post(f"{rides_service}", json=jsonable_encoder(requestRide), headers=headers)
    return r.json()


@app.put("/rides/{ride_id}/state", response_model=ChangeRideState)
async def change_ride_state(ride_id: int, state: ChangeRideState, cache=Depends(get_redis)):
    r = httpx.put(f"{rides_service}/{ride_id}/state", json=jsonable_encoder(state), headers=headers)
    if (cache.get(f"ride_{ride_id}")) is not None:
        cache.delete(f"ride_{ride_id}")
        return r.json()
    else:
        return r.json()


@app.put("/drivers/{driver_id}/availability")
async def change_driver_availability(driver_id: int, availability: Availability):
    r = httpx.put(f"{drivers_service}/{driver_id}/availability", json=jsonable_encoder(availability), headers=headers)
    return r.json()


@app.put("/drivers/{driver_id}/ride")
async def complete_ride(driver_id: int, state: CompleteRide):
    r = httpx.put(f"{drivers_service}/{driver_id}/ride", json=jsonable_encoder(state), headers=headers)
    return r.json()
