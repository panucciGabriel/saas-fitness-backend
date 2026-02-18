package com.meuprojeto.saas.feature.invite;

import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final InviteRepository inviteRepository;
    private final TenantRepository tenantRepository;

    public InviteController(InviteRepository inviteRepository, TenantRepository tenantRepository) {
        this.inviteRepository = inviteRepository;
        this.tenantRepository = tenantRepository;
    }

    // --- CORREÇÃO PRINCIPAL AQUI ---
    @PostMapping
    public ResponseEntity<?> createInvite() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(403).body("Usuário não autenticado.");
        }

        String ownerEmail = authentication.getName();

        // Verifica se o usuário logado é um Personal (Tenant). Alunos recebem 403.
        Tenant tenant = tenantRepository.findByOwnerEmail(ownerEmail)
                .orElse(null);

        if (tenant == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Apenas Personais podem criar convites."));
        }

        // 3. Criamos o convite "Em Branco" (Self-Service)
        // Removemos o 'body.get("email")' que estava causando o erro
        Invite invite = Invite.builder()
                .tenantId(tenant.getId())
                .email(null) // Convite aberto
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        inviteRepository.save(invite);

        // URL configurável via variável de ambiente APP_FRONTEND_URL
        String link = frontendUrl + "/register?token=" + invite.getId();

        return ResponseEntity.ok(Map.of(
                "link", link,
                "token", invite.getId(),
                "expiresAt", invite.getExpiresAt()));
    }

    // Endpoint legado: GET /api/invites/{token}
    @GetMapping("/{token}")
    public ResponseEntity<?> validateInviteLegacy(@PathVariable UUID token) {
        return validateInviteById(token);
    }

    // Endpoint semântico: GET /api/invites/validate/{token} (usado pelo frontend)
    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateInvite(@PathVariable UUID token) {
        return validateInviteById(token);
    }

    private ResponseEntity<?> validateInviteById(UUID token) {
        return inviteRepository.findById(token)
                .map(invite -> {
                    if (invite.isUsed()) {
                        return ResponseEntity.status(400).body(Map.of("error", "Convite já utilizado."));
                    }
                    if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return ResponseEntity.status(400).body(Map.of("error", "Convite expirado."));
                    }

                    String personalName = tenantRepository.findById(invite.getTenantId())
                            .map(Tenant::getName)
                            .orElse("Personal");

                    return ResponseEntity.ok(Map.of(
                            "valid", true,
                            "personalName", personalName,
                            "tenantId", invite.getTenantId(),
                            "email", invite.getEmail() != null ? invite.getEmail() : ""));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Convite não encontrado.")));
    }
}