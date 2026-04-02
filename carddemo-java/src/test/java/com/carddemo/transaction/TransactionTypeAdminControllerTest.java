package com.carddemo.transaction;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionTypeAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCanCreateTransactionType() throws Exception {
        mockMvc.perform(post("/admin/transaction-types")
                        .with(csrf())
                        .with(user("admin").roles("ADMIN"))
                        .param("trType", "Z9")
                        .param("description", "Test type"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/transaction-types"));

        mockMvc.perform(get("/admin/transaction-types").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
