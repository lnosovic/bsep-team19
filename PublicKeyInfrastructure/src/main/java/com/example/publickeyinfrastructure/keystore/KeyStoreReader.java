package com.example.publickeyinfrastructure.keystore;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
public class KeyStoreReader {

    private KeyStore load(String keyStoreFile, char[] keyStorePass) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(keyStoreFile))) {
            ks.load(in, keyStorePass);
        }
        return ks;
    }

    public Certificate readCertificate(String keyStoreFile, char[] keyStorePass, String alias) throws Exception {
        return load(keyStoreFile, keyStorePass).getCertificate(alias);
    }

    // OVA METODA JE KLJUČNA I FALILA JE
    public Certificate[] readCertificateChain(String keyStoreFile, char[] keyStorePass, String alias) throws Exception {
        return load(keyStoreFile, keyStorePass).getCertificateChain(alias);
    }

    public PrivateKey readPrivateKey(String keyStoreFile, char[] keyStorePass, String alias, char[] entryPass) throws Exception {
        return (PrivateKey) load(keyStoreFile, keyStorePass).getKey(alias, entryPass);
    }
}
