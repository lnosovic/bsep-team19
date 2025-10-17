package com.example.publickeyinfrastructure.config;
import com.example.publickeyinfrastructure.model.KeystorePassword;
import com.example.publickeyinfrastructure.repository.KeystorePasswordRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.UUID;

@Component
public class KeystoreInitializer {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreInitializer.class);
    private static final String KEYSTORE_FILE = "keystore.jks";

    private final KeystorePasswordRepository passwordRepository;

    public KeystoreInitializer(KeystorePasswordRepository passwordRepository) {
        this.passwordRepository = passwordRepository;
        try {
            init(); // Pozovi logiku iz konstruktora
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize keystore", e);
        }
    }

    public void init() throws Exception {
        // PROVERAVAJ DA LI JE BAZA PRAZNA, A NE DA LI FAJL POSTOJI
        if (passwordRepository.count() == 0) {
            logger.info("Keystore master password not found in DB. Creating a new one...");

            File keystoreFile = new File(KEYSTORE_FILE);
            String randomKeystorePassword = UUID.randomUUID().toString();
            char[] passwordChars = randomKeystorePassword.toCharArray();

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, passwordChars);

            try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                keyStore.store(fos, passwordChars);
            }

            KeystorePassword keystorePassword = new KeystorePassword(randomKeystorePassword);
            passwordRepository.save(keystorePassword);

            logger.info("New keystore created and master password saved to the database.");
        }
    }
}