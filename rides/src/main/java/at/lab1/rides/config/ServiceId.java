package at.lab1.rides.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
public class ServiceId {
    private HttpEntity<Map<String, String>> identifier;
}