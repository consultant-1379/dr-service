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
package com.ericsson.bos.dr.service.utils;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Groovy operations.
 */
public abstract class Groovy {

    private static final String PROPERTY_PREFIX = "arg";

    private Groovy() {}

    /**
     * Evaluate  groovy script passing in the args array. The args will be
     * accessible in the script via the arg index e.g arg1,arg2..argn.
     * @param scriptClass script class
     * @param args args
     * @return evaluated value
     */
    public static Object evalScript(final Class<Script> scriptClass, final Object... args) {
        final Map<String, Object> mapArgs = IntStream.range(0, args.length).mapToObj(Integer.class::cast)
                .collect(Collectors.toMap(i -> PROPERTY_PREFIX + (i+1), i -> args[i]));
        return evalScript(scriptClass, mapArgs);
    }

    /**
     * Evaluate  groovy script passing the args map. The args will be accessible via the
     * key names in them map.
     * @param scriptClass script class
     * @param args args
     * @return evaluated value
     */
    public static Object evalScript(final Class<Script> scriptClass, final Map<String, Object> args) {
        final Script script;
        try {
            script = scriptClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        args.entrySet().forEach(e -> script.setProperty(e.getKey(), e.getValue()));
        return script.run();
    }

    /**
     * Evaluate  groovy expression passing in the args array. The args will be
     * accessible in the script via the arg index e.g arg1,arg2..argn.
     * @param expression expression
     * @param args args
     * @return evaluated value
     */
    public static Object evalExpression(final String expression, final Object... args) {
        final Map<String, Object> mapArgs = IntStream.range(0, args.length).mapToObj(Integer.class::cast)
                .collect(Collectors.toMap(i -> PROPERTY_PREFIX + (i+1), i -> args[i]));
        return evalExpression(expression, mapArgs);
    }

    /**
     * Evaluate  groovy expression passing the args map. The args will be accessible via the
     * key names in them map.
     * @param expression expression
     * @param args args
     * @return evaluated value
     */
    public static Object evalExpression(final String expression, final Map<String, Object> args) {
        final var binding = new Binding();
        args.entrySet().forEach(e -> binding.setVariable(e.getKey(), e.getValue()));
        final var groovySh = new GroovyShell(binding);
        return groovySh.evaluate(expression);
    }
}