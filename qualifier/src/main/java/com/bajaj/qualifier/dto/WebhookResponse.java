package com.bajaj.qualifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookResponse {
    @JsonProperty("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
    private String webhookUrl;
    private String accessToken;
}