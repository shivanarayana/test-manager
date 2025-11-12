package com.example.test_manager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class HealthProxyController {

    // Inject the Kafka health URL from application.yaml
    @Value("${verification.kafka-health-url}")
    private String kafkaHealthUrl;

    private final RestTemplate restTemplate;

    public HealthProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * This is the "verifyServiceRediness" endpoint.
     * It calls the Kafka App's Actuator and returns the result directly.
     * The response body will contain the full aggregated health JSON.
     */
    @GetMapping("/verify-readiness")
    public ResponseEntity<String> verifyServiceRediness() {
        try {
            // Outbound Call: Call the Kafka App's Actuator endpoint
            ResponseEntity<String> response = restTemplate.getForEntity(kafkaHealthUrl, String.class);

            // Inbound Response: Return the Kafka App's response body and status
            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());

        } catch (HttpClientErrorException e) {
            // If Kafka is DOWN, it returns a 503 Service Unavailable (or similar)
            // We catch the error and pass the error status code and body back
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());

        } catch (Exception e) {
            // General connection failure (e.g., Kafka App is completely offline)
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"status\":\"DOWN\",\"error\":\"Failed to connect to Kafka Service Checker: " + e.getMessage() + "\"}");
        }
    }
}
