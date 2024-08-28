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
package com.ericsson.bos.dr.tests.integration.utils

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import org.springframework.web.context.support.GenericWebApplicationContext

@Component
class SpringContextUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx
    }

    static void registerBean(String name, Object bean) {
        ConfigurableListableBeanFactory beanFactory = ((GenericWebApplicationContext)applicationContext).getBeanFactory()
        beanFactory.initializeBean(bean,name)
        beanFactory.registerSingleton(name, bean)
    }

    static void destroyBean(Object bean) {
        ConfigurableListableBeanFactory beanFactory = ((GenericWebApplicationContext)applicationContext).getBeanFactory()
        beanFactory.destroyBean(bean)
    }
}