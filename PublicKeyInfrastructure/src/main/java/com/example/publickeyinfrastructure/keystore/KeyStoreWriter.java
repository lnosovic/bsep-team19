package com.example.publickeyinfrastructure.keystore;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

@Component
public class KeyStoreWriter {

    private KeyStore keyStore;

    public KeyStoreWriter() throws KeyStoreException {
        // Bacamo izuzetak ako JKS nije podržan, što se nikad ne dešava
        this.keyStore = KeyStore.getInstance("JKS");
    }

    public void loadKeyStore(String fileName, char[] password) throws IOException, CertificateException, NoSuchAlgorithmException {
        if (fileName != null && new File(fileName).exists()) {
            keyStore.load(new FileInputStream(fileName), password);
        } else {
            // Ako fajl ne postoji, kreira se novi, prazan keystore u memoriji
            keyStore.load(null, password);
        }
    }

    public void saveKeyStore(String fileName, char[] password) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        keyStore.store(new FileOutputStream(fileName), password);
    }

    // OVA METODA JE KLJUČNA I FALILA JE
    public void writeChain(String alias, PrivateKey privateKey, char[] keyPassword, Certificate[] chain) throws KeyStoreException {
        keyStore.setKeyEntry(alias, privateKey, keyPassword, chain);
    }
}