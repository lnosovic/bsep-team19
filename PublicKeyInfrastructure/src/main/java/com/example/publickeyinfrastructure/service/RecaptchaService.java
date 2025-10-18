package com.example.publickeyinfrastructure.service;

import com.example.publickeyinfrastructure.dto.RecaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class RecaptchaService {
    private static final Logger logger = LoggerFactory.getLogger(RecaptchaService.class);
    @Value("${google.recaptcha.secret-key}")
    private String secretKey;

    @Value("${google.recaptcha.verify-url}")
    private String verifyUrl;

    /**
     * Validira reCAPTCHA token tako što ga šalje Google-u.
     * Baca izuzetak ako token nije validan.
     * @param token Token dobijen sa frontenda.
     */
    public void validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("reCAPTCHA token is missing.");
        }

        RestTemplate restTemplate = new RestTemplate();

        // Parametri za POST zahtev ka Google-u
        MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
        requestMap.add("secret", secretKey);
        requestMap.add("response", token);

        // Pošalji zahtev i sačekaj odgovor
        RecaptchaResponse response = restTemplate.postForObject(verifyUrl, requestMap, RecaptchaResponse.class);

        if (response == null || !response.isSuccess()) {
            logger.warn("reCAPTCHA validation failed.",
                    kv("eventType", "RECAPTCHA_FAILED"),
                    kv("success", response != null ? response.isSuccess() : "null response"),
                    kv("errorCodes", response != null ? response.getErrorCodes() : "N/A"));
            throw new IllegalArgumentException("reCAPTCHA validation failed.");
        }
        logger.debug("reCAPTCHA validation successful.");
    }
}
