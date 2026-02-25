package com.meuprojeto.saas.feature.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meuprojeto.saas.config.SecurityConfig;
import com.meuprojeto.saas.feature.auth.JwtFilter;
import com.meuprojeto.saas.feature.auth.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
@Import({ SecurityConfig.class, JwtFilter.class, TokenService.class })
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentRepository studentRepository;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setName("João Silva");
        student.setEmail("joao@academia.com");
        student.setPlan("Basic");
        student.setPhone("11999999999");
        student.setAge(25);
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("GET /api/students deve retornar lista de alunos com 200")
    void list_deveRetornarListaDeAlunos() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of(student));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("João Silva"))
                .andExpect(jsonPath("$[0].email").value("joao@academia.com"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("GET /api/students deve retornar lista vazia quando não há alunos")
    void list_deveRetornarListaVazia() throws Exception {
        when(studentRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("POST /api/students deve criar aluno e retornar 200")
    void create_deveRetornar200ComAlunoCriado() throws Exception {
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        mockMvc.perform(post("/api/students")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@academia.com"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("PUT /api/students/{id} com ID existente deve atualizar e retornar 200")
    void update_comIdExistente_deveRetornar200() throws Exception {
        Student alunoAtualizado = new Student();
        alunoAtualizado.setId(1L);
        alunoAtualizado.setName("João Atualizado");
        alunoAtualizado.setEmail("joao@academia.com");
        alunoAtualizado.setPlan("Premium");
        alunoAtualizado.setPhone("11999999999");
        alunoAtualizado.setAge(26);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(alunoAtualizado);

        mockMvc.perform(put("/api/students/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alunoAtualizado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Atualizado"))
                .andExpect(jsonPath("$.plan").value("Premium"));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("PUT /api/students/{id} com ID inexistente deve retornar 404")
    void update_comIdInexistente_deveRetornar404() throws Exception {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/students/99")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(student)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("DELETE /api/students/{id} com ID existente deve retornar 204")
    void delete_comIdExistente_deveRetornar204() throws Exception {
        when(studentRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/students/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("DELETE /api/students/{id} com ID inexistente deve retornar 404")
    void delete_comIdInexistente_deveRetornar404() throws Exception {
        when(studentRepository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/students/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("GET /api/students/stats deve retornar total de alunos")
    void getStats_deveRetornarTotalDeAlunos() throws Exception {
        when(studentRepository.count()).thenReturn(5L);

        mockMvc.perform(get("/api/students/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(5))
                .andExpect(jsonPath("$.activePlans").value(5));
    }
}
