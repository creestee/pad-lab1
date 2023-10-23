FROM python:3.10-slim as build
WORKDIR /app
ADD requirements.txt /app
RUN pip install --no-cache-dir -r requirements.txt
ADD . /app
CMD uvicorn main:app --host 0.0.0.0 --port 4000