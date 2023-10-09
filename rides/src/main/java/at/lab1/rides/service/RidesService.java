package at.lab1.rides.service;

import at.lab1.rides.dto.*;
import at.lab1.rides.dto.enums.RideStatus;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RidesService {

    private final Gson gson;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = {"q.ride-acceptance"})
    private void onRideAcceptance(String acceptedRide) {
        log.info("New accepted ride : {}", acceptedRide);
        Ride ride = new Ride();
        try {
            Gson gson = new Gson();
            ride = gson.fromJson(acceptedRide, Ride.class);

            // TODO: to implement driver assignment

        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    @RabbitListener(queues = {"q.ride-completion"})
    private void onRideCompletion(String completedRide) {
        log.info("New completed ride : {}", completedRide);
        CompleteRideResponse ride = new CompleteRideResponse();
        try {
            Gson gson = new Gson();
            ride = gson.fromJson(completedRide, CompleteRideResponse.class);

            // TODO: to implement driver assignment

        } catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public RequestRideResponse requestRide(RequestRide requestRide) {
        Ride newRide = new Ride();
        newRide.setRideId(0L); // to be incremented
        newRide.setStatus(RideStatus.PENDING);
        newRide.setPassengerId(requestRide.getPassengerId());
        newRide.setPickupLocation(requestRide.getPickupLocation());
        newRide.setDropoffLocation(requestRide.getDropoffLocation());

        rabbitTemplate.convertAndSend("q.ride-requests", gson.toJson(newRide));

        return new RequestRideResponse(newRide.getRideId(), newRide.getStatus());
    }

    public CancelRideResponse cancelRide(CancelRide cancelRide) {
        rabbitTemplate.convertAndSend("q.ride-cancels", gson.toJson(cancelRide));

        return new CancelRideResponse(cancelRide.getRideId(), RideStatus.CANCELED);
    }

    public void getRide() {
        //TODO: to implement
    }

}
