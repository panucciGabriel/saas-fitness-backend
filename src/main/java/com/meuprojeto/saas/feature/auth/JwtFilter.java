package com.meuprojeto.saas.feature.auth;

import com.meuprojeto.saas.config.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Libera requisições OPTIONS (Pre-flight do CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String email = tokenService.extractUsername(token);

                if (email != null && tokenService.isTokenValid(token, email)) {
                    // 2. Extrai o schema do Token
                    String schema = tokenService.extractSchema(token);

                    if (schema != null) {
                        // 3. Define o schema no Contexto
                        TenantContext.setTenant(schema);
                        System.out.println("DEBUG: Tenant definido para -> " + schema);
                    } else {
                        System.out.println("DEBUG: Token válido, mas schema veio NULO.");
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email, null, Collections.emptyList());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Erro ao validar token: " + e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 4. Limpa o contexto SEMPRE após a requisição
            TenantContext.clear();
        }
    }
}