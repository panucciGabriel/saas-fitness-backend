package com.meuprojeto.saas.feature.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.meuprojeto.saas.config.tenant.TenantContext;
import com.meuprojeto.saas.feature.invite.Invite;
import com.meuprojeto.saas.feature.invite.InviteRepository;
import com.meuprojeto.saas.feature.student.Student;
import com.meuprojeto.saas.feature.student.StudentDirectory;
import com.meuprojeto.saas.feature.student.StudentDirectoryRepository;
import com.meuprojeto.saas.feature.student.StudentRepository;
import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import com.meuprojeto.saas.feature.tenant.TenantService;

// 🌟 NOVOS IMPORTS DA RECUPERAÇÃO DE SENHA
import com.meuprojeto.saas.feature.password.EmailService;
import com.meuprojeto.saas.feature.password.PasswordResetToken;
import com.meuprojeto.saas.feature.password.PasswordResetTokenRepository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TenantRepository tenantRepository;
    private final TokenService tokenService;
    private final TenantService tenantService;
    private final InviteRepository inviteRepository;
    private final StudentRepository studentRepository;
    private final StudentDirectoryRepository studentDirectoryRepository;
    private final PasswordEncoder passwordEncoder;

    // 🌟 NOVAS INJEÇÕES
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthController(TenantRepository tenantRepository,
                          TokenService tokenService,
                          TenantService tenantService,
                          InviteRepository inviteRepository,
                          StudentRepository studentRepository,
                          StudentDirectoryRepository studentDirectoryRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          EmailService emailService) {
        this.tenantRepository = tenantRepository;
        this.tokenService = tokenService;
        this.tenantService = tenantService;
        this.inviteRepository = inviteRepository;
        this.studentRepository = studentRepository;
        this.studentDirectoryRepository = studentDirectoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    // --- LOGIN UNIFICADO ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String rawPassword = request.get("password");

        if (email == null || rawPassword == null) {
            return ResponseEntity.status(400).body(Map.of("error", "E-mail e senha são obrigatórios."));
        }

        Optional<Tenant> tenantOpt = tenantRepository.findByOwnerEmail(email);
        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            if (!passwordEncoder.matches(rawPassword, tenant.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Usuário ou senha inválidos."));
            }
            String token = tokenService.generateToken(tenant);
            return ResponseEntity.ok(Map.of("token", token, "role", "TENANT"));
        }

        Optional<StudentDirectory> directoryOpt = studentDirectoryRepository.findByEmail(email);
        if (directoryOpt.isPresent()) {
            Long tenantId = directoryOpt.get().getTenantId();
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Erro: Personal do aluno não encontrado."));

            TenantContext.setTenant(tenant.getSchemaName());
            try {
                Student student = studentRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

                if (!passwordEncoder.matches(rawPassword, student.getPassword())) {
                    return ResponseEntity.status(401).body(Map.of("error", "Usuário ou senha inválidos."));
                }

                String token = tokenService.generateStudentToken(tenant, email);
                return ResponseEntity.ok(Map.of("token", token, "role", "STUDENT"));
            } finally {
                TenantContext.clear();
            }
        }

        return ResponseEntity.status(401).body(Map.of("error", "Usuário ou senha inválidos."));
    }

    // --- LOGIN E CADASTRO COM GOOGLE ---
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String googleToken = request.get("token");

        if (googleToken == null || googleToken.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Token do Google não enviado."));
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("629004845915-6ge8nhfsdh3r8a5dd59pnvogc6875bot.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleToken);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                Optional<Tenant> tenantOpt = tenantRepository.findByOwnerEmail(email);
                if (tenantOpt.isPresent()) {
                    Tenant tenant = tenantOpt.get();
                    String token = tokenService.generateToken(tenant);
                    return ResponseEntity.ok(Map.of("token", token, "role", "TENANT", "name", name));
                }

                Optional<StudentDirectory> directoryOpt = studentDirectoryRepository.findByEmail(email);
                if (directoryOpt.isPresent()) {
                    Long tenantId = directoryOpt.get().getTenantId();
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Erro: Personal do aluno não encontrado."));

                    String token = tokenService.generateStudentToken(tenant, email);
                    return ResponseEntity.ok(Map.of("token", token, "role", "STUDENT", "name", name));
                }

                String tenantName = (name != null && !name.isEmpty()) ? name : email.split("@")[0];
                String schemaName = "tenant_" + email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                String randomPassword = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(randomPassword);

                tenantService.createTenant(tenantName, email, schemaName, encodedPassword, null);

                Tenant novoTenant = tenantRepository.findByOwnerEmail(email)
                        .orElseThrow(() -> new RuntimeException("Erro ao recuperar a conta recém-criada via Google."));

                String token = tokenService.generateToken(novoTenant);

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", "TENANT",
                        "name", tenantName,
                        "isNewAccount", true
                ));

            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Token do Google inválido ou expirado."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao validar com o Google: " + e.getMessage()));
        }
    }

    // --- CADASTRO DE PERSONAL (Tradicional) ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");
        String phone = request.get("phone");

        if (name == null || email == null || password == null || phone == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nome, e-mail, senha e WhatsApp são obrigatórios."));
        }

        if (tenantRepository.findByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este número de WhatsApp já está registado num Personal."));
        }

        try {
            String schema = "tenant_" + name.toLowerCase().replace(" ", "_");
            tenantService.createTenant(name, email, schema, passwordEncoder.encode(password), phone);

            return ResponseEntity.ok(Map.of("message", "Academia criada com sucesso!", "schema", schema));

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este e-mail ou nome de academia já está em uso. Escolha outro."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno ao criar conta."));
        }
    }

    // --- CADASTRO DE ALUNO (SELF-SERVICE) ---
    @PostMapping("/register-student")
    public ResponseEntity<?> registerStudent(@RequestBody Map<String, Object> request) {
        String tokenStr = (String) request.get("token");
        String name = (String) request.get("name");
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        String phone = (String) request.get("phone");
        Integer age = request.get("age") != null ? Integer.parseInt(request.get("age").toString()) : null;

        if (tokenStr == null || name == null || email == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Dados obrigatórios faltando."));

        UUID tokenUUID;
        try {
            tokenUUID = UUID.fromString(tokenStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token em formato inválido."));
        }

        Invite invite = inviteRepository.findById(tokenUUID).orElseThrow(() -> new RuntimeException("Convite inválido."));

        if (invite.isUsed() || invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Convite inválido ou expirado."));
        }

        Tenant tenant = tenantRepository.findById(invite.getTenantId()).orElseThrow(() -> new RuntimeException("Personal não encontrado."));

        try {
            TenantContext.setTenant(tenant.getSchemaName());

            if(studentRepository.findByEmail(email).isPresent()){
                return ResponseEntity.badRequest().body(Map.of("error", "Este e-mail já está cadastrado nesta academia."));
            }
            if(studentRepository.findByPhone(phone).isPresent()){
                return ResponseEntity.badRequest().body(Map.of("error", "Este número de WhatsApp já está em uso nesta academia."));
            }

            Student student = new Student();
            student.setName(name);
            student.setEmail(email);
            student.setPassword(passwordEncoder.encode(password));
            student.setPhone(phone);
            student.setAge(age);
            student.setPlan("Basic");

            studentRepository.save(student);
        } finally {
            TenantContext.clear();
        }

        studentDirectoryRepository.save(new StudentDirectory(email, tenant.getId()));
        invite.setUsed(true);
        inviteRepository.save(invite);

        return ResponseEntity.ok(Map.of("message", "Aluno cadastrado com sucesso!"));
    }

    // 🌟 -------------------------------------------------------- 🌟
    // 🌟 NOVOS ENDPOINTS DE RECUPERAÇÃO DE SENHA (O DETETIVE)     🌟
    // 🌟 -------------------------------------------------------- 🌟

    @Transactional
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) return ResponseEntity.badRequest().body(Map.of("error", "E-mail obrigatório."));

        String userType = null;

        // O Detetive: É um Personal?
        if (tenantRepository.findByOwnerEmail(email).isPresent()) {
            userType = "TENANT";
        }
        // O Detetive: É um Aluno?
        else if (studentDirectoryRepository.findByEmail(email).isPresent()) {
            userType = "STUDENT";
        }

        // Se achou alguém com esse e-mail no sistema
        if (userType != null) {
            // Limpa tokens velhos para não acumular lixo
            passwordResetTokenRepository.deleteByEmail(email);

            // Gera o código gigante
            String tokenStr = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken(tokenStr, email, userType, LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(token);

            // 🚀 Dispara o e-mail!
            emailService.sendPasswordResetEmail(email, tokenStr);
        }

        // Retornamos sempre a mesma mensagem (Prática de segurança: não confirmar se o e-mail existe para hackers)
        return ResponseEntity.ok(Map.of("message", "Se o e-mail estiver cadastrado, receberá um link de recuperação."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String tokenStr = request.get("token");
        String newPassword = request.get("newPassword");

        if (tokenStr == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token e nova senha são obrigatórios."));
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(tokenStr);

        // Verifica se o código existe e não passou de 1 hora
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Link de recuperação inválido ou expirado."));
        }

        PasswordResetToken token = tokenOpt.get();
        String encodedPassword = passwordEncoder.encode(newPassword);

        if ("TENANT".equals(token.getUserType())) {
            // Atualiza a senha do Personal
            Tenant tenant = tenantRepository.findByOwnerEmail(token.getEmail()).orElseThrow();
            tenant.setPassword(encodedPassword);
            tenantRepository.save(tenant);
        } else {
            // Atualiza a senha do Aluno (Entra no schema correto)
            StudentDirectory dir = studentDirectoryRepository.findByEmail(token.getEmail()).orElseThrow();
            Tenant tenant = tenantRepository.findById(dir.getTenantId()).orElseThrow();

            TenantContext.setTenant(tenant.getSchemaName());
            try {
                Student student = studentRepository.findByEmail(token.getEmail()).orElseThrow();
                student.setPassword(encodedPassword);
                studentRepository.save(student);
            } finally {
                TenantContext.clear();
            }
        }

        // Destrói o token para não ser usado 2 vezes
        passwordResetTokenRepository.delete(token);

        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso!"));
    }
}