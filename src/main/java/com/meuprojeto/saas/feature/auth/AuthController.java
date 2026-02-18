package com.meuprojeto.saas.feature.auth;

import com.meuprojeto.saas.config.tenant.TenantContext;
import com.meuprojeto.saas.feature.invite.Invite;
import com.meuprojeto.saas.feature.invite.InviteRepository;
import com.meuprojeto.saas.feature.student.Student;
import com.meuprojeto.saas.feature.student.StudentRepository;
import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import com.meuprojeto.saas.feature.tenant.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TenantRepository tenantRepository;
    private final TokenService tokenService;
    private final TenantService tenantService;

    // Dependências novas necessárias para o cadastro de aluno
    private final InviteRepository inviteRepository;
    private final StudentRepository studentRepository;

    public AuthController(TenantRepository tenantRepository,
                          TokenService tokenService,
                          TenantService tenantService,
                          InviteRepository inviteRepository,
                          StudentRepository studentRepository) {
        this.tenantRepository = tenantRepository;
        this.tokenService = tokenService;
        this.tenantService = tenantService;
        this.inviteRepository = inviteRepository;
        this.studentRepository = studentRepository;
    }

    // (Mantenha o método login igual ao original)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Tenant tenant = tenantRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = tokenService.generateToken(tenant);
        return ResponseEntity.ok(Map.of("token", token));
    }

    // (Mantenha o método register igual ao original)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String schema = "tenant_" + name.toLowerCase().replace(" ", "_");
        tenantService.createTenant(name, email, schema);
        return ResponseEntity.ok(Map.of("message", "Academia criada com sucesso!", "schema", schema));
    }

    // --- NOVO MÉTODO CORRIGIDO ---
    @PostMapping("/register-student")
    public ResponseEntity<?> registerStudent(@RequestBody Map<String, String> request) {
        String tokenStr = request.get("token");
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");

        if (tokenStr == null) return ResponseEntity.badRequest().body("Token obrigatório.");

        // 1. Valida o Convite (Busca no schema public)
        UUID tokenUUID = UUID.fromString(tokenStr);
        Invite invite = inviteRepository.findById(tokenUUID)
                .orElseThrow(() -> new RuntimeException("Convite inválido."));

        if (invite.isUsed() || invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Convite inválido ou expirado.");
        }

        // 2. Descobre qual é o Schema do Personal que convidou
        Tenant tenant = tenantRepository.findById(invite.getTenantId())
                .orElseThrow(() -> new RuntimeException("Personal não encontrado."));

        try {
            // 3. TROCA O CONTEXTO PARA O SCHEMA DO PERSONAL
            // Isso faz com que o StudentRepository salve na tabela certa
            TenantContext.setTenant(tenant.getSchemaName());

            // 4. Cria o aluno
            Student student = new Student();
            student.setName(name);
            student.setEmail(email);
            student.setPassword(password); // Agora o campo existe na classe Student!
            student.setPlan("Basic");

            // NÃO PRECISA DE setTenantId, pois o schema isola os dados
            studentRepository.save(student);

        } finally {
            // 5. Limpa o contexto para voltar ao normal (public)
            TenantContext.clear();
        }

        // 6. Atualiza o convite como usado (Invite está no schema public, então funciona direto)
        invite.setUsed(true);
        inviteRepository.save(invite);

        return ResponseEntity.ok(Map.of("message", "Aluno cadastrado com sucesso!"));
    }
}