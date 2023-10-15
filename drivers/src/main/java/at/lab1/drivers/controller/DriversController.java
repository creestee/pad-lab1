package at.lab1.drivers.controller;

import at.lab1.drivers.dto.Availability;
import at.lab1.drivers.dto.CompleteRide;
import at.lab1.drivers.service.DriversService;
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
@RequestMapping("api/drivers")
public class DriversController {

    private final DriversService driversService;

    @PutMapping(path = "/{id}/availability")
    public Callable<ResponseEntity<Availability>> changeAvailability(@PathVariable Long id, @RequestBody Availability availability) {
        return () -> {
            try {
                return ResponseEntity.ok(driversService.changeAvailability(id, availability));
            } catch (AsyncRequestTimeoutException ex) {
                log.error("Request timeout on change_driver_availability with driver_id : {}", id);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
        };
    }

    @PutMapping(path = "/{id}/ride")
    public Callable<ResponseEntity<CompleteRide>> completeRide(@PathVariable Long id, @RequestBody CompleteRide state) {
        return () -> {
            try {
                return ResponseEntity.ok(driversService.completeRide(id, state));
            } catch (AsyncRequestTimeoutException ex) {
                log.error("Request timeout on complete_ride with driver_id : {}", id);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
        };
    }

    @GetMapping(path = "/status")
    @ResponseStatus(code = HttpStatus.OK)
    public void statusEndpoint() { }
}