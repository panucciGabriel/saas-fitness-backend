package com.meuprojeto.saas.feature.invite;

import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
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

    private final InviteRepository inviteRepository;
    private final TenantRepository tenantRepository;

    public InviteController(InviteRepository inviteRepository, TenantRepository tenantRepository) {
        this.inviteRepository = inviteRepository;
        this.tenantRepository = tenantRepository;
    }

    // --- 1. CRIAR CONVITE (POST) ---
    // Não recebe @RequestBody, pois é apenas um clique no botão.
    @PostMapping
    public ResponseEntity<?> createInvite() {
        // Pega o usuário logado do contexto de segurança
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(403).body("Usuário não autenticado.");
        }

        String ownerEmail = auth.getName();

        Tenant tenant = tenantRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Personal não encontrado."));

        // Cria convite genérico (email null)
        Invite invite = Invite.builder()
                .tenantId(tenant.getId())
                .email(null)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        inviteRepository.save(invite);

        // Gera o link (Ajuste a URL base se for rodar em produção)
        String link = "http://localhost:5173/register?token=" + invite.getId();

        return ResponseEntity.ok(Map.of(
                "link", link,
                "token", invite.getId(),
                "expiresAt", invite.getExpiresAt()
        ));
    }

    // --- 2. VALIDAR CONVITE (GET) ---
    @GetMapping("/{token}")
    public ResponseEntity<?> validateInvite(@PathVariable UUID token) {
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
                            "tenantId", invite.getTenantId()
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Convite não encontrado.")));
    }
}