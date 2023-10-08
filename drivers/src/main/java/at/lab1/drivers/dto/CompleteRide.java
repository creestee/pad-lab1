package at.lab1.drivers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CompleteRide {

    private Long driverId;

    private Long rideId;
}
