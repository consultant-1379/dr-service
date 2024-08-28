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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Provides static access to spring context.
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    @SuppressWarnings("all")
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    /**
     * Get typed bean from spring application context.
     * @param clazz class
     * @param <T> bean type
     * @return bean instance
     */
    public static <T> T getBean(Class<T> clazz) {
        return SpringContextHolder.applicationContext.getBean(clazz);
    }
}