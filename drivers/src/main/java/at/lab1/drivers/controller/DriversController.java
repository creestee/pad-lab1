package at.lab1.drivers.controller;

import at.lab1.drivers.dto.Availability;
import at.lab1.drivers.dto.CompleteRide;
import at.lab1.drivers.dto.CompleteRideResponse;
import at.lab1.drivers.service.DriversService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("api/drivers")
public class DriversController {

    private final DriversService driversService;

    @PostMapping(path = "/availability")
    public ResponseEntity<Availability> changeAvailability(@RequestBody Availability availability) {
        log.info("Change availability : {}", availability);
        return new ResponseEntity<>(driversService.changeAvailability(availability), HttpStatus.OK);
    }

    @PostMapping(path = "/complete-ride")
    public ResponseEntity<CompleteRideResponse> completeRide(@RequestBody CompleteRide completeRide) {
        log.info("Complete ride : {}", completeRide);
        return new ResponseEntity<>(driversService.completeRide(completeRide), HttpStatus.OK);
    }
}