package com.example.publickeyinfrastructure.service;

import com.example.publickeyinfrastructure.dto.RecaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaService {

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
            // Možete logovati i 'error-codes' za detaljniju analizu
            throw new IllegalArgumentException("reCAPTCHA validation failed.");
        }
    }
}
