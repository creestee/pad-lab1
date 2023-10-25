from pydantic import BaseModel
from typing import Optional


class Ride(BaseModel):
    id: int
    passengerId: int
    driverId: Optional[int] = None
    pickupLocation: str
    dropoffLocation: str
    status: str


class RequestRide(BaseModel):
    passengerId: int
    pickupLocation: str
    dropoffLocation: str


class RequestRideResponse(BaseModel):
    rideId: int
    status: str


class ChangeRideState(BaseModel):
    rideStatus: str


class Availability(BaseModel):
    availabilityStatus: str


class CompleteRide(BaseModel):
    rideId: int
    rideStatus: str


class NewPassenger(BaseModel):
    firstName: str
    lastName: str


class NewDriver(BaseModel):
    firstName: str
    lastName: str


class Passenger(BaseModel):
    id: int
    firstName: str
    lastName: str


class Driver(BaseModel):
    id: int
    firstName: str
    lastName: str
    availabilityStatus: str
