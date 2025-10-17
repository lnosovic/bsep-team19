package com.example.publickeyinfrastructure.service;
import com.example.publickeyinfrastructure.dto.CertificateDTO;
import com.example.publickeyinfrastructure.dto.NewCertificateDTO;
import com.example.publickeyinfrastructure.model.CertificateData;
import com.example.publickeyinfrastructure.repository.CertificateDataRepository;
import com.example.publickeyinfrastructure.util.CertificateGenerator;
import com.example.publickeyinfrastructure.util.IssuerData;
import com.example.publickeyinfrastructure.util.SubjectData;
import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class CertificateService {
    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);
    private final CertificateDataRepository certificateDataRepository;
    private final String KEYSTORE_FILE = "keystore.jks";

    @Value("${keystore.password}")
    private String keystorePassword;

    public CertificateService(CertificateDataRepository certificateDataRepository) {
        this.certificateDataRepository = certificateDataRepository;
    }

    public void createCertificate(NewCertificateDTO dto) throws Exception {
        // 2. Dobavite ime ulogovanog korisnika za potrebe logovanja
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Inicijalna validacija ulaznih podataka
        Date today = new Date();
        if (dto.getValidFrom().before(today) && !DateUtils.isSameDay(dto.getValidFrom(), today)) {
            throw new InvalidParameterException("Datum 'Valid From' ne može biti u prošlosti.");
        }
        if (dto.getValidFrom().after(dto.getValidTo())) {
            throw new InvalidParameterException("Datum 'Valid From' ne može biti posle datuma 'Valid To'.");
        }

        // 3. Obmotajte ključnu logiku u try-catch blok
        try {
            KeyPair keyPair = generateKeyPair();
            SubjectData subjectData = generateSubjectData(dto, keyPair);
            IssuerData issuerData;
            X509CertificateHolder certificateHolder;
            boolean isCa = !dto.getCertificateType().equals("END_ENTITY");

            if (dto.getCertificateType().equals("ROOT_CA")) {
                issuerData = new IssuerData(keyPair.getPrivate(), subjectData.getX500name());
                int pathLenForRoot = 1;
                certificateHolder = CertificateGenerator.generateCertificate(subjectData, issuerData, isCa, pathLenForRoot);
            } else {
                KeyStore keyStore = KeyStore.getInstance("JKS");
                try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE)) {
                    keyStore.load(fis, keystorePassword.toCharArray());
                }

                String issuerAlias = dto.getIssuerSerialNumber();
                PrivateKey issuerPrivateKey = (PrivateKey) keyStore.getKey(issuerAlias, keystorePassword.toCharArray());
                if (issuerPrivateKey == null) {
                    throw new KeyStoreException("Ne može se pronaći privatni ključ za izdavaoca sa aliasom (SN): " + issuerAlias);
                }

                java.security.cert.X509Certificate issuerCert = (java.security.cert.X509Certificate) keyStore.getCertificate(issuerAlias);
                if (issuerCert == null) {
                    throw new KeyStoreException("Ne može se pronaći sertifikat za izdavaoca sa aliasom (SN): " + issuerAlias);
                }

                issuerCert.checkValidity();
                X509CertificateHolder issuerCertHolder = new JcaX509CertificateHolder(issuerCert);
                BasicConstraints issuerBasicConstraints = BasicConstraints.fromExtensions(issuerCertHolder.getExtensions());
                if (issuerBasicConstraints == null || !issuerBasicConstraints.isCA()) {
                    throw new InvalidParameterException("Izabrani izdavalac (issuer) nije CA sertifikat i ne može da potpisuje druge sertifikate.");
                }

                if (dto.getValidFrom().before(issuerCert.getNotBefore()) || dto.getValidTo().after(issuerCert.getNotAfter())) {
                    throw new InvalidParameterException("Period validnosti novog sertifikata mora biti unutar perioda validnosti sertifikata izdavaoca.");
                }

                int newPathLen = -1;
                if (dto.getCertificateType().equals("INTERMEDIATE_CA")) {
                    BigInteger issuerPathLen = issuerBasicConstraints.getPathLenConstraint();
                    if (issuerPathLen == null) {
                        newPathLen = 0;
                    } else {
                        if (issuerPathLen.intValue() < 1) {
                            throw new InvalidParameterException("Izdavalac (issuer) ima pathLenConstraint=0 i ne može da izda novi Intermediate CA sertifikat.");
                        }
                        newPathLen = issuerPathLen.intValue() - 1;
                    }
                }

                X500Name issuerX500Name = issuerCertHolder.getSubject();
                issuerData = new IssuerData(issuerPrivateKey, issuerX500Name);
                certificateHolder = CertificateGenerator.generateCertificate(subjectData, issuerData, isCa, newPathLen);
            }

            saveToKeystore(subjectData.getSerialNumber().toString(), keyPair.getPrivate(), certificateHolder);

            CertificateData certData = new CertificateData();
            certData.setSerialNumber(subjectData.getSerialNumber());
            certData.setSubjectName(certificateHolder.getSubject().toString());
            certData.setIssuerName(certificateHolder.getIssuer().toString());
            certData.setValidFrom(certificateHolder.getNotBefore());
            certData.setValidTo(certificateHolder.getNotAfter());
            certData.setCa(isCa);
            certificateDataRepository.save(certData);

            // 4. Logovanje USPEŠNOG događaja na kraju try bloka
            logger.info("Certificate created successfully.",
                    kv("eventType", "CERTIFICATE_CREATED"),
                    kv("outcome", "SUCCESS"),
                    kv("context", Map.of(
                            "certificateType", dto.getCertificateType(),
                            "subject", certificateHolder.getSubject().toString(),
                            "serialNumber", subjectData.getSerialNumber().toString(),
                            "issuerSerialNumber", dto.getIssuerSerialNumber() != null ? dto.getIssuerSerialNumber() : "self-signed"
                    ))
            );

        } catch (Exception e) {
            // 5. Logovanje NEUSPEŠNOG događaja u catch bloku
            logger.error("Certificate creation failed.",
                    kv("eventType", "CERTIFICATE_CREATION_FAILED"),
                    kv("outcome", "FAILURE"),
                    kv("reason", e.getMessage()),
                    kv("requestData", dto) // Logujemo i ulazne podatke radi lakšeg debagovanja
            );
            // 6. Ponovo baci izuzetak da bi ga obradio globalni exception handler
            throw e;
        }
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private SubjectData generateSubjectData(NewCertificateDTO dto, KeyPair keyPair) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.CN, dto.getCommonName());
        builder.addRDN(BCStyle.O, dto.getOrganization());
        builder.addRDN(BCStyle.OU, dto.getOrganizationalUnit());
        builder.addRDN(BCStyle.C, dto.getCountry());
        builder.addRDN(BCStyle.E, dto.getEmail());

        return new SubjectData(keyPair.getPublic(), builder.build(), new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16), dto.getValidFrom(), dto.getValidTo());
    }

    private void saveToKeystore(String alias, PrivateKey privateKey, X509CertificateHolder certificateHolder) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(KEYSTORE_FILE)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        } catch (java.io.FileNotFoundException e) {
            keyStore.load(null, null);
        }

        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");
        Certificate certificate = certConverter.getCertificate(certificateHolder);

        keyStore.setKeyEntry(alias, privateKey, keystorePassword.toCharArray(), new Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream(KEYSTORE_FILE)) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }
    }

    public List<CertificateDTO> getAllCertificates() {
        return certificateDataRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private CertificateDTO convertToDto(CertificateData certData) {
        CertificateDTO dto = new CertificateDTO();
        dto.setId(certData.getId());
        dto.setSerialNumber(certData.getSerialNumber().toString());
        dto.setSubjectName(certData.getSubjectName());
        dto.setIssuerName(certData.getIssuerName());
        dto.setValidFrom(certData.getValidFrom());
        dto.setValidTo(certData.getValidTo());
        dto.setCa(certData.isCa());
        return dto;
    }
}