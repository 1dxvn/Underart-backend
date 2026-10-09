package com.underart.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.out.UserRepositoryPort;
import com.underart.infrastructure.web.controller.ArtworkController;
import com.underart.infrastructure.web.controller.AuthController;
import com.underart.infrastructure.web.controller.RootController;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {RootController.class, ArtworkController.class, AuthController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private CreateArtworkUseCase createArtworkUseCase;
    @MockBean
    private FindArtworkUseCase findArtworkUseCase;
    @MockBean
    private ListArtworksUseCase listArtworksUseCase;
    @MockBean
    private UserRepositoryPort userRepository;

    @Test
    void rootIsPublicAndIdentifiesTheApplication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("Underart Backend"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void protectedRouteRejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/artworks")).andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRejectsInvalidToken() throws Exception {
        when(jwtService.validateToken("bad")).thenReturn(false);
        mockMvc.perform(get("/api/v1/artworks").header("Authorization", "Bearer bad"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        when(jwtService.validateToken("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("demo@underart.com");
        when(jwtService.extractUserId("good")).thenReturn(UUID.randomUUID());
        when(listArtworksUseCase.list(null)).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/artworks").header("Authorization", "Bearer good"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownRouteWithValidTokenReturnsNotFound() throws Exception {
        when(jwtService.validateToken("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("demo@underart.com");
        when(jwtService.extractUserId("good")).thenReturn(UUID.randomUUID());
        mockMvc.perform(get("/api/v1/nope").header("Authorization", "Bearer good"))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swaggerUiHtmlPathIsNotBlockedBySecurity() throws Exception {
        int status = mockMvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(403);
    }

    @Test
    void apiDocsPathIsNotBlockedBySecurity() throws Exception {
        int status = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(403);
    }
}
