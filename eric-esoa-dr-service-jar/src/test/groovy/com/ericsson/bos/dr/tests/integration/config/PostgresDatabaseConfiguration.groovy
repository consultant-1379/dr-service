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
package com.ericsson.bos.dr.tests.integration.config

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.datasource.DriverManagerDataSource

import javax.sql.DataSource

@TestConfiguration
@Profile("test-pg")
class PostgresDatabaseConfiguration {

    @Bean(destroyMethod = "close")
    EmbeddedPostgres embeddedPostgresDS(@Value("\${db.port}") int port) {
        return EmbeddedPostgres.builder().setPort(port).start()
    }

    @Bean
    @Primary
    HikariDataSource dataSource(EmbeddedPostgres embeddedPostgres, @Value("\${spring.datasource.username}") String user,
                          @Value("\${spring.datasource.password}") String password) {
        final Properties props = new Properties();
        props.setProperty("stringtype", "unspecified");
        DataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class)
                .url(embeddedPostgres.getJdbcUrl(user, password))
                .username(user)
                .password(password).build()
        dataSource.setDataSourceProperties(props)
        dataSource.setMaximumPoolSize(20)
        return dataSource
    }
}