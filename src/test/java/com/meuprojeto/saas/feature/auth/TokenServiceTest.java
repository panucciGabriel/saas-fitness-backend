package com.meuprojeto.saas.feature.auth;

import com.meuprojeto.saas.feature.tenant.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private TokenService tokenService;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        // Inicializa o serviço com uma chave de teste (>= 32 bytes)
        tokenService = new TokenService("TestSecretKeyForJwtThatIsLongEnough123456789012");

        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Academia Teste");
        tenant.setOwnerEmail("personal@teste.com");
        tenant.setSchemaName("tenant_academia_teste");
        tenant.setPassword("senha_encodada");
        tenant.setPlan("FREE");
    }

    @Test
    @DisplayName("Deve gerar um token JWT não-nulo para um Tenant")
    void generateToken_deveRetornarTokenNaoNulo() {
        String token = tokenService.generateToken(tenant);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Deve extrair o e-mail correto do token de Tenant")
    void extractUsername_deveRetornarEmailDoTenant() {
        String token = tokenService.generateToken(tenant);
        String username = tokenService.extractUsername(token);
        assertThat(username).isEqualTo("personal@teste.com");
    }

    @Test
    @DisplayName("Deve extrair o schema correto do token de Tenant")
    void extractSchema_deveRetornarSchemaDoTenant() {
        String token = tokenService.generateToken(tenant);
        String schema = tokenService.extractSchema(token);
        assertThat(schema).isEqualTo("tenant_academia_teste");
    }

    @Test
    @DisplayName("Token válido deve ser reconhecido como válido")
    void isTokenValid_comTokenValido_deveRetornarTrue() {
        String token = tokenService.generateToken(tenant);
        boolean isValid = tokenService.isTokenValid(token, "personal@teste.com");
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Token com email errado deve ser considerado inválido")
    void isTokenValid_comEmailErrado_deveRetornarFalse() {
        String token = tokenService.generateToken(tenant);
        boolean isValid = tokenService.isTokenValid(token, "outro@email.com");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar token de aluno com o email correto como subject")
    void generateStudentToken_deveUsarEmailDoAlunoComoSubject() {
        String studentEmail = "aluno@teste.com";
        String token = tokenService.generateStudentToken(tenant, studentEmail);
        String username = tokenService.extractUsername(token);
        assertThat(username).isEqualTo(studentEmail);
    }

    @Test
    @DisplayName("Token de aluno deve conter o schema do tenant")
    void generateStudentToken_deveConterSchemaDoTenant() {
        String token = tokenService.generateStudentToken(tenant, "aluno@teste.com");
        String schema = tokenService.extractSchema(token);
        assertThat(schema).isEqualTo("tenant_academia_teste");
    }
}
