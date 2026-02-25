package com.meuprojeto.saas.feature.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meuprojeto.saas.config.SecurityConfig;
import com.meuprojeto.saas.feature.invite.Invite;
import com.meuprojeto.saas.feature.invite.InviteRepository;
import com.meuprojeto.saas.feature.student.StudentDirectoryRepository;
import com.meuprojeto.saas.feature.student.StudentRepository;
import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import com.meuprojeto.saas.feature.tenant.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtFilter.class, TokenService.class })
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private TenantRepository tenantRepository;

        @MockBean
        private TenantService tenantService;

        @MockBean
        private InviteRepository inviteRepository;

        @MockBean
        private StudentRepository studentRepository;

        @MockBean
        private StudentDirectoryRepository studentDirectoryRepository;

        @MockBean
        private PasswordEncoder passwordEncoder;

        private Tenant tenant;

        @BeforeEach
        void setUp() {
                tenant = new Tenant();
                tenant.setId(1L);
                tenant.setName("Academia Teste");
                tenant.setOwnerEmail("personal@teste.com");
                tenant.setSchemaName("tenant_academia_teste");
                tenant.setPassword("$2a$10$encodedPassword");
                tenant.setPlan("FREE");
        }

        // ==================== TESTES DE LOGIN ====================

        @Test
        @DisplayName("POST /auth/login sem email e senha deve retornar 400")
        void login_semCredenciais_deveRetornar400() throws Exception {
                mockMvc.perform(post("/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of())))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("POST /auth/login com credenciais corretas de Personal deve retornar 200 com role TENANT")
        void login_comCredenciaisDePersonal_deveRetornar200() throws Exception {
                when(tenantRepository.findByOwnerEmail("personal@teste.com")).thenReturn(Optional.of(tenant));
                when(passwordEncoder.matches("senha123", tenant.getPassword())).thenReturn(true);

                Map<String, String> body = Map.of("email", "personal@teste.com", "password", "senha123");

                mockMvc.perform(post("/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").exists())
                                .andExpect(jsonPath("$.role").value("TENANT"));
        }

        @Test
        @DisplayName("POST /auth/login com senha errada deve retornar 401")
        void login_comSenhaErrada_deveRetornar401() throws Exception {
                when(tenantRepository.findByOwnerEmail("personal@teste.com")).thenReturn(Optional.of(tenant));
                when(passwordEncoder.matches("senhaErrada", tenant.getPassword())).thenReturn(false);

                Map<String, String> body = Map.of("email", "personal@teste.com", "password", "senhaErrada");

                mockMvc.perform(post("/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("POST /auth/login com email não cadastrado deve retornar 401")
        void login_comEmailNaoCadastrado_deveRetornar401() throws Exception {
                when(tenantRepository.findByOwnerEmail(anyString())).thenReturn(Optional.empty());
                when(studentDirectoryRepository.findByEmail(anyString())).thenReturn(Optional.empty());

                Map<String, String> body = Map.of("email", "naoexiste@teste.com", "password", "qualquer");

                mockMvc.perform(post("/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isUnauthorized());
        }

        // ==================== TESTES DE CADASTRO DE PERSONAL ====================

        @Test
        @DisplayName("POST /auth/register com dados válidos deve retornar 200")
        void register_comDadosValidos_deveRetornar200() throws Exception {
                when(tenantRepository.findByPhone(anyString())).thenReturn(Optional.empty());
                when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");

                Map<String, String> body = Map.of(
                                "name", "Academia Nova",
                                "email", "novo@academia.com",
                                "password", "senha123",
                                "phone", "11999999999");

                mockMvc.perform(post("/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("POST /auth/register sem campos obrigatórios deve retornar 400")
        void register_semCamposObrigatorios_deveRetornar400() throws Exception {
                Map<String, String> body = Map.of("name", "Academia Nova");

                mockMvc.perform(post("/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("POST /auth/register com telefone já cadastrado deve retornar 400")
        void register_comTelefoneDuplicado_deveRetornar400() throws Exception {
                when(tenantRepository.findByPhone("11999999999")).thenReturn(Optional.of(tenant));

                Map<String, String> body = Map.of(
                                "name", "Academia Nova",
                                "email", "novo@academia.com",
                                "password", "senha123",
                                "phone", "11999999999");

                mockMvc.perform(post("/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error")
                                                .value("Este número de WhatsApp já está registado num Personal."));
        }

        // ==================== TESTES DE CADASTRO DE ALUNO ====================

        @Test
        @DisplayName("POST /auth/register-student com token inválido (formato errado) deve retornar 400")
        void registerStudent_comTokenFormatoErrado_deveRetornar400() throws Exception {
                Map<String, Object> body = Map.of(
                                "token", "nao-e-uuid",
                                "name", "Aluno Teste",
                                "email", "aluno@teste.com",
                                "password", "senha123");

                mockMvc.perform(post("/auth/register-student")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Token em formato inválido."));
        }

        @Test
        @DisplayName("POST /auth/register-student sem token deve retornar 400")
        void registerStudent_semToken_deveRetornar400() throws Exception {
                Map<String, Object> body = Map.of(
                                "name", "Aluno Teste",
                                "email", "aluno@teste.com",
                                "password", "senha123");

                mockMvc.perform(post("/auth/register-student")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Token obrigatório."));
        }

        @Test
        @DisplayName("POST /auth/register-student com convite expirado deve retornar 400")
        void registerStudent_comConviteExpirado_deveRetornar400() throws Exception {
                UUID tokenUUID = UUID.randomUUID();

                Invite expiredInvite = Invite.builder()
                                .tenantId(1L)
                                .used(false)
                                .expiresAt(LocalDateTime.now().minusHours(1)) // expirado
                                .build();

                when(inviteRepository.findById(tokenUUID)).thenReturn(Optional.of(expiredInvite));

                Map<String, Object> body = Map.of(
                                "token", tokenUUID.toString(),
                                "name", "Aluno Teste",
                                "email", "aluno@teste.com",
                                "password", "senha123",
                                "phone", "11988888888");

                mockMvc.perform(post("/auth/register-student")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Convite inválido ou expirado."));
        }

        @Test
        @DisplayName("POST /auth/register-student com convite já usado deve retornar 400")
        void registerStudent_comConviteJaUsado_deveRetornar400() throws Exception {
                UUID tokenUUID = UUID.randomUUID();

                Invite usedInvite = Invite.builder()
                                .tenantId(1L)
                                .used(true)
                                .expiresAt(LocalDateTime.now().plusHours(48))
                                .build();

                when(inviteRepository.findById(tokenUUID)).thenReturn(Optional.of(usedInvite));

                Map<String, Object> body = Map.of(
                                "token", tokenUUID.toString(),
                                "name", "Aluno Teste",
                                "email", "aluno@teste.com",
                                "password", "senha123",
                                "phone", "11988888888");

                mockMvc.perform(post("/auth/register-student")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Convite inválido ou expirado."));
        }
}
