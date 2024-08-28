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

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
@Profile("test-containers")
class TestContainerConfiguration {
    
    static {
        int containerPort = 5432
        int localPort = 5432
    PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse(
            "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/postgres:15.2").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("drdb")
            .withPassword("postgres")
            .withUsername("postgres")
            .withExposedPorts(containerPort)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(localPort), new ExposedPort(containerPort)))
            ))
        
        postgreSQLContainer.start()
        System.setProperty("DB_HOST", postgreSQLContainer.getJdbcUrl())
        System.setProperty("DB_USERNAME", postgreSQLContainer.getUsername())
        System.setProperty("DB_PASSWORD", postgreSQLContainer.getPassword())
    }
}
