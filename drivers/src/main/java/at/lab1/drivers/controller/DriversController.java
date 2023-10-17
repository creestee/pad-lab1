package at.lab1.drivers.controller;

import at.lab1.drivers.dto.Availability;
import at.lab1.drivers.dto.CompleteRide;
import at.lab1.drivers.service.DriversService;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/drivers")
public class DriversController {

    private final DriversService driversService;
    private Bucket bucket;

    // bucket with capacity 20 tokens and with refilling speed 20 tokens per each minute
    @PostConstruct
    public void setupBucket() {
        this.bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(20).refillGreedy(20, Duration.ofMinutes(1)))
                .build();
    }

    @PutMapping(path = "/{id}/availability")
    public Callable<ResponseEntity<?>> changeAvailability(@PathVariable Long id, @RequestBody Availability availability) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(driversService.changeAvailability(id, availability));
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on change_driver_availability with driver_id : {}", id);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
            };
        } else {
            return () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too Many Requests - Please try again later.");
        }
    }

    @PutMapping(path = "/{id}/ride")
    public Callable<ResponseEntity<?>> completeRide(@PathVariable Long id, @RequestBody CompleteRide state) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(driversService.completeRide(id, state));
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on complete_ride with driver_id : {}", id);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
            };
        } else {
            return () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too Many Requests - Please try again later.");
        }
    }

    @GetMapping(path = "/status")
    @ResponseStatus(code = HttpStatus.OK)
    public void statusEndpoint() { }
}