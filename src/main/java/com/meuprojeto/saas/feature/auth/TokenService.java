package com.meuprojeto.saas.feature.auth;

import com.meuprojeto.saas.feature.tenant.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {

    private final Key secretKey;

    // Lê a chave do application.yml (variável de ambiente JWT_SECRET em produção)
    public TokenService(@Value("${api.security.token.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Token para o Personal (Tenant) — subject é o e-mail do Personal
    public String generateToken(Tenant tenant) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("schema", tenant.getSchemaName());
        claims.put("tenantId", tenant.getId().toString());
        claims.put("role", "TENANT");
        claims.put("name", tenant.getName());

        return buildToken(claims, tenant.getOwnerEmail());
    }

    // Token para o Aluno — subject é o e-mail do próprio aluno
    public String generateStudentToken(Tenant tenant, String studentEmail) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("schema", tenant.getSchemaName());
        claims.put("tenantId", tenant.getId().toString());
        claims.put("role", "STUDENT");

        return buildToken(claims, studentEmail);
    }

    private String buildToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(secretKey)
                .compact();
    }

    public String extractSchema(String token) {
        return extractAllClaims(token).get("schema", String.class);
    }

    public boolean isTokenValid(String token, String email) {
        final String subject = extractAllClaims(token).getSubject();
        return (subject.equals(email) && !isTokenExpired(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }
}
