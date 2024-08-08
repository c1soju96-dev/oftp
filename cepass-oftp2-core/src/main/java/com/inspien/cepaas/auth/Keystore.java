package com.inspien.cepaas.auth;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.neociclo.odetteftp.util.SecurityUtil;

import lombok.Getter;


public class Keystore {
    @Getter
    private KeyManager[] keyManagers;
    @Getter
    private PrivateKey privateKey;
    @Getter
    private KeyStore keystore;
    @Getter
    private X509Certificate certificate;

    public Keystore(String keystoreFilePath, char[] keystorePassword) throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException{
        File keystoreFile = new File(keystoreFilePath);
        if (!keystoreFile.exists()) throw new IOException("Keystore file not found: " + keystoreFile.getAbsolutePath());
        keystore = SecurityUtil.openKeyStore(keystoreFile, keystorePassword);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePassword);
        keyManagers = keyManagerFactory.getKeyManagers();
        privateKey = SecurityUtil.getPrivateKey(keystore, keystorePassword);
        certificate = SecurityUtil.getCertificateEntry(keystore);
    }
}
