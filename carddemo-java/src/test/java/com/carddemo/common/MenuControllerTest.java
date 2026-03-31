package com.carddemo.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void showMenu_rendersMenuTemplate() throws Exception {
        mockMvc.perform(get("/menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("menu"))
                .andExpect(model().attributeExists("menuOptions"))
                .andExpect(model().attribute("menuTitle", "Main Menu"));
    }

    @Test
    @WithMockUser
    void processMenuSelection_validOption_redirects() throws Exception {
        mockMvc.perform(post("/menu")
                        .param("option", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/view"));
    }

    @Test
    @WithMockUser
    void processMenuSelection_invalidOption_redirectsWithError() throws Exception {
        mockMvc.perform(post("/menu")
                        .param("option", "99")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/menu"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser
    void processMenuSelection_noOption_redirectsWithError() throws Exception {
        mockMvc.perform(post("/menu").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/menu"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
