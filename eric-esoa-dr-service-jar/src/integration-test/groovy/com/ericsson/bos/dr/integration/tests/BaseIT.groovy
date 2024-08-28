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

package com.ericsson.bos.dr.integration.tests

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.apache.commons.lang3.SystemUtils
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.nio.file.Path

class BaseIT extends Specification {

    static final int EXPOSED_DR_PORT = 8080
    static final String DB_USERNAME = "postgres"
    static final String DB_PASSWORD = "postgres"
    static final int POSTGRES_CONTAINER_PORT = 5432
    static final int POSTGRES_LOCAL_PORT = 5433

    static final GenericContainer drContainer
    static final PostgreSQLContainer postgreSQLContainer

    static {
        postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse(
                "armdocker.rnd.ericsson.se/dockerhub-ericsson-remote/postgres:15.2").asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("drdb")
                .withPassword(DB_PASSWORD)
                .withUsername(DB_USERNAME)
                .withExposedPorts(POSTGRES_CONTAINER_PORT)
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(POSTGRES_LOCAL_PORT), new ExposedPort(POSTGRES_CONTAINER_PORT)))
                ))
        postgreSQLContainer.start()
    }

    static {
        drContainer = new GenericContainer(
                new ImageFromDockerfile()
                        .withFileFromPath(".",
                                Path.of(System.getProperty("user.dir")).parent))
                .dependsOn([postgreSQLContainer])
                .withExposedPorts(EXPOSED_DR_PORT)
                .withEnv(["DB_HOST"                        : getDbHost(),
                          "DB_USERNAME"                    : DB_USERNAME,
                          "DB_PASSWORD"                    : DB_PASSWORD,
                          "ALARMS_ENABLED"                 : "false",
                          "SPRING_CLOUD_KUBERNETES_ENABLED": "false"])
        drContainer.start()
    }

    static String getDbHost() {
        String dbHost = postgreSQLContainer.getJdbcUrl()
        if (SystemUtils.IS_OS_WINDOWS) {
            return dbHost.replaceFirst("jdbc:postgresql://(.*?):", "host.docker.internal:")
        } else {
            return dbHost.replaceFirst("jdbc:postgresql://", "")
        }
    }

    String outputDrContainerLogs() {
        println drContainer.getLogs()
    }

    ResponseEntity get(String uri, Class returnType) {
        String address = drContainer.getHost() + ":" + drContainer.getMappedPort(EXPOSED_DR_PORT)
        WebClient client = WebClient.builder()
                .baseUrl("http://" + address)
                .build()
        return client.get()
                .uri(uri)
                .retrieve()
                .toEntity(returnType)
                .onErrorResume(error -> {
                    outputDrContainerLogs()
                    return Mono.error(error)
                }).block()
    }
}