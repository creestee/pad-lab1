import json
import uuid
import redis
import logging
import datetime

from fastapi import FastAPI, Depends
from pydantic import BaseModel

app = FastAPI()
time_to_expire = 3600  # seconds
logger = logging.getLogger(__name__)


class Service(BaseModel):
    name: str
    host: str
    port: int


class Heartbeat(BaseModel):
    service: str


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


@app.get("/service/{service_name}", status_code=200)
async def get_service_instances(service_name: str, store=Depends(get_redis)):
    keys = store.keys(f'service:{service_name}:*')
    return keys


@app.post("/register", status_code=201)
async def register_service(service: Service, store=Depends(get_redis)):
    key = f"service:{service.name}:{uuid.uuid4().hex}"
    data = {f"{service.host}": f"{service.port}"}
    store.set(key, json.dumps(data), ex=time_to_expire)
    print(f"registered {key}")
    return key


@app.put("/heartbeat", status_code=200)
async def send_heartbeat(service_heartbeat: Heartbeat, store=Depends(get_redis)):
    service_identifier = service_heartbeat.service.replace("\"", "")

    if store.exists(f"{service_identifier}"):
        store.expire(f"{service_identifier}", time_to_expire, xx=True)


@app.get("/status")
async def status_endpoint():
    return {"service_discovery": "ALIVE", "timestamp": datetime.datetime.now()}