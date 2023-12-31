# Ride-Sharing Platform (Uber clone)

## Architecture

![](diagrams/updated-diagram.svg)

## How to run it?

1. Clone the repository:
    ```
    https://github.com/creestee/pad-lab1
    ```
2. Pull the images from dockerhub:
    ```
    docker image pull creesteee/rides-service
   docker image pull creesteee/drivers-service
   docker image pull creesteee/gateway
   docker image pull creesteee/service-discovery
    ```
3. Run the docker compose in root directory of project:
    ```
   docker-compose up -d
   ```
4. Use postman or whatever HTTP client you like to test the below endpoints. Gateway URL is `http://localhost:4000`

### Endpoints

<details>
<summary>Press to see the endpoints</summary>

1. Create new ride (`/rides`) :
    - Request body (POST)
   ```json
   { 
      "passengerId": 1,
      "pickupLocation": "Chisinau",
      "dropoffLocation": "Kiev"
   }
   ```
    - Response
   ```json
   {
       "rideId": 1,
       "status": "IN_PROGRESS"
   }
   ```

2. Fetch ride (`/rides/{ride_id}`) :
    - Request (GET)
    - Response
   ```json
   {
       "id": 1,
       "passengerId": 1,
       "driverId": 4,
       "pickupLocation": "Chisinau",
       "dropoffLocation": "Kiev",
       "status": "IN_PROGRESS"
   }
   ```

3. Create new passenger (`/passenger`) :
    - Request body (POST)
   ```json
   { 
      "firstName": "John",
      "lastName": "Cena"
   }
   ```
    - Response
   ```json
   { 
      "id": 1,
      "firstName": "John",
      "lastName": "Cena"
   }
   ```

4. Create new driver (`/drivers`) :
    - Request body (POST)
   ```json
   { 
      "firstName": "John",
      "lastName": "Cena"
   }
   ```
    - Response
   ```json
   { 
      "id": 1,
      "firstName": "John",
      "lastName": "Cena",
      "availabilityStatus": "ONLINE"
   }
   ```

5. Fetch passenger (`/passenger/{passenger_id}`) :
    - Request (GET)
    - Response
   ```json
   {
       "id": 1,
       "firstName": "Andrei",
       "lastName": "Vasile"
   }
   ```

6. Fetch driver (`/drivers/{driver_id}`) :
    - Request (GET)
    - Response
   ```json
   {
       "id": 1,
       "firstName": "Andrei",
       "lastName": "Vasile",
       "availabilityStatus": "ONLINE"
   }
   ```

7. Change ride state (`/rides/{ride_id}/state`) :
    - Request body (PUT)
    - PENDING,
      CANCELED,
      IN_PROGRESS,
      COMPLETED
   ```json
   { 
      "rideStatus": "CANCELED"
   }
   ```
    - Response
   ```json
   { 
      "rideStatus": "CANCELED"
   }
   ```

8. Change driver availability (`/drivers/{driver_id}/availability`) :
    - Request body (PUT)
    - OFFLINE,
      ONLINE,
      IN_A_RIDE
   ```json
   { 
      "availabilityStatus": "OFFLINE"
   }
   ```
    - Response
   ```json
   { 
      "availabilityStatus": "OFFLINE"
   }
   ```

9. Complete ride (`/drivers/{driver_id}/ride`) :
    - Request body (PUT)
   ```json
   { 
      "rideStatus": "COMPLETED"
   }
   ```
    - Response
   ```json
   { 
      "rideStatus": "COMPLETED"
   }
   ```

</details>

## Application Suitability:

Implementation through distributed systems of a Ride-Sharing Platform is necessary, because by adopting a monolithic approach there is a possibility to ran into several operational issues that microservices solve :

1. **Availability Risks**. A single regression within a monolithic code base can bring the whole system down.
2. **Risky, expensive deployments**. These are painful and time consuming to perform with the frequent need for rollbacks.
3. **Poor separation of concerns**. It is difficult to maintain good separations of concerns with a huge code base. 

So, a microservices approach allows the system to become more flexible :

1. **System reliability**. Overall system reliability goes up in a microservice architecture. A single service can go down (and be rolled back) without taking down the whole system.
2. **Separation of concerns**. Architecturally, microservice architectures force you to ask the question “why does this service exist?” more clearly defining the roles of different components.
3. **Autonomous execution**. Independent deployments unlock better continous integration and safer deployments.

For example, Uber began its journey with a monolithic architecture built for a single offering in a single city. Having one codebase seemed cleaned at that time, and solved Uber core business problems. However, as Uber started expanding worldwide they rigorously faced various problems with respect to the scalability and continuous integration.

While Uber started expanding worldwide this kind of framework introduced various challenges. The following are some of the prominent challenges :
- All the features had to be re-built, deployed and tested again and again to update a single feature.
- Fixing bugs became extremely difficult in a single repository as developers had to change the code again and again.
- Scaling the features simultaneously with the introduction of new features worldwide was quite tough to be handled together.

To avoid such problems Uber decided to break its monolithic architecture into multiple codebases to form a microservice architecture.

## Service Boundaries

- **Ride Request Service**
    - Accepting and processing ride requests from passengers.
    - Matching available drivers to ride requests based on availability.
    - Managing tracking and status updates for rides until they are assigned to a driver.
- **Drivers Service**
    - Handling driver availability (online/offline status).
    - Assigning ride requests to available drivers.
    - Real-time tracking of driver locations for passenger updates.

## Technology Stack and Communication Patterns

All services will be written in Java using Spring Framework, while the Gateway and Service Discovery will be written in Python using FastAPI. Rides Service and Drivers Service will communicate between themselves by message broker communication pattern. ActiveMQ serves as the communication middleware between the Rides Service and the Drivers Service. Services will also make RESTful HTTP requests to endpoints provided by the Service Discovery.

## Data Management

Each service will have its own Postgres database. Ride Request Service will store information about ride requests made by passengers. Drivers Service will store driver-specific information. 


## Deployment and Scaling

Each microservice will be containerized within Docker containers. This approach allows to package the microservices and their dependencies into lightweight, isolated containers, also ensure consistency across different environments and simplifying the deployment process. For a simple Uber clone, using Docker Compose for orchestration may be sufficient. However, I will consider Kubernetes for its advanced scaling and production-grade features if its needed along the project.