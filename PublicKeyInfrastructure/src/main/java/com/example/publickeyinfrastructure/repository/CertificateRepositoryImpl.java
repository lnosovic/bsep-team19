package com.example.publickeyinfrastructure.repository;
import com.example.publickeyinfrastructure.config.KeystoreInitializer;
import com.example.publickeyinfrastructure.dto.CertificateDTO;
import com.example.publickeyinfrastructure.keystore.KeyStoreReader;
import com.example.publickeyinfrastructure.keystore.KeyStoreWriter;
import com.example.publickeyinfrastructure.model.CertificateEntity;
import com.example.publickeyinfrastructure.util.CertificateGenerator;
import com.example.publickeyinfrastructure.util.IssuerData;
import com.example.publickeyinfrastructure.util.SubjectData;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Repository
public class CertificateRepositoryImpl implements CertificateRepository {
    private static final Logger logger = LoggerFactory.getLogger(CertificateRepositoryImpl.class);
    private final KeyStoreWriter keyStoreWriter;
    private final KeyStoreReader keyStoreReader;
    private final CertificateEntityRepository entityRepository;
    private final KeystorePasswordRepository passwordRepository;
    private String keystorePassword;

    public CertificateRepositoryImpl(KeyStoreWriter writer, KeyStoreReader reader, CertificateEntityRepository repo, KeystorePasswordRepository passRepo) {
        this.keyStoreWriter = writer;
        this.keyStoreReader = reader;
        this.entityRepository = repo;
        this.passwordRepository = passRepo;
    }

    @PostConstruct
    private void loadKeystorePassword() {
        try {
            this.keystorePassword = passwordRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Keystore password not found in database."))
                    .getPassword();
            logger.info("Keystore password successfully loaded from the database.");
        } catch (IllegalStateException e) {
            logger.error("FATAL: Could not load keystore password on startup. Application might not function correctly.",
                    kv("eventType", "KEYSTORE_PASSWORD_LOAD_FAILED"));
            throw e;
        }
    }

    @Override
    @Transactional
    public void saveRootCertificate(SubjectData subjectData, IssuerData issuerData, String privateKeyPassword, String alias) throws Exception {
        logger.info("Attempting to save a new Root CA certificate.",
                kv("eventType", "ROOT_CERT_SAVE"),
                kv("alias", alias),
                kv("serialNumber", subjectData.getSerialNumber()));
        X509CertificateHolder certHolder = CertificateGenerator.generateCertificate(subjectData, issuerData, true, 1);
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");
        Certificate certificate = certConverter.getCertificate(certHolder);

//        String alias = subjectData.getSerialNumber().toString();
        char[] mainPass = keystorePassword.toCharArray();

        keyStoreWriter.loadKeyStore(KeystoreInitializer.KEYSTORE_FILE_PATH, mainPass);
        // Root sertifikat ima lanac koji sadrži samo njega
        keyStoreWriter.writeChain(alias, issuerData.getPrivateKey(), privateKeyPassword.toCharArray(), new Certificate[]{certificate});
        keyStoreWriter.saveKeyStore(KeystoreInitializer.KEYSTORE_FILE_PATH, mainPass);

        CertificateEntity entity = new CertificateEntity(subjectData.getSerialNumber(), alias, privateKeyPassword);
        entityRepository.save(entity);
        logger.info("Successfully saved Root CA certificate.", kv("alias", alias));
    }

    @Override
    @Transactional
    public void saveChainedCertificate(SubjectData subjectData, KeyPair subjectKeyPair, BigInteger issuerSerialNumber, boolean isCa, String privateKeyPassword, String alias) throws Exception {
        logger.info("Attempting to save a new chained certificate.",
                kv("eventType", "CHAINED_CERT_SAVE"),
                kv("alias", alias),
                kv("subjectSerialNumber", subjectData.getSerialNumber()),
                kv("issuerSerialNumber", issuerSerialNumber));
        // 1. Pronađi metapodatke izdavaoca u bazi
        CertificateEntity issuerEntity = entityRepository.findBySerialNumber(issuerSerialNumber)
                .orElseThrow(() -> new KeyStoreException("Izdavalac sa serijskim brojem " + issuerSerialNumber + " nije pronađen."));

        char[] mainPass = keystorePassword.toCharArray();

        // 2. Pročitaj sertifikat izdavaoca iz keystore-a
        Certificate[] issuerChain = keyStoreReader.readCertificateChain(KeystoreInitializer.KEYSTORE_FILE_PATH, mainPass, issuerEntity.getAlias());
        if (issuerChain == null || issuerChain.length == 0) {
            throw new KeyStoreException("Lanac sertifikata za izdavaoca " + issuerSerialNumber + " je prazan ili ne postoji.");
        }
        X509Certificate issuerCert = (X509Certificate) issuerChain[0];

        // =======================================================================
        // ==                 POČETAK VALIDACIONE LOGIKE (TODO)                 ==
        // =======================================================================

        // VALIDACIJA 1: Da li je izdavalac uopšte CA?
        // getBasicConstraints() vraća -1 ako sertifikat nije CA.
        int issuerPathLen = issuerCert.getBasicConstraints();
        if (issuerPathLen == -1) {
            logger.warn("Validation failed: Issuer is not a CA.", kv("issuerSerialNumber", issuerSerialNumber));
            throw new CertificateException("Izabrani izdavalac nije CA sertifikat i ne može da potpisuje druge sertifikate.");
        }

        // VALIDACIJA 2: Validnost datuma
        if (subjectData.getStartDate().before(issuerCert.getNotBefore()) ||
                subjectData.getEndDate().after(issuerCert.getNotAfter())) {
            logger.warn("Validation failed: Certificate validity period is outside issuer's validity.", kv("issuerSerialNumber", issuerSerialNumber));
            throw new InvalidParameterException("Period validnosti novog sertifikata mora biti unutar perioda validnosti sertifikata izdavaoca.");
        }

        // VALIDACIJA 3: Path Length Constraint
        int newPathLen = -1; // -1 znači da se ne primenjuje (za end-entity)
        if (isCa) { // Ako kreiramo novi Intermediate CA
            if (issuerPathLen < 1) {
                // Ako je pathLen izdavaoca 0, on ne može da stvara nove CA sertifikate ispod sebe.
                throw new CertificateException("Izdavalac ima pathLenConstraint=0 i ne može da izda novi Intermediate CA sertifikat.");
            }
            // Novi CA će imati pathLen za jedan manji od svog izdavaoca.
            newPathLen = issuerPathLen - 1;
        }

        // =======================================================================
        // ==                   KRAJ VALIDACIONE LOGIKE (TODO)                  ==
        // =======================================================================

        // 3. Pročitaj privatni ključ izdavaoca
        PrivateKey issuerPrivateKey = keyStoreReader.readPrivateKey(
                KeystoreInitializer.KEYSTORE_FILE_PATH,
                mainPass,
                issuerEntity.getAlias(),
                issuerEntity.getPrivateKeyPassword().toCharArray()
        );

        // 4. Pripremi podatke i generiši novi sertifikat
        X500Name issuerName = new JcaX509CertificateHolder(issuerCert).getSubject();
        IssuerData issuerData = new IssuerData(issuerPrivateKey, issuerName);

        // Prosleđujemo izračunati newPathLen generatoru
        X509CertificateHolder newCertHolder = CertificateGenerator.generateCertificate(subjectData, issuerData, isCa, newPathLen);

        // 5. Sastavi novi lanac i sačuvaj u keystore
        Certificate newCertificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(newCertHolder);
        Certificate[] newChain = new Certificate[issuerChain.length + 1];
        newChain[0] = newCertificate;
        System.arraycopy(issuerChain, 0, newChain, 1, issuerChain.length);

        String newAlias = alias;
        keyStoreWriter.loadKeyStore(KeystoreInitializer.KEYSTORE_FILE_PATH, mainPass);
        keyStoreWriter.writeChain(newAlias, subjectKeyPair.getPrivate(), privateKeyPassword.toCharArray(), newChain);
        keyStoreWriter.saveKeyStore(KeystoreInitializer.KEYSTORE_FILE_PATH, mainPass);

        // 6. Sačuvaj metapodatke novog sertifikata u bazu
        CertificateEntity newEntity = new CertificateEntity(subjectData.getSerialNumber(), newAlias, privateKeyPassword);
        entityRepository.save(newEntity);
        logger.info("Successfully saved chained certificate with alias '{}' signed by issuer '{}'.", newAlias, issuerSerialNumber);
    }
    @Override
    public byte[] getCertificateFile(BigInteger serialNumber) throws Exception {
        // 1. Pronađi metapodatke u bazi
        CertificateEntity entity = entityRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new RuntimeException("Sertifikat sa serijskim brojem " + serialNumber + " nije pronađen."));

        char[] mainPass = keystorePassword.toCharArray();

        // 2. Pročitaj sertifikat iz keystore-a
        Certificate certificate = keyStoreReader.readCertificate(
                KeystoreInitializer.KEYSTORE_FILE_PATH,
                mainPass,
                entity.getAlias()
        );

        if (certificate == null) {
            throw new RuntimeException("Fajl sertifikata za alias '" + entity.getAlias() + "' nije mogao biti pročitan iz keystore-a.");
        }

        // 3. Vrati enkodirane (sirove) podatke sertifikata
        return certificate.getEncoded();
    }

    @Override
    public List<CertificateDTO> findAllCertificates() throws Exception {
        List<CertificateEntity> entities = entityRepository.findAll();
        List<CertificateDTO> dtos = new ArrayList<>();
        char[] mainPass = keystorePassword.toCharArray();

        for (CertificateEntity entity : entities) {
            // Dovoljno je pročitati samo prvi sertifikat iz lanca za prikaz osnovnih informacija
            X509Certificate cert = (X509Certificate) keyStoreReader.readCertificate(KeystoreInitializer.KEYSTORE_FILE_PATH, mainPass, entity.getAlias());
            if (cert == null) continue;

            CertificateDTO dto = new CertificateDTO();
            dto.setId(entity.getId());
            dto.setSerialNumber(cert.getSerialNumber().toString());
            dto.setSubjectName(cert.getSubjectX500Principal().getName());
            dto.setIssuerName(cert.getIssuerX500Principal().getName());
            dto.setValidFrom(cert.getNotBefore());
            dto.setValidTo(cert.getNotAfter());
            dto.setCa(cert.getBasicConstraints() != -1);
            dtos.add(dto);
        }
        return dtos;
    }
}