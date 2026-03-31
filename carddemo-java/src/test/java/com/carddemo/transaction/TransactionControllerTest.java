package com.carddemo.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.carddemo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void list_rendersListViewWithPageResponse() throws Exception {
        var dto = sampleDto("0000000000000001");
        var pageable = PageRequest.of(0, 10);
        when(transactionService.listTransactions(any()))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        mockMvc.perform(get("/transaction/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/list"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("startId", ""))
                .andExpect(model().attribute("pageSize", 10));
    }

    @Test
    @WithMockUser
    void list_withStartId_usesFilteredQuery() throws Exception {
        var dto = sampleDto("0000000000000002");
        var pageable = PageRequest.of(0, 10);
        when(transactionService.findByTransactionIdGreaterThanEqual(eq("0000000000000001"), any()))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        mockMvc.perform(get("/transaction/list")
                        .param("startId", "0000000000000001"))

                .andExpect(status().isOk())
                .andExpect(view().name("transaction/list"))
                .andExpect(model().attribute("startId", "0000000000000001"));

        verify(transactionService).findByTransactionIdGreaterThanEqual(eq("0000000000000001"), any());
    }

    @Test
    @WithMockUser
    void detailByQuery_noId_showsPrompt() throws Exception {
        mockMvc.perform(get("/transaction/detail"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/detail"))
                .andExpect(model().attribute("promptForId", true));
    }

    @Test
    @WithMockUser
    void detailByQuery_withId_rendersDetail() throws Exception {
        Transaction tx = sampleEntity("TXN0001");
        when(transactionService.findTransactionById("TXN0001")).thenReturn(Optional.of(tx));

        mockMvc.perform(get("/transaction/detail").param("transactionId", "TXN0001"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/detail"))
                .andExpect(model().attribute("txn", tx));
    }

    @Test
    @WithMockUser
    void detail_rendersDetailView() throws Exception {
        Transaction tx = sampleEntity("0000000000000003");
        when(transactionService.findTransactionById("0000000000000003")).thenReturn(Optional.of(tx));

        mockMvc.perform(get("/transaction/0000000000000003"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction/detail"))
                .andExpect(model().attribute("txn", tx));
    }

    @Test
    @WithMockUser
    void detail_notFound_returns404() throws Exception {
        when(transactionService.findTransactionById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/transaction/missing"))
                .andExpect(status().isNotFound());
    }

    private static TransactionDto sampleDto(String id) {
        return new TransactionDto(
                id,
                "4111111111111111",
                "01",
                3,
                "Purchase",
                new BigDecimal("9.99"),
                "2026-03-30T12:00:00"
        );
    }

    private static Transaction sampleEntity(String id) {
        Transaction tx = new Transaction();
        tx.setTransactionId(id);
        tx.setTypeCode("01");
        tx.setCategoryCode(3);
        tx.setDescription("Purchase");
        tx.setAmount(new BigDecimal("9.99"));
        tx.setCardNumber("4111111111111111");
        tx.setOriginTimestamp("2026-03-30T12:00:00");
        return tx;
    }
}
