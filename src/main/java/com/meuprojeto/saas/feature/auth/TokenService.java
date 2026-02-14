package com.meuprojeto.saas.feature.auth;

import com.meuprojeto.saas.feature.tenant.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {

    // Substitua a linha antiga por esta:
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(
            "minha_chave_secreta_super_segura_para_o_guia_saas_123".getBytes()
    );

    public String generateToken (Tenant tenant){
        Map<String, Object> claims = new HashMap<>();
        claims.put("schema", tenant.getSchemaName());
        claims.put("tenantId", tenant.getId().toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(tenant.getOwnerEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SECRET_KEY)
                .compact();
    }

    public String extractSchema(String token) {
        return extractAllClaims(token).get("schema", String.class);
    }

    // Valida se o token é autêntico
    public boolean isTokenValid(String token, String email) {
        final String username = extractAllClaims(token).getSubject();
        return (username.equals(email) && !isTokenExpired(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

}
