import json
import os
import uuid
from logging.config import dictConfig

import redis
import logging
import datetime
import httpx

from fastapi import FastAPI, Depends
from httpx import ConnectError
from pydantic import BaseModel
from apscheduler.schedulers.background import BackgroundScheduler

app = FastAPI()
time_to_expire = 20  # seconds


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


class Service(BaseModel):
    name: str
    host: str
    port: int


class Heartbeat(BaseModel):
    service: str


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


def send_heartbeat():
    registered_instances = get_redis().keys(f'service:*')
    for instance in registered_instances:
        instance_info = json.loads(get_redis().get(instance))

        host, port, service_name = instance_info["host"], instance_info["port"], instance_info["name"]
        try:
            r = httpx.get(f"http://{host}:{port}/api/{service_name}/status")
            if r.status_code == 200:
                get_redis().expire(f"{instance}", time_to_expire)
        except ConnectError:
            pass


@app.on_event('startup')
def init_data():
    scheduler = BackgroundScheduler()
    scheduler.add_job(send_heartbeat, 'cron', second='*/5')
    scheduler.start()


@app.post("/register", status_code=201)
async def register_service(service: Service, store=Depends(get_redis)):
    key = f"service:{service.name}:{uuid.uuid4().hex}"
    data = {"host": service.host, "port": service.port, "name": service.name}
    logger.info(f"A new [{service.name}] service has been registered --- {data}")
    store.set(key, json.dumps(data), ex=time_to_expire)
    return key


@app.get("/status")
async def status_endpoint():
    return {"service_discovery": "ALIVE", "timestamp": datetime.datetime.now()}
