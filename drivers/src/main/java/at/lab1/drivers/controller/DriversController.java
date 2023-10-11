package at.lab1.drivers.controller;

import at.lab1.drivers.dto.Availability;
import at.lab1.drivers.dto.CompleteRide;
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

    @PutMapping(path = "/{id}/availability")
    @TimeLimiter(name = "driversService")
    public CompletableFuture<ResponseEntity<Availability>> changeAvailability(@PathVariable Long id, @RequestBody Availability availability) {
        log.info("Change availability : {}", availability);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(driversService.changeAvailability(id, availability));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @PutMapping(path = "/{id}/ride")
    @TimeLimiter(name = "driversService")
    public CompletableFuture<ResponseEntity<CompleteRide>> completeRide(@PathVariable Long id, @RequestBody CompleteRide state) {
        log.info("Change ride state : {}", state);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(driversService.completeRide(id, state));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
            }
        });
    }

    @GetMapping(path = "/status")
    @ResponseStatus(code = HttpStatus.OK)
    public void statusEndpoint() { }
}