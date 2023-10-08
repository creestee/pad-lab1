package at.lab1.rides.controller;

import at.lab1.rides.dto.CancelRide;
import at.lab1.rides.dto.CancelRideResponse;
import at.lab1.rides.dto.RequestRide;
import at.lab1.rides.dto.RequestRideResponse;
import at.lab1.rides.service.RidesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("api/rides")
public class RidesController {

    private final RidesService ridesService;

    @GetMapping(path = "/{id}")
    public void getRide(@PathVariable Integer id) {
        log.info("Get ride : {}", id);
        // TODO: to implement
    }

    @PostMapping(path = "/request")
    public ResponseEntity<RequestRideResponse> requestRide(@RequestBody RequestRide ride) {
        log.info("Request ride : {}", ride);
        ridesService.requestRide(ride);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(path = "/cancel")
    public ResponseEntity<CancelRideResponse> cancelRide(@RequestBody CancelRide ride) {
        log.info("Cancel Ride : {}", ride);
        ridesService.cancelRide(ride);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}