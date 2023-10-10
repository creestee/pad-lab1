package at.lab1.rides.mapper;

import at.lab1.rides.dto.Ride;
import at.lab1.rides.persistence.entity.RideEntity;
import org.modelmapper.PropertyMap;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {
    private final AutoMapper mapper;

    public RideMapper(AutoMapper autoMapper) {
        this.mapper = autoMapper.addCustomMappings(

                new PropertyMap<RideEntity, Ride>() {
                    @Override
                    protected void configure() {
                        map(source.getPassenger().getId(), destination.getPassengerId());
                    }
                },

                new PropertyMap<Ride, RideEntity>() {
                    @Override
                    protected void configure() {
                        map(source.getPassengerId(), destination.getPassenger().getId());
                    }
                });
    }

    public Ride toElement(RideEntity entity) {
        return mapper.map(entity, Ride.class);
    }

    public RideEntity toEntity(Ride element) {
        return mapper.map(element, RideEntity.class);
    }
}
