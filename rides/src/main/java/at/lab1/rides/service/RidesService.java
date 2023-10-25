package at.lab1.rides.service;

import at.lab1.rides.dto.*;
import at.lab1.rides.dto.enums.RideStatus;
import at.lab1.rides.exception.EntryNotFoundException;
import at.lab1.rides.mapper.RideMapper;
import at.lab1.rides.persistence.entity.PassengerEntity;
import at.lab1.rides.persistence.entity.RideEntity;
import at.lab1.rides.persistence.repository.PassengerRepository;
import at.lab1.rides.persistence.repository.RideRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RidesService {

    private static final String RIDE_NOT_FOUND = "Ride not found";
    private static final String PASSENGER_NOT_FOUND = "Passenger not found";

    private final Gson gson;
    private final RabbitTemplate rabbitTemplate;
    private final RideRepository rideRepository;
    private final PassengerRepository passengerRepository;
    private final RideMapper rideMapper;

    @RabbitListener(queues = {"q.ride-acceptance"})
    @Transactional
    public void onRideAcceptance(@Payload String acceptedRide) {
        log.info("New accepted ride : {}", acceptedRide);
        try {
            Ride ride = gson.fromJson(acceptedRide, Ride.class);

            RideEntity rideEntity = rideRepository.findById(ride.getId())
                    .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(ride.getId())));

            rideEntity.setStatus(RideStatus.IN_PROGRESS);
            rideEntity.setDriverId(ride.getDriverId());
            rideRepository.saveAndFlush(rideEntity);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    @RabbitListener(queues = {"q.ride-completion"})
    @Transactional
    public void onRideCompletion(@Payload String completedRide) {
        log.info("New completed ride : {}", completedRide);
        try {
            CompleteRideResponse ride = gson.fromJson(completedRide, CompleteRideResponse.class);

            RideEntity rideEntity = rideRepository.findById(ride.getRideId())
                    .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(ride.getRideId())));

            rideEntity.setStatus(RideStatus.COMPLETED);
            rideRepository.saveAndFlush(rideEntity);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    @Transactional
    public RequestRideResponse requestRide(RequestRide requestRide) {
        log.info("Request ride : {}", requestRide);
        Ride newRide = new Ride();
        newRide.setStatus(RideStatus.PENDING);
        newRide.setPassengerId(requestRide.getPassengerId());
        newRide.setPickupLocation(requestRide.getPickupLocation());
        newRide.setDropoffLocation(requestRide.getDropoffLocation());

        RideEntity rideEntity = rideMapper.toEntity(newRide);

        rideRepository.saveAndFlush(rideEntity);
        newRide.setId(rideEntity.getId());
        rabbitTemplate.convertAndSend("q.ride-assignment", gson.toJson(newRide));

        return new RequestRideResponse(rideEntity.getId(), newRide.getStatus());
    }

    @Transactional
    public Passenger createPassenger(NewPassenger newPassenger) {
        log.info("Creating new passenger : {}", newPassenger);
        Passenger passenger = new Passenger();
        passenger.setLastName(newPassenger.getLastName());
        passenger.setFirstName(newPassenger.getFirstName());

        PassengerEntity passengerEntity = new PassengerEntity();
        passengerEntity.setFirstName(passenger.getFirstName());
        passengerEntity.setLastName(passenger.getLastName());

        passengerRepository.saveAndFlush(passengerEntity);

        passenger.setId(passengerEntity.getId());

        return passenger;
    }

    public Passenger getPassenger(Long id) {
        PassengerEntity passengerEntity = passengerRepository.findById(id)
                .orElseThrow(() -> new EntryNotFoundException(PASSENGER_NOT_FOUND, String.valueOf(id)));
        log.info("Get passenger : {}", id);
        Passenger passenger = new Passenger();
        passenger.setId(passengerEntity.getId());
        passenger.setFirstName(passengerEntity.getFirstName());
        passenger.setLastName(passengerEntity.getLastName());
        return passenger;
    }

    @Transactional
    public ChangeRideState changeRideState(Long id, ChangeRideState changeRideState) {
        log.info("Change state of ride : {}", id);
        RideEntity rideEntity = rideRepository.findById(id)
                .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(id)));

        rideEntity.setStatus(changeRideState.getRideStatus());
        rideRepository.saveAndFlush(rideEntity);

        if (changeRideState.getRideStatus().equals(RideStatus.CANCELED)) {
            rabbitTemplate.convertAndSend("q.ride-cancellation", gson.toJson(rideMapper.toElement(rideEntity)));
        }

        return new ChangeRideState(changeRideState.getRideStatus());
    }

    public Ride getRide(Long id) {
        RideEntity rideEntity = rideRepository.findById(id)
                .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(id)));
        log.info("Get ride : {}", id);
        return rideMapper.toElement(rideEntity);
    }
}
