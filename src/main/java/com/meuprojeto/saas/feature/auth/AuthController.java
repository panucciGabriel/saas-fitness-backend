package com.meuprojeto.saas.feature.auth;

import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import com.meuprojeto.saas.feature.tenant.TenantService; // Import novo
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TenantRepository tenantRepository;
    private final TokenService tokenService;
    private final TenantService tenantService; // Novo serviço

    public AuthController(TenantRepository tenantRepository, TokenService tokenService, TenantService tenantService) {
        this.tenantRepository = tenantRepository;
        this.tokenService = tokenService;
        this.tenantService = tenantService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Tenant tenant = tenantRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = tokenService.generateToken(tenant);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // --- NOVO ENDPOINT DE REGISTRO ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        // Gera um nome de schema simples (ex: remove espaços e põe minúsculo)
        String schema = "tenant_" + name.toLowerCase().replace(" ", "_");

        // Chama o mestre de obras
        tenantService.createTenant(name, email, schema);

        return ResponseEntity.ok(Map.of("message", "Academia criada com sucesso!", "schema", schema));
    }
}