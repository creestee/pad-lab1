package at.lab1.drivers.service;

import at.lab1.drivers.dto.Availability;
import at.lab1.drivers.dto.CancelRide;
import at.lab1.drivers.dto.CompleteRideResponse;
import at.lab1.drivers.dto.RequestRide;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DriversService {

    @RabbitListener(queues = {"q.ride-requests"})
    private void notifyNewRide(String rideRequest) {
        log.info("New ride request : {}", rideRequest);
        RequestRide ride = new RequestRide();
        try {
            Gson gson = new Gson();
            ride = gson.fromJson(rideRequest, RequestRide.class);
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

    public Availability changeAvailability() {
        return new Availability();
    }

    public CompleteRideResponse completeRide() {
        return new CompleteRideResponse();
    }
}
