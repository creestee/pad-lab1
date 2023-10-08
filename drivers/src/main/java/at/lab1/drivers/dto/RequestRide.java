package at.lab1.drivers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestRide {

    private Long passengerId;

    private Location pickupLocation;

    private Location dropoffLocation;
}
