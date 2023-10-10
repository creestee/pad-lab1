package at.lab1.rides.service;

import at.lab1.rides.dto.*;
import at.lab1.rides.dto.enums.RideStatus;
import at.lab1.rides.exception.EntryNotFoundException;
import at.lab1.rides.mapper.RideMapper;
import at.lab1.rides.persistence.entity.RideEntity;
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

    private final Gson gson;
    private final RabbitTemplate rabbitTemplate;
    private final RideRepository rideRepository;
    private final RideMapper rideMapper;

    @RabbitListener(queues = {"q.ride-acceptance"})
    @Transactional
    private void onRideAcceptance(@Payload String acceptedRide) {
        log.info("New accepted ride : {}", acceptedRide);
        try {
            Ride ride = gson.fromJson(acceptedRide, Ride.class);

            RideEntity rideEntity = rideRepository.findById(ride.getId())
                    .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(ride.getId())));

            rideEntity.setStatus(RideStatus.IN_PROGRESS);
            rideRepository.saveAndFlush(rideEntity);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    @RabbitListener(queues = {"q.ride-completion"})
    @Transactional
    private void onRideCompletion(@Payload String completedRide) {
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
        Ride newRide = new Ride();
        newRide.setStatus(RideStatus.PENDING);
        newRide.setPassengerId(requestRide.getPassengerId());
        newRide.setPickupLocation(requestRide.getPickupLocation());
        newRide.setDropoffLocation(requestRide.getDropoffLocation());

        RideEntity rideEntity = rideMapper.toEntity(newRide);
        newRide.setId(rideEntity.getId());

        rideRepository.saveAndFlush(rideEntity);
        rabbitTemplate.convertAndSend("q.ride-assignment", gson.toJson(newRide));

        return new RequestRideResponse(rideEntity.getId(), newRide.getStatus());
    }

    @Transactional
    public CancelRideResponse cancelRide(CancelRide cancelRide) {
        RideEntity rideEntity = rideRepository.findById(cancelRide.getRideId())
                .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(cancelRide.getRideId())));

        rideEntity.setStatus(RideStatus.CANCELED);
        rideRepository.saveAndFlush(rideEntity);

        rabbitTemplate.convertAndSend("q.ride-cancellation", gson.toJson(rideMapper.toElement(rideEntity)));
        return new CancelRideResponse(cancelRide.getRideId(), RideStatus.CANCELED);
    }

    public Ride getRide(Long id) {
        RideEntity rideEntity = rideRepository.findById(id)
                .orElseThrow(() -> new EntryNotFoundException(RIDE_NOT_FOUND, String.valueOf(id)));

        return rideMapper.toElement(rideEntity);
    }

}
