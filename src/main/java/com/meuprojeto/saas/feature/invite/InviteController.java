package com.meuprojeto.saas.feature.invite;

import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<?> createInvite(@AuthenticationPrincipal String ownerEmail, @RequestBody Map<String, String> body) {

        Tenant tenant = tenantRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Personal não encontrado."));

        String studentEmail = body.get("email");

        Invite invite = Invite.builder()
                .tenantId(tenant.getId())
                .email(studentEmail)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        inviteRepository.save(invite);

        String link = "https://saas-fitness-frontend.vercel.app/register" + invite.getId();

        return ResponseEntity.ok(Map.of(
                "link", link,
                "token", invite.getId(),
                "expiresAt", invite.getExpiresAt()
        ));
    }

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

                    // Busca nome do personal para mostrar na tela: "Cadastre-se na academia X"
                    String personalName = tenantRepository.findById(invite.getTenantId())
                            .map(Tenant::getName)
                            .orElse("Personal");

                    return ResponseEntity.ok(Map.of(
                            "valid", true,
                            "personalName", personalName,
                            "tenantId", invite.getTenantId(),
                            "email", invite.getEmail() != null ? invite.getEmail() : ""
                    ));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Convite não encontrado.")));
    }

}
