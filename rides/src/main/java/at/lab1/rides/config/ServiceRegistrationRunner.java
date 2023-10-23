package at.lab1.rides.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.web.ServerProperties;
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

    private final static String SERVICE_NAME = "rides";
    private final ServerProperties serverProperties;

    @Value("${service-discovery.host}")
    private String serviceDiscoveryHost;

    @Value("${service-discovery.port}")
    private Integer serviceDiscoveryPort;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("name", SERVICE_NAME);
            requestBody.put("host", InetAddress.getLocalHost().getHostAddress());
            requestBody.put("port", String.valueOf(serverProperties.getPort()));

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    String.format("http://%s:%s/register", serviceDiscoveryHost, serviceDiscoveryPort),
                    requestEntity, String.class);

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}