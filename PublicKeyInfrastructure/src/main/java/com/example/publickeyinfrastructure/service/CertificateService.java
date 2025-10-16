package com.example.publickeyinfrastructure.service;
import com.example.publickeyinfrastructure.dto.CertificateDTO;
import com.example.publickeyinfrastructure.dto.NewCertificateDTO;
import com.example.publickeyinfrastructure.model.CertificateData;
import com.example.publickeyinfrastructure.repository.CertificateDataRepository;
import com.example.publickeyinfrastructure.util.CertificateGenerator;
import com.example.publickeyinfrastructure.util.IssuerData;
import com.example.publickeyinfrastructure.util.SubjectData;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CertificateService {

    private final CertificateDataRepository certificateDataRepository;
    private final String KEYSTORE_FILE = "keystore.jks";
    @Value("${keystore.password}")
    private String keystorePassword;

    public CertificateService(CertificateDataRepository certificateDataRepository) {
        this.certificateDataRepository = certificateDataRepository;
    }

    public void createCertificate(NewCertificateDTO dto) throws Exception {
        KeyPair keyPair = generateKeyPair();
        SubjectData subjectData = generateSubjectData(dto, keyPair);
        IssuerData issuerData;
        X509CertificateHolder certificate;
        boolean isCa = !dto.getCertificateType().equals("END_ENTITY");

        if (dto.getCertificateType().equals("ROOT_CA")) {
            // Self-signed certificate
            issuerData = new IssuerData(keyPair.getPrivate(), subjectData.getX500name());
            certificate = CertificateGenerator.generateCertificate(subjectData, issuerData, isCa, 0);
        } else {
            // Find issuer from keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new java.io.FileInputStream(KEYSTORE_FILE), keystorePassword.toCharArray());

            PrivateKey issuerPrivateKey = (PrivateKey) keyStore.getKey(dto.getIssuerSerialNumber(), keystorePassword.toCharArray());
            Certificate issuerCert = keyStore.getCertificate(dto.getIssuerSerialNumber());

            X500Name issuerX500Name = new X509CertificateHolder(issuerCert.getEncoded()).getSubject();
            issuerData = new IssuerData(issuerPrivateKey, issuerX500Name);

            int pathLen = dto.getCertificateType().equals("INTERMEDIATE_CA") ? 0 : -1;
            certificate = CertificateGenerator.generateCertificate(subjectData, issuerData, isCa, pathLen);
        }

        // Save to Keystore
        saveToKeystore(subjectData.getSerialNumber().toString(), keyPair.getPrivate(), certificate);

        // Save metadata to DB
        CertificateData certData = new CertificateData();
        certData.setSerialNumber(subjectData.getSerialNumber());
        certData.setSubjectName(certificate.getSubject().toString());
        certData.setIssuerName(certificate.getIssuer().toString());
        certData.setValidFrom(certificate.getNotBefore());
        certData.setValidTo(certificate.getNotAfter());
        certData.setCa(isCa);
        certificateDataRepository.save(certData);
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

    private void saveToKeystore(String alias, PrivateKey privateKey, X509CertificateHolder certificate) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try {
            keyStore.load(new java.io.FileInputStream(KEYSTORE_FILE), keystorePassword.toCharArray());
        } catch (java.io.FileNotFoundException e) {
            keyStore.load(null, null);
        }

        keyStore.setKeyEntry(alias, privateKey, keystorePassword.toCharArray(), new Certificate[]{new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificate)});

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
        dto.setSerialNumber(certData.getSerialNumber());
        dto.setSubjectName(certData.getSubjectName());
        dto.setIssuerName(certData.getIssuerName());
        dto.setValidFrom(certData.getValidFrom());
        dto.setValidTo(certData.getValidTo());
        dto.setCa(certData.isCa());
        return dto;
    }
}