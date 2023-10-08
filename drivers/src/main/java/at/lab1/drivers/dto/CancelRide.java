package at.lab1.drivers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CancelRide {

    private Long rideId;

    private Long passengerId;

    private String reason;
}
