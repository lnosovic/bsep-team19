package com.example.publickeyinfrastructure.service;

import com.example.publickeyinfrastructure.dto.LoginRequest;
import com.example.publickeyinfrastructure.dto.LoginResponse;
import com.example.publickeyinfrastructure.dto.RegisterRequest;
import com.example.publickeyinfrastructure.model.Role;
import com.example.publickeyinfrastructure.model.User;
import com.example.publickeyinfrastructure.model.VerificationToken;
import com.example.publickeyinfrastructure.repository.UserRepository;
import com.example.publickeyinfrastructure.repository.VerificationTokenRepository;
import com.example.publickeyinfrastructure.util.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final TokenUtils tokenUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final RecaptchaService recaptchaService;
    public UserService(UserRepository userRepository, VerificationTokenRepository tokenRepository, PasswordEncoder passwordEncoder, EmailService emailService, AuthenticationManager authenticationManager, TokenUtils tokenUtils, CustomUserDetailsService customUserDetailsService, PasswordPolicyValidator passwordPolicyValidator, RecaptchaService recaptchaService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.tokenUtils = tokenUtils;
        this.customUserDetailsService = customUserDetailsService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.recaptchaService = recaptchaService;
    }
    public void register(RegisterRequest request) {
        passwordPolicyValidator.validate(request);
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already in use");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setOrganization(request.getOrganization());
        user.setRole(Role.ORDINARY_USER);
        user.setEnabled(false);

        User savedUser = userRepository.save(user);

        // Kreiranje i slanje verifikacionog tokena
        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(tokenString, savedUser);
        tokenRepository.save(verificationToken);

        String confirmationLink = "http://localhost:8080/api/auth/confirm?token=" + tokenString;
        emailService.sendNotificaitionAsync(user.getEmail(), "Potvrda registracije", "Molimo vas da potvrdite vašu registraciju klikom na link: " + confirmationLink);
        logger.info("New user registered and verification email sent.",
                kv("eventType", "USER_REGISTERED"),
                kv("outcome", "SUCCESS")
        );
    }

    public String confirmToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Token confirmation failed: Token not found.",
                            kv("eventType", "TOKEN_CONFIRMATION_FAILED"),
                            kv("reason", "Token not found"),
                            kv("token", token)
                    );
                    return new IllegalStateException("Token not found");});

        if (verificationToken.isExpired()) {
            logger.warn("Token confirmation failed: Token expired.",
                    kv("eventType", "TOKEN_CONFIRMATION_FAILED"),
                    kv("reason", "Token expired")
            );
            throw new IllegalStateException("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
        logger.info("User account successfully activated.",
                kv("eventType", "ACCOUNT_ACTIVATED"),
                kv("outcome", "SUCCESS")
        );
        return "Nalog je uspešno aktiviran!";
    }

    public LoginResponse login(LoginRequest request) {
        try {
            recaptchaService.validateToken(request.getRecaptchaToken());
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new IllegalStateException("User not found"));
            MDC.put("username", user.getUsername());

            // Logovanje uspešne prijave
            logger.info("User successfully logged in.",
                    kv("eventType", "SUCCESSFUL_LOGIN"),
                    // kv("sourceIp", ...), // Potrebno je dobiti IP adresu iz requesta
                    kv("outcome", "SUCCESS")
            );

            String token = tokenUtils.generateToken(user);
            return new LoginResponse(token);

        } catch (Exception e) {
            MDC.put("username", request.getEmail());
            logger.warn("User login failed.",
                    kv("eventType", "FAILED_LOGIN"),
                    kv("outcome", "FAILURE"),
                    kv("reason", e.getMessage())
            );
            throw e;
        }
    }
}
