package com.meuprojeto.saas.feature.invite;

import com.meuprojeto.saas.config.SecurityConfig;
import com.meuprojeto.saas.feature.auth.JwtFilter;
import com.meuprojeto.saas.feature.auth.TokenService;
import com.meuprojeto.saas.feature.student.StudentRepository;
import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InviteController.class)
@Import({ SecurityConfig.class, JwtFilter.class, TokenService.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
                "app.frontend.url=http://localhost:5173",
                "api.security.token.secret=TestSecretKeyForJwtThatIsLongEnough123456789012"
})
class InviteControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private InviteRepository inviteRepository;

        @MockBean
        private TenantRepository tenantRepository;

        @MockBean
        private StudentRepository studentRepository;

        private Tenant tenant;
        private Invite invite;
        private UUID inviteToken;

        @BeforeEach
        void setUp() {
                tenant = new Tenant();
                tenant.setId(1L);
                tenant.setName("Academia Teste");
                tenant.setOwnerEmail("personal@teste.com");
                tenant.setSchemaName("tenant_academia_teste");
                tenant.setPassword("$2a$10$encodedPassword");
                tenant.setPlan("PRO"); // PRO = sem limite de alunos

                inviteToken = UUID.randomUUID();
                invite = Invite.builder()
                                .tenantId(1L)
                                .used(false)
                                .expiresAt(LocalDateTime.now().plusHours(48))
                                .build();
        }

        // ==================== TESTES DE CRIAÇÃO DE CONVITE ====================

        @Test
        @WithMockUser(username = "personal@teste.com")
        @DisplayName("POST /api/invites autenticado deve criar convite e retornar 200 com link")
        void createInvite_autenticado_deveRetornar200ComLink() throws Exception {
                when(tenantRepository.findByOwnerEmail("personal@teste.com")).thenReturn(Optional.of(tenant));

                // O Invite retornado pelo save() precisa ter ID definido para o Map.of() não
                // lançar NPE.
                // O Lombok @Data gera setId(), então podemos defini-lo diretamente.
                UUID generatedId = UUID.randomUUID();
                Invite savedInvite = Invite.builder()
                                .tenantId(1L)
                                .used(false)
                                .expiresAt(LocalDateTime.now().plusHours(48))
                                .build();
                savedInvite.setId(generatedId);

                when(inviteRepository.save(any(Invite.class))).thenReturn(savedInvite);

                mockMvc.perform(post("/api/invites").with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.link").exists())
                                .andExpect(jsonPath("$.expiresAt").exists());
        }

        @Test
        @DisplayName("POST /api/invites sem autenticação deve retornar 403")
        void createInvite_semAutenticacao_deveRetornar403() throws Exception {
                mockMvc.perform(post("/api/invites").with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "personal@teste.com")
        @DisplayName("POST /api/invites com Plano FREE e 5 alunos deve retornar 403")
        void createInvite_planoFreeComLimiteAtingido_deveRetornar403() throws Exception {
                Tenant tenantFree = new Tenant();
                tenantFree.setId(1L);
                tenantFree.setName("Academia Free");
                tenantFree.setOwnerEmail("personal@teste.com");
                tenantFree.setSchemaName("tenant_academia_free");
                tenantFree.setPassword("$2a$10$encodedPassword");
                tenantFree.setPlan("FREE");

                when(tenantRepository.findByOwnerEmail("personal@teste.com")).thenReturn(Optional.of(tenantFree));
                when(studentRepository.count()).thenReturn(5L); // 5 alunos = limite atingido

                mockMvc.perform(post("/api/invites").with(csrf()))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.error").exists());
        }

        // ==================== TESTES DE VALIDAÇÃO DE CONVITE ====================

        @Test
        @DisplayName("GET /api/invites/{token} com convite válido deve retornar 200")
        void validateInvite_comConviteValido_deveRetornar200() throws Exception {
                when(inviteRepository.findById(inviteToken)).thenReturn(Optional.of(invite));
                when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

                mockMvc.perform(get("/api/invites/{token}", inviteToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.personalName").value("Academia Teste"));
        }

        @Test
        @DisplayName("GET /api/invites/{token} com token inexistente deve retornar 404")
        void validateInvite_comTokenInexistente_deveRetornar404() throws Exception {
                UUID tokenDesconhecido = UUID.randomUUID();
                when(inviteRepository.findById(tokenDesconhecido)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/invites/{token}", tokenDesconhecido))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("Convite não encontrado."));
        }

        @Test
        @DisplayName("GET /api/invites/{token} com convite expirado deve retornar 400")
        void validateInvite_comConviteExpirado_deveRetornar400() throws Exception {
                Invite expiredInvite = Invite.builder()
                                .tenantId(1L)
                                .used(false)
                                .expiresAt(LocalDateTime.now().minusHours(1)) // expirado
                                .build();

                when(inviteRepository.findById(inviteToken)).thenReturn(Optional.of(expiredInvite));

                mockMvc.perform(get("/api/invites/{token}", inviteToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Convite expirado."));
        }

        @Test
        @DisplayName("GET /api/invites/{token} com convite já utilizado deve retornar 400")
        void validateInvite_comConviteJaUtilizado_deveRetornar400() throws Exception {
                Invite usedInvite = Invite.builder()
                                .tenantId(1L)
                                .used(true)
                                .expiresAt(LocalDateTime.now().plusHours(48))
                                .build();

                when(inviteRepository.findById(inviteToken)).thenReturn(Optional.of(usedInvite));

                mockMvc.perform(get("/api/invites/{token}", inviteToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Convite já utilizado."));
        }
}
