/*
 * Copyright 2015-2024 Ritense BV, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.valtimo.keycloak.security.jwt.authentication;

import static com.ritense.valtimo.contract.security.jwt.JwtConstants.EMAIL_KEY;
import static com.ritense.valtimo.contract.security.jwt.JwtConstants.ROLES_SCOPE;
import static java.util.Objects.requireNonNull;

import com.ritense.valtimo.contract.security.jwt.TokenAuthenticator;
import io.jsonwebtoken.Claims;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class KeycloakTokenAuthenticator implements TokenAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakTokenAuthenticator.class);
    public static final String REALM_ACCESS = "realm_access";
    public static final String RESOURCE_ACCESS = "resource_access";
    private final String clientName;

    public KeycloakTokenAuthenticator(String keycloakClient) {
        this.clientName = keycloakClient;
    }

    @Override
    public boolean supports(Claims claims) {
        try {
            final String email = getEmail(claims);
            if (email == null) {
                logger.info("Support failed: email must be present");
                return false;
            }
            if (email.isBlank()) {
                logger.info("Support failed: email is blank");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.info("Support failed with exception", e);
            return false;
        }
    }

    @Override
    public Authentication authenticate(String jwt, Claims claims) {
        requireNonNull(jwt, "jwt must not be null");
        requireNonNull(claims, "claims must not be null");

        final String email = getEmail(claims);
        final List<String> roles = getRoles(claims);

        if (email != null && roles != null && !roles.isEmpty()) {
            final Set<? extends GrantedAuthority> authorities = roles.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.toUpperCase()))
                .collect(Collectors.toSet());

            final User principal = new User(email, "", authorities);
            return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
        }
        return null;
    }

    private String getEmail(Claims claims) {
        return claims.get(EMAIL_KEY, String.class);
    }

    private List<String> getRoles(Claims claims) {
        final Map<String, List<String>> realmSettings = claims.get(REALM_ACCESS, Map.class);
        final Map<String, Map<String, List<String>>> resourceSettings = claims.get(RESOURCE_ACCESS, Map.class);

        List<String> roles = new ArrayList<>(realmSettings.get(ROLES_SCOPE));

        if (!clientName.isBlank() && resourceSettings != null && resourceSettings.containsKey(clientName)) {
            roles.addAll(resourceSettings.get(clientName).get(ROLES_SCOPE));
        }

        return roles;
    }

}
