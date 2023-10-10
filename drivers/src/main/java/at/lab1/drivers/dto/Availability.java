package at.lab1.drivers.dto;

import at.lab1.drivers.dto.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Availability {

    private AvailabilityStatus availabilityStatus;
}
