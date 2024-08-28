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
package com.ericsson.bos.dr.tests.unit.substitution

import com.ericsson.bos.dr.service.AssetService
import com.ericsson.bos.dr.service.PropertiesService
import com.ericsson.bos.dr.service.exceptions.DRServiceException
import com.ericsson.bos.dr.service.exceptions.ErrorCode
import com.ericsson.bos.dr.service.substitution.SubstitutionEngine
import com.ericsson.bos.dr.service.utils.SpringContextHolder
import com.google.common.collect.Maps
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification
import static org.mockito.Mockito.when

@TestPropertySource(properties = ["service.substitution.fail-on-unknown-tokens=true"])
@ContextConfiguration(classes = [SubstitutionEngine.class])
class SubstitutionEngineSpec extends Specification {

    @Autowired
    SubstitutionEngine substitutionEngine

    @MockBean
    PropertiesService propertiesServiceMock

    def "Successful substitution using replaceAtSymbol function"() {

        setup: "set substitution context map"
        Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put("originalStr", testString)

        when: "render the substitution result"
        String result = substitutionEngine.render("{{fn:replaceAtSymbol(originalStr)}}", contextMap, 100l)

        then: "substitution result is as expected"
        result == expectedResult

        where:
        testString                 | expectedResult
        "someone@ericsson.com"     | "someone__ericsson.com"
        "someone@e@r@icsson.com"   | "someone__e__r__icsson.com"
        "someone@e*r!>?icsson.com" | "someone__e*r!>?icsson.com"
        '''someone@ericsson.com''' | '''someone__ericsson.com'''
    }

    def "Successful substitution using jq function"() {

        when: "render the substitution result"
        String result = substitutionEngine.render("{{fn:jq('" + json + "','" + jqExpression + "')}}", [:], 100l)

        then: "substitution result is as expected"
        result == expectedResult

        where:
        json                     | jqExpression | expectedResult
        '{"id": 1}'              | ".id"        | "1"
        '{"value": true}'        | ".value"     | "true"
        '[{"id": 1},{"id": 2}]' | ".[] | {id}" | "[{\"id\":1},{\"id\":2}]"
    }

    def "Successful substitution using currentTimeStamp function"() {

        when: "render the result for currentTimeStamp"
        String result = substitutionEngine.render("{{fn:currentTimeStamp(\"yyyy-MM-dd'T'HH:mm:ss.SSSxxx\")}}", [:], 100l)

        then: "substitution result is as expected"
        !result.isBlank()
    }

    def "Successful substitution using currentTimeMillis function"() {

        when: "render the result for currentTimeMillis"
        String result = substitutionEngine.render("{{fn:currentTimeMillis()}}", [:], 100l)

        then: "substitution result is as expected"
        !result.isBlank()
    }

    def "Exception is thrown when property is incorrect or missing"() {

        setup: "set substitution context map"
        Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put(contextMapKey, "someone@ericsson.com")

        when: "render the substitution result"
        String template = "{{" + namespace + ":" + functionName + "(" + param + ")" + "}}"
        substitutionEngine.render(template, contextMap, 100l)

        then: "DRServiceException is thrown"
        DRServiceException drServiceException = thrown(DRServiceException)
        drServiceException.errorMessage.errorCode == ErrorCode.SUBSTITUTION_FAILED.errorCode

        where:
        namespace          | functionName      | param          | contextMapKey
        "fn"               | "replaceAtSymbol" | "unknownParam" | "originalStr"
        "fn"               | "replaceAtSymbol" | null           | "originalStr"
        "unknownNamespace" | "replaceAtSymbol" | "originalStr"  | "originalStr"
        null               | "replaceAtSymbol" | "originalStr"  | "originalStr"
        "fn"               | "unknownMethod"   | "originalStr"  | "originalStr"
        "fn"               | null              | "originalStr"  | "originalStr"
        "fn"               | "replaceAtSymbol" | "originalStr"  | "unknownKey"
    }

    def "Successful substitution using groovy expression"() {

        when: "render the expression using groovy function"
        String result = substitutionEngine.render(expression, substitutionCtx, 100l)

        then: "substitution result is as expected"
        verification.call(result)

        where:
        expression                                                                    | substitutionCtx  | verification
        "{{fn:groovy('System.currentTimeMillis()')}}"                                 | [:]              | { s -> s.isLong() }
        "{{fn:groovy('java.time.format.DateTimeFormatter" +
                ".ofPattern(arg1).format(java.time.ZonedDateTime.now())', format) }}" | [format: "yyyy"] | { s -> s.isInteger() }
        "{{fn:groovy('arg1.replace(\"@\",\"_\")', value)}}"                           | [value: "1@2@3"] | { s -> s == "1_2_3" }
    }

    def "Successful substitution using groovy script"() {

        setup: "Mock get groovy script"
        String scriptName = "script.groovy"
        String scriptContents = "arg1.toInteger() + arg2.toInteger()"
        Class<Script> scriptClass = new GroovyShell().parse(scriptContents).getClass()

        ApplicationContext applicationCtxMock = Mock(ApplicationContext)
        AssetService assetServiceMock = Mock(AssetService)
        new SpringContextHolder(applicationContext: applicationCtxMock)

        applicationCtxMock.getBean(AssetService.class) >> assetServiceMock
        assetServiceMock.getGroovyScript(scriptName, 100) >> scriptClass

        when: "render the expression using groovy function with script reference"
        String expression = "{{fn:groovy('@${scriptName}', value1, value2)}}"
        String result = substitutionEngine.render(expression, [value1: 1, value2: 9], 100l)

        then: "substitution result is as expected"
        result == "10"
    }

    def "Successful substitution using properties"() {

        setup: "Mock get properties"
        when(propertiesServiceMock.getProperties(1))
                .thenReturn(["prop1": "value1"])

        when: "Render the substitution result"
        String template = "{{ properties.prop1 }}"
        String result = substitutionEngine.render(template, [:], 1)

        then: "Substitution result is as expected"
        result == "value1"
    }

    def "Exception is throw when groovy expression error"() {

        when: "render the expression with invalid groovy expression"
        String expression = "{{fn:groovy('System.currentTime()')}}"
        String result = substitutionEngine.render(expression, [:], 100l)

        then: "DRServiceException is thrown with expected message"
        DRServiceException exception = thrown(DRServiceException)
        exception.getMessage().contains("No signature of method: static java.lang.System.currentTime() is applicable for argument")
    }
}
