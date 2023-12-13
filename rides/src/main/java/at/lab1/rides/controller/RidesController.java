package at.lab1.rides.controller;

import at.lab1.rides.dto.*;
import at.lab1.rides.exception.EntryNotFoundException;
import at.lab1.rides.service.RidesService;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.Callable;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/rides")
public class RidesController {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;


    private final RidesService ridesService;
    private Bucket bucket;

    // bucket with capacity 20 tokens and with refilling speed 20 tokens per each minute
    @PostConstruct
    public void setupBucket() {
        this.bucket = Bucket.builder()
                .addLimit(limit -> limit.capacity(20).refillGreedy(20, Duration.ofMinutes(1)))
                .build();
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Callable<ResponseEntity<?>> getRide(@PathVariable Long id) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(ridesService.getRide(id));
                } catch (EntryNotFoundException ex) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ride not found with this id");
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on ride with ID : {}", id);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
            };
        } else {
            return () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too Many Requests - Please try again later.");
        }
    }

    @GetMapping(path = "/passengers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Callable<ResponseEntity<?>> getPassenger(@PathVariable Long id) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(ridesService.getPassenger(id));
                } catch (EntryNotFoundException ex) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Passenger not found with this id");
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on passenger with ID : {}", id);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
            };
        } else {
            return () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too Many Requests - Please try again later.");
        }
    }

    @PostMapping(path = "/passengers", produces = MediaType.APPLICATION_JSON_VALUE)
    public Callable<ResponseEntity<?>> createPassenger(@RequestBody NewPassenger newPassenger) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(ridesService.createPassenger(newPassenger));
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on create passenger : {}", newPassenger);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
            };
        } else {
            return () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too Many Requests - Please try again later.");
        }
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Callable<ResponseEntity<?>> requestRide(@RequestBody RequestRide ride) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(ridesService.requestRide(ride));
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on ride_request : {}", ride);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
            };
        } else {
            return () -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too Many Requests - Please try again later.");
        }
    }

    @PutMapping(path = "/{id}/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public Callable<ResponseEntity<?>> changeRideState(@PathVariable Long id, @RequestBody ChangeRideState state) {
        if (bucket.tryConsume(1)) {
            return () -> {
                try {
                    return ResponseEntity.ok(ridesService.changeRideState(id, state));
                } catch (EntryNotFoundException ex) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ride not found with this id");
                } catch (AsyncRequestTimeoutException ex) {
                    log.error("Request timeout on change_ride_state with id : {}", id);
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

    @GetMapping("/health")
    public ResponseEntity<String> healthEndpoint() {
        try {
            DriverManager.setLoginTimeout(2);
            try (Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
                return new ResponseEntity<>("Database connection is OK", HttpStatus.OK);
            }
        } catch (SQLException e) {
            HttpHeaders responseHeaders = new HttpHeaders();
            return new ResponseEntity<>("Database connection error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}