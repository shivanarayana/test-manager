package com.example.test_manager.controllers;

import com.example.test_manager.config.BulkUrlConfig;
import com.example.test_manager.dto.BulkHealthRequest;
import com.example.test_manager.dto.UrlHealthResult;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class ReadinessController {

    private final RestTemplate restTemplate;

    // The health URL that Kafka App will hit (The "Dummy" Status Reporter)
    private static final String SELF_URL_CHECK = "http://localhost:9012/dummy";

    // The new Kafka App endpoint that performs the dynamic check
    private static final String KAFKA_APP_CHECK_URL = "http://localhost:9001/actuator/dynamichealthcheck";
    private static final String KAFKA_APP_BULK_CHECK_URL = "http://localhost:9001/actuator/dynamichealthbulk";

    private final BulkUrlConfig bulkUrlConfig;

    public ReadinessController(RestTemplate restTemplate, BulkUrlConfig bulkUrlConfig) {
        this.restTemplate = restTemplate;
        this.bulkUrlConfig = bulkUrlConfig;
    }

    /**
     * 1. Status Reporter Endpoint: http://localhost:9012/dummy
     * This is the simple endpoint the Kafka App will call back to.
     * It confirms the Test Manager is running and listening without any proxy logic.
     */
    @GetMapping("/dummy")
    public ResponseEntity<String> selfReadinessCheck() {
        // Return a 200 OK with a simple status (NO outbound calls here)
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\":\"Test Manager Self-Check\"}");
    }

    /**
     * 2. Status Reporter Endpoint: http://localhost:9012/health/bulk
     * This is the bulk endpoint to the Kafka App.
     * It confirms the listening of bulk.
     */
    @PostMapping("/health/bulk")
    public ResponseEntity<List<UrlHealthResult>> sendUrlsForHealthCheck() {

        List<String> urls = bulkUrlConfig.getUrls();

        List<UrlHealthResult> results = checkBulkHealth(urls);

        return ResponseEntity.ok(results);
    }

    public List<UrlHealthResult> checkBulkHealth(List<String> urls) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        HttpEntity<List<String>> entity = new HttpEntity<>(urls, headers);

        ResponseEntity<UrlHealthResult[]> response =
                restTemplate.exchange(
                        KAFKA_APP_BULK_CHECK_URL,
                        HttpMethod.POST,
                        entity,
                        UrlHealthResult[].class
                );

        return List.of(response.getBody());
    }

//    /**
//     * 2. Main Trigger/Proxy Endpoint: http://localhost:9012/verify-readiness
//     * This is the entry point called by an external monitor.
//     */
//    @GetMapping("/verify-readiness")
//    public ResponseEntity<String> verifyServiceRediness() {
//
//        // 1. Set up headers to tell Kafka App what URL to check
//        HttpHeaders headers = new HttpHeaders();
//        // CRITICAL: Pass the "dummy" URL in the custom header to prevent recursion.
//        headers.set("X-Caller-Health-Url", SELF_URL_CHECK);
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        try {
//            // 2. Outbound Call: Test Manager calls the Kafka App's dynamic checker.
//            ResponseEntity<String> response = restTemplate.exchange(
//                    KAFKA_APP_CHECK_URL,
//                    HttpMethod.GET,
//                    entity,
//                    String.class
//            );
//
//            // 3. Return the result (which is the health of the /dummy endpoint)
//            return ResponseEntity
//                    .status(response.getStatusCode())
//                    .body(response.getBody());
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                    .body("{\"status\": \"DOWN\", \"service_checked\":\"TestManager\", \"reason\":\"Failed to get dynamic check from Kafka App.\" }");
//        }
//    }
}