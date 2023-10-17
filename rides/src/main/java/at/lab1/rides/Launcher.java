package at.lab1.rides;

import at.lab1.rides.config.ServiceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;


@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@SpringBootApplication
public class Launcher {
    private final ServiceId serviceId;
    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }

    @Scheduled(initialDelay = 3000, fixedRate = 5000)
    public void sendHeartbeat() {
        try {
            restTemplate.put("http://127.0.0.1:3000/heartbeat", serviceId.getIdentifier(), String.class);
            log.info("new heartbeat");
        } catch (Exception e) {
            log.error(e.getCause().toString());
        }
    }
}