package com.example.publickeyinfrastructure.service;
import com.example.publickeyinfrastructure.dto.CertificateDTO;
import com.example.publickeyinfrastructure.dto.NewCertificateDTO;
import com.example.publickeyinfrastructure.model.KeystorePassword;
//import com.example.publickeyinfrastructure.repository.CertificateDataRepository;
import com.example.publickeyinfrastructure.repository.CertificateRepository;
import com.example.publickeyinfrastructure.repository.KeystorePasswordRepository;
import com.example.publickeyinfrastructure.util.CertificateGenerator;
import com.example.publickeyinfrastructure.util.IssuerData;
import com.example.publickeyinfrastructure.util.SubjectData;
import jakarta.annotation.PostConstruct;
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
    private final CertificateRepository certificateRepository;

    public CertificateService(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    public void createCertificate(NewCertificateDTO dto) throws Exception {
        // 2. Dobavite ime ulogovanog korisnika za potrebe logovanja
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Inicijalna validacija ulaznih podataka
        Date today = new Date();
//        if (dto.getValidFrom().before(today) && !DateUtils.isSameDay(dto.getValidFrom(), today)) {
//            throw new InvalidParameterException("Datum 'Valid From' ne može biti u prošlosti.");
//        }
        if (dto.getValidFrom().after(dto.getValidTo())) {
            throw new InvalidParameterException("Datum 'Valid From' ne može biti posle datuma 'Valid To'.");
        }
        BigInteger serialNumber = generateSerialNumber();
        String alias = (dto.getAlias() != null && !dto.getAlias().isEmpty())
                ? dto.getAlias()
                : serialNumber.toString();
        KeyPair keyPair = generateKeyPair();
        X500Name subjectName = buildX500Name(dto);
        SubjectData subjectData = new SubjectData(keyPair.getPublic(), subjectName,
                generateSerialNumber(), dto.getValidFrom(), dto.getValidTo());

        String privateKeyPassword = generateRandomPassword();

        if ("ROOT_CA".equals(dto.getCertificateType())) {
            IssuerData issuerData = new IssuerData(keyPair.getPrivate(), subjectName);
            certificateRepository.saveRootCertificate(subjectData, issuerData, privateKeyPassword, alias);
        } else {
            BigInteger issuerSerialNumber = new BigInteger(dto.getIssuerSerialNumber());
            boolean isCa = "INTERMEDIATE_CA".equals(dto.getCertificateType());
            certificateRepository.saveChainedCertificate(subjectData, keyPair, issuerSerialNumber, isCa, privateKeyPassword, alias);
        }
    }


    public List<CertificateDTO> getAllCertificates() {
        try {
            return certificateRepository.findAllCertificates();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška pri čitanju sertifikata: " + e.getMessage());
        }
    }

    // Pomoćne metode
    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private X500Name buildX500Name(NewCertificateDTO dto) {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
        builder.addRDN(BCStyle.CN, dto.getCommonName());
        builder.addRDN(BCStyle.O, dto.getOrganization());
        builder.addRDN(BCStyle.OU, dto.getOrganizationalUnit());
        builder.addRDN(BCStyle.C, dto.getCountry());
        builder.addRDN(BCStyle.E, dto.getEmail());
        return builder.build();
    }

    private BigInteger generateSerialNumber() {
        return new BigInteger(UUID.randomUUID().toString().replace("-", ""), 16);
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString();
    }
    public byte[] downloadCertificate(String serialNumber) throws Exception {
        BigInteger sn = new BigInteger(serialNumber);
        return certificateRepository.getCertificateFile(sn);
    }
}