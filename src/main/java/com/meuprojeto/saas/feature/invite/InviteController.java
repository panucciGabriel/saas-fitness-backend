package com.meuprojeto.saas.feature.invite;

import com.meuprojeto.saas.config.tenant.TenantContext;
import com.meuprojeto.saas.feature.student.StudentRepository;
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

    private final InviteRepository inviteRepository;
    private final TenantRepository tenantRepository;
    private final StudentRepository studentRepository; // 🌟 NOVO: Injetando o repositório de alunos

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public InviteController(InviteRepository inviteRepository,
                            TenantRepository tenantRepository,
                            StudentRepository studentRepository) {
        this.inviteRepository = inviteRepository;
        this.tenantRepository = tenantRepository;
        this.studentRepository = studentRepository;
    }

    // --- 1. CRIAR CONVITE (POST) ---
    @PostMapping
    public ResponseEntity<?> createInvite() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(403).body(Map.of("error", "Usuário não autenticado."));
        }

        String ownerEmail = auth.getName();
        Tenant tenant = tenantRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Personal não encontrado."));

        // 🌟 NOVA REGRA DE NEGÓCIO: Limite de 5 alunos no Plano Free
        if ("FREE".equalsIgnoreCase(tenant.getPlan())) {
            TenantContext.setTenant(tenant.getSchemaName()); // Entra no banco do Personal
            try {
                long studentCount = studentRepository.count(); // Conta os alunos
                if (studentCount >= 5) {
                    return ResponseEntity.status(403).body(Map.of(
                            "error", "Limite do Plano Grátis atingido! Você já possui 5 alunos. Faça o upgrade para adicionar mais."
                    ));
                }
            } finally {
                TenantContext.clear(); // Sai do banco do Personal
            }
        }

        // Se passou do limite, cria o convite normalmente
        Invite invite = Invite.builder()
                .tenantId(tenant.getId())
                .email(null)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        inviteRepository.save(invite);

        String link = frontendUrl + "/register?token=" + invite.getId();

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