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
package com.ericsson.bos.dr.spring.security;

import com.ericsson.bos.dr.service.utils.JSON;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;


/**
 * AuthenticationFilter to verify the user has the required admin role 'eric-bos-dr:admin' to create, update and delete feature packs.
 * If not, then 403 response is returned.
 */
public class FeaturePackAuthenticationFilter extends BasicAuthenticationFilter {

    public static final String ERIC_BOS_DR_ADMIN = "eric-bos-dr:admin";
    public static final String TOKEN_PREFIX = "Bearer ";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturePackAuthenticationFilter.class);

    /**
     * JwtBasicAuthenticationFilter constructor
     *
     * @param authenticationManager - authenticationManager
     */
    FeaturePackAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    /**
     * doFilterInternal - handle the request and read the JWT
     *
     * @param request     - the request sent
     * @param response    - the response received
     * @param filterChain - spring filter chain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {

        if (!isFeaturePackRequest(request) || userIsAuthorized(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        LOGGER.warn("Request with method {} to {} forbidden", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean isFeaturePackRequest(HttpServletRequest request) {
        final String uri = request.getRequestURI();
        boolean isFeaturePackRequest = false;
        if (uri.contains("feature-packs") && !uri.contains("input-configurations") && !uri.contains("listener")) {
            if ("POST".equals(request.getMethod()) || "DELETE".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
                isFeaturePackRequest = true;
            }
        }
        return isFeaturePackRequest;
    }

    private static boolean userIsAuthorized(HttpServletRequest request) {
        final String token = request.getHeader(HttpHeaders.AUTHORIZATION).replace(TOKEN_PREFIX, "");
        if (StringUtils.isNotEmpty(token)) {
            try {
                final JWT jwt = JWTParser.parse(token);
                final JWTClaimsSet jwtClaimSet = jwt.getJWTClaimsSet();
                final AccessToken mappedToken = JSON.read(jwtClaimSet.toString(true), AccessToken.class);
                return (mappedToken.getRoles().contains(ERIC_BOS_DR_ADMIN));
            } catch (Exception exception) {
                LOGGER.error("Failed to parse token: {}", exception.getMessage());
                return false;
            }
        } else {
            LOGGER.warn("Failed to extract token from request");
            return false;
        }
    }
}