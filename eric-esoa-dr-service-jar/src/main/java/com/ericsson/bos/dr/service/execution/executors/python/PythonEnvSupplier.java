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
package com.ericsson.bos.dr.service.execution.executors.python;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Supply environmental variables which should be accessible in python scripts.
 */
@Component
public class PythonEnvSupplier implements Supplier<Map<String, String>> {

    @Value("${security.tls.enabled}")
    private boolean tlsEnabled;

    @Value("${security.commonCa.certDirectory}")
    private String caCertDir;

    @Value("${security.commonCa.certFile}")
    private String caCertFile;

    @Value("${security.keystore.tlsCertDirectory}")
    private String tlsCertDir;

    @Value("${security.keystore.tlsCertFile}")
    private String tlsCertFile;

    @Value("${security.keystore.tlsKeyFile}")
    private String tlsKeyFile;

    @Value("${service.rest-service.baseUrl}")
    private String restServiceUrl;

    @Override
    public Map<String, String> get() {
        final Map<String, String> env = new HashMap<>();
        env.put("REST_SERVICE_URL", restServiceUrl);
        if (tlsEnabled) {
            env.put("REQUESTS_CA_BUNDLE", Paths.get(caCertDir, caCertFile).toString());
            env.put("CLIENT_CERT", Paths.get(tlsCertDir, tlsCertFile).toString());
            env.put("CLIENT_KEY", Paths.get(tlsCertDir, tlsKeyFile).toString());
        }
        return env;
    }
}