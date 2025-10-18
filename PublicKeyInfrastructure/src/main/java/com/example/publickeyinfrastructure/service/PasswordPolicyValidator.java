package com.example.publickeyinfrastructure.service;
import com.example.publickeyinfrastructure.dto.RegisterRequest;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // Ili WebClient

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class PasswordPolicyValidator {
    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicyValidator.class);
    private static final int MIN_LENGTH = 8;
    private static final int MIN_ZXCVBN_SCORE = 3; // We require a score of "Safely unguessable" or better

    /**
     * Main method that runs all validations.
     * Throws an exception if any check fails.
     */
    public void validate(RegisterRequest request) {
        String password = request.getPassword();

        if (!password.equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long.");
        }

        checkAgainstContextualInfo(password, request);

        checkStrength(password);

        checkIfPwned(password);
    }

    /**
     * Checks that the password does not contain parts of the user's first name, last name, or email.
     */
    private void checkAgainstContextualInfo(String password, RegisterRequest request) {
        String lowerCasePassword = password.toLowerCase();

        String firstName = request.getFirstName();
        if (firstName != null && !firstName.isBlank() && lowerCasePassword.contains(firstName.toLowerCase())) {
            throw new IllegalArgumentException("Password must not contain your first name.");
        }

        String lastName = request.getLastName();
        if (lastName != null && !lastName.isBlank() && lowerCasePassword.contains(lastName.toLowerCase())) {
            throw new IllegalArgumentException("Password must not contain your last name.");
        }

        String email = request.getEmail();
        if (email != null && !email.isBlank()) {
            String emailUsername = email.split("@")[0].toLowerCase();
            if (lowerCasePassword.contains(emailUsername)) {
                throw new IllegalArgumentException("Password must not contain part of your email address.");
            }
        }
    }

    /**
     * Uses the zxcvbn library to estimate password strength.
     * Scores: 0 (very weak) to 4 (very strong). We require a minimum of 3.
     */
    private void checkStrength(String password) {
        Zxcvbn zxcvbn = new Zxcvbn();
        var strength = zxcvbn.measure(password);

        if (strength.getScore() < MIN_ZXCVBN_SCORE) {
            // You can also return specific suggestions to the user
            // String feedback = String.join("\n", strength.getFeedback().getSuggestions());
            throw new IllegalArgumentException("Password is too weak. Try using a longer phrase or a combination of unrelated words.");
        }
    }

    /**
     * Checks the password against the "Have I Been Pwned" Pwned Passwords API
     * in a secure way (using k-Anonymity).
     */
    private void checkIfPwned(String password) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(password.getBytes());
            String hexHash = toHexString(hash);

            String prefix = hexHash.substring(0, 5);
            String suffix = hexHash.substring(5);

            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = "https://api.pwnedpasswords.com/range/" + prefix;
            String response = restTemplate.getForObject(apiUrl, String.class);

            if (response != null && response.toUpperCase().contains(suffix.toUpperCase())) {
                throw new IllegalArgumentException("This password has been exposed in a data breach. Please choose a different one.");
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-1 algorithm not found. Pwned password check cannot be performed.", e);
            System.err.println("SHA-1 algorithm not found.");
        } catch (Exception e) {
            logger.warn("Could not check password against Pwned Passwords API. Check was skipped. Reason: {}", e.getMessage(),
                    kv("eventType", "PWNED_CHECK_SKIPPED"));
            System.err.println("Could not check password against Pwned Passwords API: " + e.getMessage());
        }
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
