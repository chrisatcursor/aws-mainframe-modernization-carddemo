package com.carddemo.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminRoutes_unauthenticated_redirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/user/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminRoutes_nonAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/admin/user/list")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminRoutes_admin_ok() throws Exception {
        mockMvc.perform(get("/admin/user/list")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularRoutes_authenticatedUser_ok() throws Exception {
        mockMvc.perform(get("/menu")).andExpect(status().isOk());
    }
}
