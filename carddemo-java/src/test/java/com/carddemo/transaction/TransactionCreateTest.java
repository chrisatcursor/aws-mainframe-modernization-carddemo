package com.carddemo.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.carddemo.config.SecurityConfig;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void showAddForm_rendersWithEmptyRequest() throws Exception {
        mockMvc.perform(get("/transaction/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/add"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    @WithMockUser
    void addSubmit_valid_redirectsWithFlash() throws Exception {
        Transaction created = new Transaction();
        created.setTransactionId("0000000000100042");
        when(transactionService.createTransaction(any(TransactionCreateRequest.class))).thenReturn(created);

        mockMvc.perform(post("/transaction/add")
                        .param("cardNumber", "4111111111111111")
                        .param("typeCode", "01")
                        .param("categoryCode", "3")
                        .param("amount", "10.00")
                        .param("originTimestamp", "2026-03-30 12:00:00.000000")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transaction/list"))
                .andExpect(flash().attribute(
                        "successMessage", "Transaction created: 0000000000100042"));

        verify(transactionService).createTransaction(any(TransactionCreateRequest.class));
    }

    @Test
    @WithMockUser
    void addSubmit_validationErrors_returnsForm() throws Exception {
        mockMvc.perform(post("/transaction/add")
                        .param("cardNumber", "")
                        .param("typeCode", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/add"))
                .andExpect(model().attributeHasErrors("request"));
    }

    @Test
    @WithMockUser
    void addSubmit_unknownCard_returnsFormWithGlobalError() throws Exception {
        when(transactionService.createTransaction(any(TransactionCreateRequest.class)))
                .thenThrow(new IllegalArgumentException("Card number not found in cross-reference."));

        mockMvc.perform(post("/transaction/add")
                        .param("cardNumber", "4111111111111111")
                        .param("typeCode", "01")
                        .param("categoryCode", "3")
                        .param("amount", "5.00")
                        .param("originTimestamp", "2026-03-30 12:00:00.000000")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/add"))
                .andExpect(model().attributeHasErrors("request"));
    }

    @Test
    @WithMockUser
    void addSubmit_bindsOptionalMerchantFields() throws Exception {
        Transaction created = new Transaction();
        created.setTransactionId("0000000000100099");
        when(transactionService.createTransaction(any(TransactionCreateRequest.class))).thenAnswer(inv -> {
            TransactionCreateRequest r = inv.getArgument(0);
            assertThat(r.getMerchantName()).isEqualTo("Shop");
            assertThat(r.getMerchantCity()).isEqualTo("Austin");
            assertThat(r.getMerchantZip()).isEqualTo("78701");
            assertThat(r.getAmount()).isEqualByComparingTo(new BigDecimal("1.25"));
            return created;
        });

        mockMvc.perform(post("/transaction/add")
                        .param("cardNumber", "4111111111111111")
                        .param("typeCode", "01")
                        .param("categoryCode", "2")
                        .param("description", "Coffee")
                        .param("amount", "1.25")
                        .param("merchantName", "Shop")
                        .param("merchantCity", "Austin")
                        .param("merchantZip", "78701")
                        .param("originTimestamp", "2026-03-30 08:15:00.000000")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transaction/list"));
    }
}
