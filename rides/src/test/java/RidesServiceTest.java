import at.lab1.rides.dto.*;
import at.lab1.rides.dto.enums.RideStatus;
import at.lab1.rides.mapper.RideMapper;
import at.lab1.rides.persistence.entity.RideEntity;
import at.lab1.rides.persistence.repository.RideRepository;
import at.lab1.rides.service.RidesService;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RidesServiceTest {

    @Mock
    private Gson gson;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideMapper rideMapper;

    @InjectMocks
    private RidesService ridesService;

    @Test
    void onRideAcceptance_shouldUpdateRideStatusAndDriverId() {
        String acceptedRide = "{\"id\": 1, \"driverId\": 123}";
        Ride ride = new Ride();
        ride.setId(1L);
        ride.setDriverId(123L);
        RideEntity rideEntity = new RideEntity();
        rideEntity.setId(1L);
        rideEntity.setStatus(RideStatus.PENDING);

        when(gson.fromJson(acceptedRide, Ride.class)).thenReturn(ride);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(rideEntity));

        ridesService.onRideAcceptance(acceptedRide);

        verify(rideRepository).saveAndFlush(rideEntity);
        assertEquals(RideStatus.IN_PROGRESS, rideEntity.getStatus());
        assertEquals(123L, rideEntity.getDriverId());
    }

    @Test
    void onRideCompletion_shouldUpdateRideStatusToCompleted() {
        String completedRide = "{\"rideId\": 1}";
        CompleteRideResponse ride = new CompleteRideResponse();
        ride.setRideId(1L);
        RideEntity rideEntity = new RideEntity();
        rideEntity.setId(1L);
        rideEntity.setStatus(RideStatus.IN_PROGRESS);

        when(gson.fromJson(completedRide, CompleteRideResponse.class)).thenReturn(ride);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(rideEntity));

        ridesService.onRideCompletion(completedRide);

        verify(rideRepository).saveAndFlush(rideEntity);
        assertEquals(RideStatus.COMPLETED, rideEntity.getStatus());
    }

    @Test
    void requestRide_shouldSaveNewRideAndSendMessageToRabbitMQ() {
        RequestRide requestRide = new RequestRide();
        requestRide.setPassengerId(123L);
        requestRide.setPickupLocation("A");
        requestRide.setDropoffLocation("B");
        RideEntity rideEntity = new RideEntity();
        rideEntity.setId(1L);
        rideEntity.setStatus(RideStatus.PENDING);
        Ride newRide = new Ride();
        newRide.setId(1L);
        newRide.setStatus(RideStatus.PENDING);

        when(rideMapper.toEntity(any(Ride.class))).thenReturn(rideEntity);

        RequestRideResponse response = ridesService.requestRide(requestRide);

        verify(rideRepository).saveAndFlush(rideEntity);
        assertEquals(1L, response.getRideId());
        assertEquals(RideStatus.PENDING, response.getStatus());
    }

    @Test
    void changeRideState_shouldUpdateRideStatusAndSendMessageToRabbitMQIfCanceled() {
        Long id = 1L;
        ChangeRideState changeRideState = new ChangeRideState(RideStatus.CANCELED);
        RideEntity rideEntity = new RideEntity();
        rideEntity.setId(id);
        rideEntity.setStatus(RideStatus.PENDING);

        when(rideRepository.findById(id)).thenReturn(Optional.of(rideEntity));

        ChangeRideState response = ridesService.changeRideState(id, changeRideState);

        verify(rideRepository).saveAndFlush(rideEntity);
        assertEquals(RideStatus.CANCELED, rideEntity.getStatus());
        assertEquals(RideStatus.CANCELED, response.getRideStatus());
    }

    @Test
    void getRide_shouldReturnRideById() {
        Long id = 1L;
        RideEntity rideEntity = new RideEntity();
        rideEntity.setId(id);
        rideEntity.setStatus(RideStatus.PENDING);
        Ride ride = new Ride();
        ride.setId(id);
        ride.setStatus(RideStatus.PENDING);

        when(rideRepository.findById(id)).thenReturn(Optional.of(rideEntity));
        when(rideMapper.toElement(rideEntity)).thenReturn(ride);

        Ride result = ridesService.getRide(id);

        assertEquals(ride, result);
    }
}
