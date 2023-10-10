package at.lab1.drivers.service;

import at.lab1.drivers.dto.*;
import at.lab1.drivers.dto.enums.AvailabilityStatus;
import at.lab1.drivers.dto.enums.RideStatus;
import at.lab1.drivers.exception.EntryNotFoundException;
import at.lab1.drivers.exception.NotAvailableDriverException;
import at.lab1.drivers.persistence.entity.DriverEntity;
import at.lab1.drivers.persistence.repository.DriverRepository;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DriversService {

    private static final String DRIVER_NOT_FOUND = "Driver not found";
    private static final String NOT_AVAILABLE_DRIVER = "No driver is available";

    private final Gson gson;
    private final RabbitTemplate rabbitTemplate;
    private final DriverRepository driverRepository;

    @RabbitListener(queues = {"q.ride-assignment"})
    private void driverAssignment(@Payload String rideRequest) throws NotAvailableDriverException {
        log.info("New ride request : {}", rideRequest);
        try {
            Ride ride = gson.fromJson(rideRequest, Ride.class);
            DriverEntity driverEntity = driverRepository.findFirstByStatus(AvailabilityStatus.ONLINE);

            if (driverEntity != null) {
                ride.setDriverId(driverEntity.getId());
                driverEntity.setStatus(AvailabilityStatus.IN_A_RIDE);
                driverRepository.saveAndFlush(driverEntity);
                rabbitTemplate.convertAndSend("q.ride-acceptance", gson.toJson(ride));
            } else {
                throw new NotAvailableDriverException(NOT_AVAILABLE_DRIVER);
            }

        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    @RabbitListener(queues = {"q.ride-cancellation"})
    private void notifyCancelRide(@Payload String cancelledRide) {
        log.info("New ride cancel : {}", cancelledRide);
        try {
            Ride ride = gson.fromJson(cancelledRide, Ride.class);
            if (ride.getDriverId() != null) {
                DriverEntity driverEntity = driverRepository.findById(ride.getDriverId())
                        .orElseThrow(() -> new EntryNotFoundException(DRIVER_NOT_FOUND, String.valueOf(ride.getDriverId())));
                driverEntity.setStatus(AvailabilityStatus.ONLINE);
                driverRepository.saveAndFlush(driverEntity);
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public Availability changeAvailability(Availability availability) {
        DriverEntity driverEntity = driverRepository.findById(availability.getDriverId())
                .orElseThrow(() -> new EntryNotFoundException(DRIVER_NOT_FOUND, String.valueOf(availability.getDriverId())));
        driverEntity.setStatus(availability.getAvailabilityStatus());
        driverRepository.saveAndFlush(driverEntity);
        return availability;
    }

    public CompleteRideResponse completeRide(CompleteRide completeRide) {
        CompleteRideResponse completedRide = new CompleteRideResponse(completeRide.getRideId(), RideStatus.COMPLETED);
        rabbitTemplate.convertAndSend("q.ride-completion", completedRide);
        return completedRide;
    }
}
