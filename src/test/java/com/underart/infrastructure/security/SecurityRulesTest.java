package com.underart.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.underart.domain.model.PageResult;
import com.underart.domain.port.in.AuthenticateUserUseCase;
import com.underart.domain.port.in.CreateArtworkUseCase;
import com.underart.domain.port.in.DeleteArtworkUseCase;
import com.underart.domain.port.in.FindArtworkUseCase;
import com.underart.domain.port.in.ListArtworksUseCase;
import com.underart.domain.port.in.UpdateArtworkUseCase;
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
@Import({SecurityConfig.class, CurrentUserProvider.class})
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
    private UpdateArtworkUseCase updateArtworkUseCase;
    @MockBean
    private DeleteArtworkUseCase deleteArtworkUseCase;
    @MockBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    private void validToken() {
        when(jwtService.validateToken("good")).thenReturn(true);
        when(jwtService.extractEmail("good")).thenReturn("demo@underart.com");
        when(jwtService.extractUserId("good")).thenReturn(UUID.randomUUID());
    }

    @Test
    void rootIsPublicAndIdentifiesTheApplication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("Underart Backend"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void protectedRouteWithoutTokenReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/v1/artworks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedRouteRejectsInvalidToken() throws Exception {
        when(jwtService.validateToken("bad")).thenReturn(false);
        mockMvc.perform(get("/api/v1/artworks").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteAcceptsValidToken() throws Exception {
        validToken();
        when(listArtworksUseCase.list(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0));
        mockMvc.perform(get("/api/v1/artworks").header("Authorization", "Bearer good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void unknownRouteWithValidTokenReturnsNotFound() throws Exception {
        validToken();
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
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/artworks")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/artworks")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentationAndHealthAreNotBlockedBySecurity() throws Exception {
        for (String path : List.of("/swagger-ui.html", "/v3/api-docs", "/actuator/health")) {
            int code = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(code).as(path).isNotIn(401, 403);
        }
    }
}
