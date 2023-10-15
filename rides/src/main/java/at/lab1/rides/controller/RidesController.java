package at.lab1.rides.controller;

import at.lab1.rides.dto.*;
import at.lab1.rides.service.RidesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.concurrent.Callable;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("api/rides")
public class RidesController {

    private final RidesService ridesService;

    @GetMapping(path = "/{id}")
    public Callable<ResponseEntity<Ride>> getRide(@PathVariable Long id) {
        return () -> {
            try {
                return ResponseEntity.ok(ridesService.getRide(id));
            } catch (AsyncRequestTimeoutException ex) {
                log.error("Request timeout on ID : {}", id);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
        };
    }

    @PostMapping
    public Callable<ResponseEntity<RequestRideResponse>> requestRide(@RequestBody RequestRide ride) {
        return () -> {
            try {
                return ResponseEntity.ok(ridesService.requestRide(ride));
            } catch (AsyncRequestTimeoutException ex) {
                log.error("Request timeout on ride_request : {}", ride);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
        };
    }

    @PutMapping(path = "/{id}/state")
    public Callable<ResponseEntity<ChangeRideState>> changeRideState(@PathVariable Long id, @RequestBody ChangeRideState state) {
        return () -> {
            try {
                return ResponseEntity.ok(ridesService.changeRideState(id, state));
            } catch (AsyncRequestTimeoutException ex) {
                log.error("Request timeout on change_ride_state with id : {}", id);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
        };
    }

    @GetMapping(path = "/status")
    @ResponseStatus(code = HttpStatus.OK)
    public void statusEndpoint() { }
}