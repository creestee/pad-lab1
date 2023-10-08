package at.lab1.rides.service;

import at.lab1.rides.dto.CancelRide;
import at.lab1.rides.dto.RequestRide;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RidesService {

    private final Gson gson;
    private final RabbitTemplate rabbitTemplate;

    private void notifyRideCompletion(String rideRequest) {
        //TODO: to implement
    }

    private void notifyRideAcceptance(String rideCancel) {
        //TODO: to implement
    }

    public void requestRide(RequestRide requestRide) {
        rabbitTemplate.convertAndSend("q.ride-requests", gson.toJson(requestRide));
    }

    public void cancelRide(CancelRide cancelRide) {
        rabbitTemplate.convertAndSend("q.ride-cancels", gson.toJson(cancelRide));
    }

    public void getRide() {
        //TODO: to implement
    }

}
