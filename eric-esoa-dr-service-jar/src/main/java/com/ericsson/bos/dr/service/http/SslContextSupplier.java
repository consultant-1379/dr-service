/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.bos.dr.service.http;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;

import com.ericsson.bos.so.security.mtls.MtlsConfigurationReloadersRegister;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.function.Supplier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * Supply the sslContext for unsecure/secure communication with internal services.
 * The SslContext will be secure or unsecure based on the security property 'security.tls.enabled'.
 * For secure context, the truststore and keystores are managed by eric-eso-mtls-helper library which created and updates the
 * stores based on the configured secrets.
 * If the keystores are updated then the secure SslContext is recreated using the updated stores.
 */
@Component
public class SslContextSupplier implements Supplier<SslContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslContextSupplier.class);

    @Value("${security.tls.enabled}")
    private String securityTlsEnabled;

    @Value("${security.truststore.path:}")
    private String trustStorePath;

    @Value("${security.cryptoStoreParams.storePass}")
    private String storePass;

    @Value("${security.keystore.path}")
    private String keyStorePath;

    private SslContext sslContext;

    /**
     * Initialize the SslContext.
     */
    @PostConstruct
    void initSslContext() {
        if (Boolean.parseBoolean(securityTlsEnabled)) {
            createSecureSslCtx();
            MtlsConfigurationReloadersRegister.getInstance().register(() -> {
                LOGGER.info("Recreating SslContext, sip-tls keystores have been updated");
                createSecureSslCtx();
            });
        } else {
            createUnsecureSslCtx();
        }
    }

    @Override
    public synchronized SslContext get() {
        return sslContext;
    }

    private synchronized void createSecureSslCtx() {
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(getTrustManagerFactory())
                    .keyManager(getKeyManagerFactory())
                    .build();
        } catch (Exception e) {
            throw new DRServiceException(ErrorCode.GENERAL_ERROR, e.getMessage());
        }
    }

    private synchronized void createUnsecureSslCtx() {
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (Exception e) {
            throw new DRServiceException(ErrorCode.GENERAL_ERROR, e.getMessage());
        }
    }

    private TrustManagerFactory getTrustManagerFactory() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        try (InputStream trustStoreStream = Files.newInputStream(Paths.get(trustStorePath), StandardOpenOption.READ)) {
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(trustStoreStream, getStorePass().toCharArray());
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            return trustManagerFactory;
        }
    }

    private KeyManagerFactory getKeyManagerFactory()
            throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        try (InputStream keyStoreStream = Files.newInputStream(Paths.get(keyStorePath), StandardOpenOption.READ)) {
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keyStoreStream, getStorePass().toCharArray());
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, getStorePass().toCharArray());
            return keyManagerFactory;
        }
    }

    private String getStorePass() {
        return decodePassword(storePass);
    }

    private String decodePassword(final String pass) {
        return new String(Base64.getDecoder().decode(pass), StandardCharsets.UTF_8);
    }
}