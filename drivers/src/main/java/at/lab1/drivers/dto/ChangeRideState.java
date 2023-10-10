package at.lab1.drivers.dto;

import at.lab1.drivers.dto.enums.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChangeRideState {

    private Long rideId;

    private RideStatus rideStatus;
}
