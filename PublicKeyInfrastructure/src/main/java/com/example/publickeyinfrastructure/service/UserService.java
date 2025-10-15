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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final TokenUtils tokenUtils;
    private final CustomUserDetailsService customUserDetailsService;
    public UserService(UserRepository userRepository, VerificationTokenRepository tokenRepository, PasswordEncoder passwordEncoder, EmailService emailService, AuthenticationManager authenticationManager, TokenUtils tokenUtils, CustomUserDetailsService customUserDetailsService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.tokenUtils = tokenUtils;
        this.customUserDetailsService = customUserDetailsService;
    }
    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email already in use");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setOrganization(request.organization());
        user.setRole(Role.ORDINARY_USER); // Svi se registruju kao obični korisnici
        user.setEnabled(false); // Nalog nije aktivan dok se ne verifikuje

        User savedUser = userRepository.save(user);

        // Kreiranje i slanje verifikacionog tokena
        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(tokenString, savedUser);
        tokenRepository.save(verificationToken);

        String confirmationLink = "http://localhost:8080/api/auth/confirm?token=" + tokenString;
        //emailService.sendNotificaitionAsync(user.getEmail(), "Potvrda registracije", "Molimo vas da potvrdite vašu registraciju klikom na link: " + confirmationLink);
    }

    public String confirmToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));

        if (verificationToken.isExpired()) {
            throw new IllegalStateException("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken); // Token je iskorišćen, može se obrisati

        return "Nalog je uspešno aktiviran!";
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String token = tokenUtils.generateToken(user);
        return new LoginResponse(token);
    }
}
