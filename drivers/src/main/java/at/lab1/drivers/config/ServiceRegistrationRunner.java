package at.lab1.drivers.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistrationRunner implements ApplicationRunner {

    private final static String SERVICE_DISCOVERY_URL = "http://127.0.0.1:3000/register";
    private final static String SERVICE_NAME = "drivers";
    private final static Integer PORT = 6060; // to be fetched from .env

    private final ServiceId serviceId;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("name", SERVICE_NAME);
            requestBody.put("host", InetAddress.getLocalHost().getHostAddress());
            requestBody.put("port", String.valueOf(PORT));

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SERVICE_DISCOVERY_URL, requestEntity, String.class);

            Map<String, String> serviceIdentifier = new HashMap<>();
            serviceIdentifier.put("service", response.getBody());
            serviceId.setIdentifier(new HttpEntity<>(serviceIdentifier, headers));

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}