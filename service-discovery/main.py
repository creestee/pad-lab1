import json
import uuid
import redis
import logging
import datetime
import httpx

from fastapi import FastAPI, Depends
from pydantic import BaseModel
from apscheduler.schedulers.background import BackgroundScheduler

app = FastAPI()
time_to_expire = 20  # seconds
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


def send_heartbeat():
    registered_instances = get_redis().keys(f'service:*')
    for instance in registered_instances:
        instance_info = json.loads(get_redis().get(instance))

        host, port, service_name = instance_info["host"], instance_info["port"], instance_info["name"]
        r = httpx.get(f"http://{host}:{port}/api/{service_name}/status")

        if r.status_code == 200:
            get_redis().expire(f"{instance}", time_to_expire)


@app.on_event('startup')
def init_data():
    scheduler = BackgroundScheduler()
    scheduler.add_job(send_heartbeat, 'cron', second='*/5')
    scheduler.start()


@app.post("/register", status_code=201)
async def register_service(service: Service, store=Depends(get_redis)):
    key = f"service:{service.name}:{uuid.uuid4().hex}"
    data = {"host": f"{service.host}", "port": f"{service.port}", "name": f"{service.name}"}
    store.set(key, json.dumps(data), ex=time_to_expire)
    return key


@app.get("/status")
async def status_endpoint():
    return {"service_discovery": "ALIVE", "timestamp": datetime.datetime.now()}
