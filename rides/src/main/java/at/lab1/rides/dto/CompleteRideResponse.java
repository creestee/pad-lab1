package at.lab1.rides.dto;

import at.lab1.rides.dto.enums.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CompleteRideResponse {

    private Long rideId;

    private RideStatus status;
}
