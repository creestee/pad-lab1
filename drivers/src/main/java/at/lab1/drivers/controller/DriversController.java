package at.lab1.drivers.controller;

import at.lab1.drivers.dto.Availability;
import at.lab1.drivers.dto.CompleteRide;
import at.lab1.drivers.dto.CompleteRideResponse;
import at.lab1.drivers.service.DriversService;
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
@RequestMapping("api/drivers")
public class DriversController {

    private final DriversService driversService;

    @PostMapping(path = "/availability")
    @TimeLimiter(name = "driversService")
    public CompletableFuture<ResponseEntity<Availability>> changeAvailability(@RequestBody Availability availability) {
        log.info("Change availability : {}", availability);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(driversService.changeAvailability(availability));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @PostMapping(path = "/complete-ride")
    @TimeLimiter(name = "driversService")
    public CompletableFuture<ResponseEntity<CompleteRideResponse>> completeRide(@RequestBody CompleteRide completeRide) {
        log.info("Complete ride : {}", completeRide);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(driversService.completeRide(completeRide));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @GetMapping(path = "/status")
    @ResponseStatus(code = HttpStatus.OK)
    public void statusEndpoint() { }
}