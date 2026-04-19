package rs.teslaris.core.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import rs.teslaris.core.dto.user.AuthenticationResponseDTO;
import rs.teslaris.core.util.functional.Pair;

@SpringBootTest
public abstract class BaseTest {

    private static final Set<String> SECURITY_HEADERS = Set.of(
        "X-XSS-Protection", "Content-Security-Policy",
        "X-Frame-Options", "Cache-Control", "Pragma", "Expires"
    );
    protected MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .addFilter(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                    throws ServletException, IOException {
                    filterChain.doFilter(request, new HttpServletResponseWrapper(response) {
                        @Override
                        public void setHeader(String name, String value) {
                            if (!SECURITY_HEADERS.contains(name)) {
                                super.setHeader(name, value);
                            }
                        }

                        @Override
                        public void addHeader(String name, String value) {
                            if (!SECURITY_HEADERS.contains(name)) {
                                super.addHeader(name, value);
                            }
                        }
                    });
                }
            }, "/*")
            .build();
    }

    protected synchronized String authenticateAdminAndGetToken() throws Exception {
        String authResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content("{\"email\": \"admin@admin.com\", \"password\": \"admin\"}")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(authResponse, AuthenticationResponseDTO.class);

        return authenticationResponseDTO.getToken();
    }

    protected synchronized Pair<String, Cookie> authenticateAdminAndGetTokenWithFingerprintCookie()
        throws Exception {
        var result = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content("{\"email\": \"admin@admin.com\", \"password\": \"admin\"}")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(result.getResponse().getContentAsString(),
                AuthenticationResponseDTO.class);

        var fingerprintCookie = result.getResponse().getCookie("jwt-security-fingerprint");
        return new Pair<>(authenticationResponseDTO.getToken(), fingerprintCookie);
    }

    protected synchronized String authenticateInstitutionalEditorAndGetToken() throws Exception {
        String authResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content("{\"email\": \"editor@editor.com\", \"password\": \"editor\"}")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(authResponse, AuthenticationResponseDTO.class);

        return authenticationResponseDTO.getToken();
    }

    protected synchronized String authenticateLibrarianAndGetToken() throws Exception {
        String authResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content("{\"email\": \"librarian@librarian.com\", \"password\": \"librarian\"}")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(authResponse, AuthenticationResponseDTO.class);

        return authenticationResponseDTO.getToken();
    }

    protected synchronized String authenticateHeadOfLibraryAndGetToken() throws Exception {
        String authResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content(
                        "{\"email\": \"head_of_library@library.com\", \"password\": \"head_of_library\"}")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(authResponse, AuthenticationResponseDTO.class);

        return authenticationResponseDTO.getToken();
    }

    protected synchronized String authenticateResearcherAndGetToken() throws Exception {
        String authResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("http://localhost:8081/api/user/authenticate")
                    .content("{\"email\": \"author2@author.com\", \"password\": \"author2\"}")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
            .getResponse().getContentAsString();

        var objectMapper = new ObjectMapper();
        var authenticationResponseDTO =
            objectMapper.readValue(authResponse, AuthenticationResponseDTO.class);

        return authenticationResponseDTO.getToken();
    }
}
