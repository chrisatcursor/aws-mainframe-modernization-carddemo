package com.carddemo.user;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.carddemo.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_rendersWithPageModel() throws Exception {
        User u = new User();
        u.setUserId("USER0001");
        u.setFirstName("Jane");
        u.setLastName("Doe");
        u.setUserType("U");
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(u), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/admin/user/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user/list"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void showAddForm_renders() throws Exception {
        mockMvc.perform(get("/admin/user/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user/add"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_postRedirectsToList() throws Exception {
        User created = new User();
        created.setUserId("NEWUSER1");
        when(userService.createUser(eq("NEWUSER1"), eq("A"), eq("B"), eq("secret"), eq("U")))
                .thenReturn(created);

        mockMvc.perform(post("/admin/user/add")
                        .param("userId", "NEWUSER1")
                        .param("firstName", "A")
                        .param("lastName", "B")
                        .param("password", "secret")
                        .param("userType", "U")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/user/list"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(userService).createUser("NEWUSER1", "A", "B", "secret", "U");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_duplicateRedirectsToAddWithError() throws Exception {
        when(userService.createUser(anyString(), any(), any(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("User already exists: NEWUSER1"));

        mockMvc.perform(post("/admin/user/add")
                        .param("userId", "NEWUSER1")
                        .param("password", "secret")
                        .param("userType", "U")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/user/add"))
                .andExpect(flash().attribute("errorMessage", "User already exists: NEWUSER1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void showUpdateForm_rendersEditView() throws Exception {
        User u = sampleUser();
        when(userService.findByUserId("USER0001")).thenReturn(u);

        mockMvc.perform(get("/admin/user/update").param("userId", "USER0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user/edit"))
                .andExpect(model().attribute("user", u));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void showDeleteConfirm_rendersDeleteView() throws Exception {
        User u = sampleUser();
        when(userService.findByUserId("USER0001")).thenReturn(u);

        mockMvc.perform(get("/admin/user/delete").param("userId", "USER0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user/delete"))
                .andExpect(model().attribute("user", u));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/admin/user/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void showAddForm_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/admin/user/add"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/user/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    private static User sampleUser() {
        User u = new User();
        u.setUserId("USER0001");
        u.setFirstName("Jane");
        u.setLastName("Doe");
        u.setUserType("U");
        return u;
    }
}
