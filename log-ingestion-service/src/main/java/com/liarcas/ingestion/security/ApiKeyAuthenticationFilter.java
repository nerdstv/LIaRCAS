package com.liarcas.ingestion.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates /logs requests using configured API keys and resolves tenant context.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyAuthProperties authProperties;
    private final ApiKeyTenantResolver tenantResolver;

    /**
     * Creates an API key authentication filter.
     *
     * @param authProperties API key header and client configuration
     * @param tenantResolver resolver that maps API keys to tenant principals
     */
    public ApiKeyAuthenticationFilter(
            ApiKeyAuthProperties authProperties,
            ApiKeyTenantResolver tenantResolver
    ) {
        this.authProperties = authProperties;
        this.tenantResolver = tenantResolver;
    }

    /**
     * Applies this filter only to the log ingestion endpoint.
     *
     * @param request incoming HTTP request
     * @return true when the request should bypass this filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"/logs".equals(path);
    }

    /**
     * Authenticates requests using API key header and populates the security context.
     *
     * @param request incoming HTTP request
     * @param response outgoing HTTP response
     * @param filterChain remaining filter chain
     * @throws ServletException when downstream processing fails
     * @throws IOException when I/O fails
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String apiKey = request.getHeader(authProperties.getHeaderName());
        Optional<TenantPrincipal> tenantPrincipal = tenantResolver.resolve(apiKey);

        if (tenantPrincipal.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing API key");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        tenantPrincipal.get(),
                        apiKey,
                        Collections.emptyList()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
