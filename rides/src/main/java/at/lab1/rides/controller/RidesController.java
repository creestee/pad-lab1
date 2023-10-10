package at.lab1.rides.controller;

import at.lab1.rides.dto.*;
import at.lab1.rides.service.RidesService;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("api/rides")
public class RidesController {

    private final RidesService ridesService;

    @GetMapping(path = "/{id}")
    @TimeLimiter(name = "ridesService")
    public CompletableFuture<ResponseEntity<Ride>> getRide(@PathVariable Long id) {
        log.info("Get ride : {}", id);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(ridesService.getRide(id));
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @PostMapping
    @TimeLimiter(name = "ridesService")
    public CompletableFuture<ResponseEntity<RequestRideResponse>> requestRide(@RequestBody RequestRide ride) {
        log.info("Request ride : {}", ride);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(ridesService.requestRide(ride));
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @PutMapping(path = "/{id}/state")
    @TimeLimiter(name = "ridesService")
    public CompletableFuture<ResponseEntity<ChangeRideState>> changeRideState(@PathVariable Long id, @RequestBody ChangeRideState state) {
        log.info("Change Ride State : {}", state);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(ridesService.changeRideState(id, state));
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @GetMapping(path = "/status")
    @ResponseStatus(code = HttpStatus.OK)
    public void statusEndpoint() { }
}