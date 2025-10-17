package com.example.publickeyinfrastructure.util;

import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.cert.X509CertificateHolder;

public class CertificateGenerator {

    public static X509CertificateHolder generateCertificate(SubjectData subjectData, IssuerData issuerData, boolean isCa, int pathLen) throws Exception {
        JcaContentSignerBuilder builder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        builder = builder.setProvider("BC");
        ContentSigner contentSigner = builder.build(issuerData.getPrivateKey());

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                issuerData.getX500name(),
                subjectData.getSerialNumber(),
                subjectData.getStartDate(),
                subjectData.getEndDate(),
                subjectData.getX500name(),
                subjectData.getPublicKey());

        // Basic Constraints
        if (isCa) {
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(pathLen));
        } else {
            certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        }

        // Key Usage
        if (isCa) {
            certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else {
            certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

            // =======================================================
            // =====            DODAVANJE SAN EKSTENZIJE           =====
            // =======================================================
            // Dodajemo SAN ekstenziju samo za end-entity sertifikate.
            // Ovo je ključno za servere i klijente.
            GeneralNames subjectAltNames = new GeneralNames(new GeneralName[]{
                    new GeneralName(GeneralName.dNSName, "localhost"),
                    new GeneralName(GeneralName.iPAddress, "127.0.0.1")
            });

            // `false` znači da ekstenzija nije kritična. Ako browser ne razume SAN,
            // može da ga ignoriše (mada svi moderni browseri ga razumeju i zahtevaju).
            certGen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
            // =======================================================
        }

        return certGen.build(contentSigner);
    }
}