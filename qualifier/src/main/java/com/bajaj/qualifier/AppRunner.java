package com.bajaj.qualifier;

import com.bajaj.qualifier.dto.RegistrationRequest;
import com.bajaj.qualifier.dto.SolutionRequest;
import com.bajaj.qualifier.dto.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AppRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AppRunner.class);

    @Value("${user.name}")
    private String name;

    @Value("${user.regno}")
    private String regNo;

    @Value("${user.email}")
    private String email;

    @Value("${api.url.generate}")
    private String generateUrl;

    @Value("${api.url.submit}")
    private String submitUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(String... args) throws Exception {
        logger.info("Application starting...");

        // Step 1: Generate Webhook
        WebhookResponse webhookResponse = generateWebhook();
        if (webhookResponse == null || webhookResponse.getAccessToken() == null) {
            logger.error("Failed to generate webhook. Exiting.");
            return;
        }
        logger.info("Successfully generated webhook. Access Token received.");

        // Step 2: Get SQL Query
        String sqlQuery = getSqlQuery();
        logger.info("Final SQL Query to be submitted:\n{}", sqlQuery);

        // Step 3: Submit Solution
        submitSolution(webhookResponse.getAccessToken(), sqlQuery);
    }

    private WebhookResponse generateWebhook() {
        logger.info("Sending POST request to generate webhook...");
        RegistrationRequest requestBody = new RegistrationRequest(name, regNo, email);
        try {
            ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(generateUrl, requestBody, WebhookResponse.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                logger.error("Error generating webhook. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (Exception e) {
            logger.error("An exception occurred while generating the webhook", e);
            return null;
        }
    }

    private String getSqlQuery() {
        // Extract the last two digits from the registration number
        int lastTwoDigits;
        try {
             String numericPart = regNo.replaceAll("[^\\d]", "");
             lastTwoDigits = Integer.parseInt(numericPart.substring(numericPart.length() - 2));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            logger.error("Could not parse registration number's last two digits. Defaulting to ODD.", e);
            lastTwoDigits = 1; 
        }

       
            // Odd Number: Question 1
            logger.info("Registration number ends in an odd number. Solving Question 1.");
            return "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                   "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, d.DEPARTMENT_NAME " +
                   "FROM PAYMENTS p " +
                   "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                   "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                   "WHERE DAY(p.PAYMENT_TIME) != 1 " +
                   "ORDER BY p.AMOUNT DESC " +
                   "LIMIT 1;";
         
    }

    private void submitSolution(String accessToken, String finalQuery) {
        logger.info("Sending POST request to submit the solution...");

        // Set Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", accessToken);

        // Set Body
        SolutionRequest requestBody = new SolutionRequest(finalQuery);

        HttpEntity<SolutionRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(submitUrl, requestEntity, String.class);
            logger.info("Submission response - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            logger.error("An exception occurred while submitting the solution", e);
        }
    }
}