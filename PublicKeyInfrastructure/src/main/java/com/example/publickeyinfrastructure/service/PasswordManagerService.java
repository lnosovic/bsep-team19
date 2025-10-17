//package com.example.publickeyinfrastructure.service;
//
//import com.example.publickeyinfrastructure.dto.PasswordEntryDTO;
//import com.example.publickeyinfrastructure.model.PasswordEntry;
//import com.example.publickeyinfrastructure.repository.PasswordEntryRepository;
//import jakarta.annotation.PostConstruct;
//import org.bouncycastle.asn1.x500.X500Name;
//import org.bouncycastle.cert.X509v3CertificateBuilder;
//import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
//import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
//import org.bouncycastle.operator.ContentSigner;
//import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import javax.crypto.Cipher;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.math.BigInteger;
//import java.security.*;
//import java.security.cert.Certificate;
//import java.security.cert.X509Certificate;
//import java.util.Date;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class PasswordManagerService {
//
//    private final PasswordEntryRepository repository;
//    @Value("${keystore.password}")
//    private String keystorePassword;
//    private final String KEYSTORE_FILE = "keystore.jks";
//    private final String PM_KEY_ALIAS = "password-manager-key"; // Alias ključa za PM
//
//    public PasswordManagerService(PasswordEntryRepository repository) {
//        this.repository = repository;
//    }
//
//    // ====================== NOVI DEO KODA ======================
//    /**
//     * Ova metoda se izvršava automatski nakon pokretanja aplikacije.
//     * Proverava da li ključ za password manager postoji i kreira ga ako ne postoji.
//     */
//    @PostConstruct
//    public void initPasswordManagerKeys() {
//        try {
//            KeyStore keyStore = KeyStore.getInstance("JKS");
//            try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE)) {
//                keyStore.load(fis, keystorePassword.toCharArray());
//            } catch (FileNotFoundException e) {
//                keyStore.load(null, null); // Kreiraj novi keystore u memoriji ako fajl ne postoji
//            }
//
//            // Proveri da li ključ već postoji
//            if (!keyStore.containsAlias(PM_KEY_ALIAS)) {
//                System.out.println("Generating new key pair for Password Manager...");
//
//                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
//                keyGen.initialize(2048);
//                KeyPair pair = keyGen.generateKeyPair();
//
//                // Da bi se sačuvao privatni ključ u JKS, potreban je i "dummy" sertifikat
//                X509Certificate cert = createDummyCertificate(pair);
//
//                keyStore.setKeyEntry(PM_KEY_ALIAS, pair.getPrivate(), keystorePassword.toCharArray(), new Certificate[]{cert});
//
//                // Sačuvaj promene u fajl
//                try (FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE)) {
//                    keyStore.store(fos, keystorePassword.toCharArray());
//                }
//                System.out.println("Password Manager key pair saved to keystore under alias: " + PM_KEY_ALIAS);
//            }
//        } catch (Exception e) {
//            // U produkciji, ovo bi trebalo da zaustavi aplikaciju jer je kritična greška
//            throw new RuntimeException("Failed to initialize Password Manager keys", e);
//        }
//    }
//
//    /**
//     * Pomoćna metoda za kreiranje jednostavnog, samopotpisanog sertifikata
//     * koji je neophodan za čuvanje para ključeva u JKS formatu.
//     */
//    private X509Certificate createDummyCertificate(KeyPair keyPair) throws Exception {
//        X500Name issuerAndSubject = new X500Name("CN=PasswordManagerKey");
//        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
//        Date startDate = new Date();
//        Date endDate = new Date(startDate.getTime() + 365L * 24 * 60 * 60 * 1000 * 100); // 100 godina
//
//        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
//                issuerAndSubject,
//                serialNumber,
//                startDate,
//                endDate,
//                issuerAndSubject,
//                keyPair.getPublic()
//        );
//
//        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
//                .build(keyPair.getPrivate());
//
//        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
//    }
//    // ==================== KRAJ NOVOG DELA KODA ====================
//
//
//    public PasswordEntryDTO savePassword(PasswordEntryDTO dto) throws Exception {
//        KeyStore keyStore = KeyStore.getInstance("JKS");
//        keyStore.load(new FileInputStream(KEYSTORE_FILE), keystorePassword.toCharArray());
//
//        PublicKey publicKey = keyStore.getCertificate(PM_KEY_ALIAS).getPublicKey();
//
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        byte[] encryptedPassword = cipher.doFinal(dto.getPassword().getBytes());
//
//        PasswordEntry entry = new PasswordEntry();
//        entry.setSiteName(dto.getSiteName());
//        entry.setUsername(dto.getUsername());
//        entry.setEncryptedPassword(encryptedPassword);
//        repository.save(entry);
//
//        dto.setId(entry.getId());
//        dto.setPassword(null);
//        return dto;
//    }
//
//    public PasswordEntryDTO getPassword(Long id) throws Exception {
//        PasswordEntry entry = repository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Entry not found with id: " + id));
//
//        KeyStore keyStore = KeyStore.getInstance("JKS");
//        keyStore.load(new FileInputStream(KEYSTORE_FILE), keystorePassword.toCharArray());
//
//        PrivateKey privateKey = (PrivateKey) keyStore.getKey(PM_KEY_ALIAS, keystorePassword.toCharArray());
//
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//        cipher.init(Cipher.DECRYPT_MODE, privateKey);
//        byte[] decryptedPasswordBytes = cipher.doFinal(entry.getEncryptedPassword());
//        String decryptedPassword = new String(decryptedPasswordBytes);
//
//        PasswordEntryDTO dto = new PasswordEntryDTO();
//        dto.setId(entry.getId());
//        dto.setSiteName(entry.getSiteName());
//        dto.setUsername(entry.getUsername());
//        dto.setPassword(decryptedPassword);
//        return dto;
//    }
//
//    public List<PasswordEntryDTO> getAllEntries() {
//        return repository.findAll().stream().map(entry -> {
//            PasswordEntryDTO dto = new PasswordEntryDTO();
//            dto.setId(entry.getId());
//            dto.setSiteName(entry.getSiteName());
//            dto.setUsername(entry.getUsername());
//            return dto;
//        }).collect(Collectors.toList());
//    }
//}