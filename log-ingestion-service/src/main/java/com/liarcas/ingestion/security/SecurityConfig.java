package com.liarcas.ingestion.security;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Configures stateless API key based security for ingestion endpoints.
 */
@Configuration
@EnableConfigurationProperties(ApiKeyAuthProperties.class)
public class SecurityConfig {

        /**
         * Creates the API key to tenant resolver.
         *
         * @param authProperties API key authentication properties
         * @return tenant resolver
         */
    @Bean
    ApiKeyTenantResolver apiKeyTenantResolver(ApiKeyAuthProperties authProperties) {
        return new ApiKeyTenantResolver(authProperties);
    }

        /**
         * Creates the API key authentication filter.
         *
         * @param authProperties API key authentication properties
         * @param apiKeyTenantResolver tenant resolver
         * @return authentication filter
         */
    @Bean
    ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
            ApiKeyAuthProperties authProperties,
            ApiKeyTenantResolver apiKeyTenantResolver
    ) {
        return new ApiKeyAuthenticationFilter(authProperties, apiKeyTenantResolver);
    }

        /**
         * Builds the Spring Security filter chain for ingestion service endpoints.
         *
         * @param http Spring Security HTTP builder
         * @param apiKeyAuthenticationFilter API key authentication filter
         * @return configured security filter chain
         * @throws Exception when security configuration fails
         */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/error").permitAll()
                        .requestMatchers("/logs").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(apiKeyAuthenticationFilter, AnonymousAuthenticationFilter.class)
                .build();
    }
}
