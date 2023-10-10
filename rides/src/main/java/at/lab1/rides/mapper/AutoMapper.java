package at.lab1.rides.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AutoMapper {
    private final ModelMapper mapper;

    public AutoMapper() {
        mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public AutoMapper addCustomMappings(PropertyMap<?, ?>... maps) {
        Arrays.stream(maps).forEach(mapper::addMappings);
        return this;
    }

    public <T> T map(Object source, Class<T> destination) {
        return mapper.map(source, destination);
    }

    public <T> T map(Object source, Type destinationType) {
        return mapper.map(source, destinationType);
    }

    public <S, D> D merge(S source, D destination) {
        mapper.map(source, destination);
        return destination;
    }

    public <D, T> List<D> mapAll(final Collection<T> entityList, Class<D> outCLass) {
        return entityList.stream().map(entity -> map(entity, outCLass)).collect(Collectors.toList());
    }
}
