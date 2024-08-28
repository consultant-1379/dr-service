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
package com.ericsson.bos.dr.service.substitution;

import com.ericsson.bos.dr.service.PropertiesService;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.ericsson.bos.dr.service.exceptions.DRServiceException;
import com.ericsson.bos.dr.service.exceptions.ErrorCode;
import com.ericsson.bos.dr.service.substitution.functions.ExecuteActionFunction;
import com.ericsson.bos.dr.service.substitution.functions.GroovyFunction;
import com.ericsson.bos.dr.service.utils.JSON;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.RenderResult;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Substitution Engine.
 */
@Component
public class SubstitutionEngine {

    public static final String FP_CTX_VAR = "__featurePackId__";
    private static final Logger LOGGER = LoggerFactory.getLogger(SubstitutionEngine.class);

    private final Jinjava jinjava = new Jinjava();

    @Autowired
    private PropertiesService propertiesService;

    @Value("${service.substitution.fail-on-unknown-tokens}")
    private boolean failOnUnknownTokens;

    /**
     * Register Jinja functions.
     */
    @PostConstruct
    public void registerJinjaFunction() {
        Stream.of(new ReplaceAtSymbolFunction(),
                        new JqFunction(),
                        new CurrentTimeStampFunction(),
                        new CurrentTimeMillisFunction(),
                        new GroovyFunction(),
                        new ExecuteActionFunction())
                .forEach(jinjava.getGlobalContext()::registerFunction);
    }

    /**
     * Render the jinja template using the provided substitution context.
     *
     * @param template            jinja template
     * @param substitutionContext substitution context
     * @param featurePackId feature pack id which will be made available to the built-in functions.
     * @return rendered template
     */
    public String render(final String template, final Map<String, Object> substitutionContext, final Long featurePackId) {
        final Map<String, Object> bindings = new HashMap<>(substitutionContext);

        if (featurePackId != null) {
            enrichCtxWithFeaturePackProperties(bindings, featurePackId);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Jinja Template is:{}, substitutionContext is:{}", template, JSON.toString(bindings));
        }

        final JinjavaConfig config = JinjavaConfig.newBuilder().withFailOnUnknownTokens(failOnUnknownTokens).build();
        bindings.put(FP_CTX_VAR, featurePackId);
        final RenderResult result = jinjava.renderForResult(template, bindings, config);

        if (result.hasErrors()) {
            throw new DRServiceException(ErrorCode.SUBSTITUTION_FAILED, result.getErrors().toString());
        } else {
            return result.getOutput();
        }
    }

    private void enrichCtxWithFeaturePackProperties(Map<String, Object> substitutionContext, long featurePackId) {
        final Map<String, Object> properties = propertiesService.getProperties(featurePackId);
        if (MapUtils.isNotEmpty(properties)) {
            substitutionContext.put("properties", properties);
        }
    }
}