package at.lab1.drivers.service;

import at.lab1.drivers.dto.*;
import at.lab1.drivers.dto.enums.RideStatus;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DriversService {

    private final Gson gson;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {"q.ride-requests"})
    private void driverAssignment(String rideRequest) {
        log.info("New ride request : {}", rideRequest);
        RequestRide ride = new RequestRide();
        try {
            Gson gson = new Gson();
            ride = gson.fromJson(rideRequest, RequestRide.class);

            // TODO: to implement driver assignment

        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    @RabbitListener(queues = {"q.ride-cancels"})
    private void notifyCancelRide(String rideCancel) {
        log.info("New ride cancel : {}", rideCancel);
        CancelRide ride = new CancelRide();
        try {
            Gson gson = new Gson();
            ride = gson.fromJson(rideCancel, CancelRide.class);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public Availability changeAvailability(Availability availability) {
        return availability;
    }

    public CompleteRideResponse completeRide(CompleteRide completeRide) {
        CompleteRideResponse completedRide = new CompleteRideResponse(completeRide.getRideId(), RideStatus.COMPLETED);
        rabbitTemplate.convertAndSend("q.ride-completion", completedRide);
        return completedRide;
    }
}
