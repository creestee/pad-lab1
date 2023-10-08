package at.lab1.rides.dto;

import at.lab1.rides.dto.enums.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Ride {

    private Long rideId;

    private Long passengerId;

    private Long driverId;

    private Location pickupLocation;

    private Location dropoffLocation;

    private RideStatus status;
}