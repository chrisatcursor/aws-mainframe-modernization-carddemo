package com.carddemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/some-protected-path").accept(MediaType.TEXT_HTML))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoint_regularUser_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_adminUser_isNotForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void staticAssets_arePublic() throws Exception {
        mockMvc.perform(get("/css/carddemo.css"))
            .andExpect(status().isOk());
    }

    @Test
    void loginPage_isPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }
}
