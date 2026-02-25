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

    public AuthController(TenantRepository tenantRepository,
                          TokenService tokenService,
                          TenantService tenantService,
                          InviteRepository inviteRepository,
                          StudentRepository studentRepository,
                          StudentDirectoryRepository studentDirectoryRepository,
                          PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.tokenService = tokenService;
        this.tenantService = tenantService;
        this.inviteRepository = inviteRepository;
        this.studentRepository = studentRepository;
        this.studentDirectoryRepository = studentDirectoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- LOGIN UNIFICADO ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String rawPassword = request.get("password");

        if (email == null || rawPassword == null) {
            return ResponseEntity.status(400).body(Map.of("error", "E-mail e senha são obrigatórios."));
        }

        // 1. Tenta achar como PERSONAL
        Optional<Tenant> tenantOpt = tenantRepository.findByOwnerEmail(email);
        if (tenantOpt.isPresent()) {
            Tenant tenant = tenantOpt.get();
            // Valida a senha do Personal com BCrypt
            if (!passwordEncoder.matches(rawPassword, tenant.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Usuário ou senha inválidos."));
            }
            String token = tokenService.generateToken(tenant);
            return ResponseEntity.ok(Map.of("token", token, "role", "TENANT"));
        }

        // 2. Tenta achar como ALUNO
        Optional<StudentDirectory> directoryOpt = studentDirectoryRepository.findByEmail(email);
        if (directoryOpt.isPresent()) {
            Long tenantId = directoryOpt.get().getTenantId();
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Erro: Personal do aluno não encontrado."));

            // Busca o aluno no schema correto para validar a senha
            TenantContext.setTenant(tenant.getSchemaName());
            try {
                Student student = studentRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

                if (!passwordEncoder.matches(rawPassword, student.getPassword())) {
                    return ResponseEntity.status(401).body(Map.of("error", "Usuário ou senha inválidos."));
                }

                // Token com o e-mail do próprio aluno como subject
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
            // 1. Configura o "Inspetor" do Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    // 🌟 MANTENHA A SUA CHAVE AQUI
                    .setAudience(Collections.singletonList("629004845915-6ge8nhfsdh3r8a5dd59pnvogc6875bot.apps.googleusercontent.com"))
                    .build();

            // 2. Valida o token
            GoogleIdToken idToken = verifier.verify(googleToken);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                // 3. Tenta achar como PERSONAL
                Optional<Tenant> tenantOpt = tenantRepository.findByOwnerEmail(email);
                if (tenantOpt.isPresent()) {
                    Tenant tenant = tenantOpt.get();
                    String token = tokenService.generateToken(tenant);
                    return ResponseEntity.ok(Map.of("token", token, "role", "TENANT", "name", name));
                }

                // 4. Tenta achar como ALUNO
                Optional<StudentDirectory> directoryOpt = studentDirectoryRepository.findByEmail(email);
                if (directoryOpt.isPresent()) {
                    Long tenantId = directoryOpt.get().getTenantId();
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Erro: Personal do aluno não encontrado."));

                    String token = tokenService.generateStudentToken(tenant, email);
                    return ResponseEntity.ok(Map.of("token", token, "role", "STUDENT", "name", name));
                }

                // 🌟 5. A MÁGICA DO CADASTRO AUTOMÁTICO (Sign-Up) 🌟
                // Se o Google confirmou, mas a pessoa não tem conta, criamos o Personal agora mesmo!

                // Trata o nome e gera um schema único sem espaços ou caracteres especiais
                String tenantName = (name != null && !name.isEmpty()) ? name : email.split("@")[0];
                String schemaName = "tenant_" + email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

                // Gera senha invisível (o usuário sempre usará o Google)
                String randomPassword = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(randomPassword);

                // Chama o tenantService para garantir que os schemas/tabelas sejam criados
                // Passamos 'null' no telefone, pois o Google não envia essa informação
                tenantService.createTenant(tenantName, email, schemaName, encodedPassword, null);

                // Busca o Personal recém-criado para gerar o Token
                Tenant novoTenant = tenantRepository.findByOwnerEmail(email)
                        .orElseThrow(() -> new RuntimeException("Erro ao recuperar a conta recém-criada via Google."));

                String token = tokenService.generateToken(novoTenant);

                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "role", "TENANT",
                        "name", tenantName,
                        "isNewAccount", true // Flag para o Frontend saber que é conta nova (se quiser mostrar um "Bem-vindo")
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
        Integer age = null;
        if (request.get("age") != null) {
            age = Integer.parseInt(request.get("age").toString());
        }

        if (tokenStr == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Token obrigatório."));
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Nome obrigatório."));
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "E-mail obrigatório."));
        if (password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Senha obrigatória."));

        UUID tokenUUID;
        try {
            tokenUUID = UUID.fromString(tokenStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token em formato inválido."));
        }

        Invite invite = inviteRepository.findById(tokenUUID)
                .orElseThrow(() -> new RuntimeException("Convite inválido."));

        if (invite.isUsed() || invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Convite inválido ou expirado."));
        }

        Tenant tenant = tenantRepository.findById(invite.getTenantId())
                .orElseThrow(() -> new RuntimeException("Personal não encontrado."));

        try {
            TenantContext.setTenant(tenant.getSchemaName());

            Optional<Student> existingStudent = studentRepository.findByEmail(email);
            if(existingStudent.isPresent()){
                return ResponseEntity.badRequest().body(Map.of("error", "Este e-mail já está cadastrado nesta academia."));
            }

            Optional<Student> existingPhone = studentRepository.findByPhone(phone);
            if(existingPhone.isPresent()){
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

        StudentDirectory directory = new StudentDirectory(email, tenant.getId());
        studentDirectoryRepository.save(directory);

        invite.setUsed(true);
        inviteRepository.save(invite);

        return ResponseEntity.ok(Map.of("message", "Aluno cadastrado com sucesso!"));
    }
}