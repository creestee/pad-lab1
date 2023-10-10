package at.lab1.drivers.persistence.repository;

import at.lab1.drivers.dto.enums.AvailabilityStatus;
import at.lab1.drivers.persistence.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<DriverEntity, Long> {
    DriverEntity findFirstByStatus(AvailabilityStatus status);
}
