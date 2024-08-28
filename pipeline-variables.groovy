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
env.SKIPPED_HELM_CHART_DESIGN_RULES =
        "-DhelmDesignRule.config.DR-D1120-045=skip " +
        "-DhelmDesignRule.config.DR-D1120-061=skip " +
        "-DhelmDesignRule.config.DR-D1121-011=skip " +
        "-DhelmDesignRule.config.DR-D1121-060=skip " +
        "-DhelmDesignRule.config.DR-D1121-064=skip " +
        "-DhelmDesignRule.config.DR-D1123-124=skip " +
        "-DhelmDesignRule.config.DR-D1123-127=skip " +
        "-DhelmDesignRule.config.DR-D1123-128=skip " +
        "-DhelmDesignRule.config.DR-D1123-133=skip " +
        "-DhelmDesignRule.config.DR-D1124-021=skip " +
        "-DhelmDesignRule.config.DR-D1120-061=skip " +
        "-DhelmDesignRule.config.DR-D1123-133=skip " +
        "-DhelmDesignRule.config.DR-D470217-001=skip "
env.VALUES_FILE_FOR_HELM_CHART_DESIGN_RULE_CHECK = "dr-values.yaml"
env.JAVA_VERSION = "17"
env.MAVEN_DOCKER_OPTIONS = "-v /usr/bin/docker:/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock"
env.SKIP_VULNERABILITY_ANALYSIS = true
