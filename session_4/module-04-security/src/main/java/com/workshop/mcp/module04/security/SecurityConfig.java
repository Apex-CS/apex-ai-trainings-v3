package com.workshop.mcp.module04.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the MCP Server.
 *
 * <p>Every POST to /mcp/message must carry a valid JWT Bearer token issued by Keycloak.
 * Spring Security validates the token signature against Keycloak's JWKS endpoint and
 * checks the issuer claim.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // MCP uses JSON-RPC POST — CSRF protection is not applicable here
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // SSE connection endpoint — the client connects here first (public)
                        // Authentication happens when it sends the first JSON-RPC message
                        .requestMatchers("/sse").permitAll()
                        // Health check for load balancer probes
                        .requestMatchers("/actuator/health").permitAll()
                        // Human-in-the-loop approval endpoint — requires authentication
                        .requestMatchers("/confirm/**").authenticated()
                        // ALL MCP JSON-RPC messages require a valid JWT Bearer token
                        .requestMatchers("/mcp/message").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /**
     * Maps Keycloak realm roles (realm_access.roles) to Spring Security authorities.
     * This enables @PreAuthorize("hasRole('deployer')") checks.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Keycloak puts roles in realm_access.roles — not the standard 'scope' claim
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return jwtConverter;
    }
}
