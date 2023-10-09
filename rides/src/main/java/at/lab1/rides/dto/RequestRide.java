package at.lab1.rides.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestRide {

    private Long passengerId;

    private String pickupLocation;

    private String dropoffLocation;
}
