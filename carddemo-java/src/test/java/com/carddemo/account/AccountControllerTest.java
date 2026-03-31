package com.carddemo.account;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser
    void viewAccount_noId_showsPrompt() throws Exception {
        mockMvc.perform(get("/account/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/view"))
                .andExpect(model().attribute("promptForId", true));
    }

    @Test
    @WithMockUser
    void viewAccount_withId_showsDetail() throws Exception {
        Account account = new Account();
        account.setAcctId(1001L);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("5000.00"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setOpenDate("2020-01-15");
        account.setExpirationDate("2025-01-15");
        account.setReissueDate("2024-01-01");
        account.setCurrentCycleCredit(new BigDecimal("500.00"));
        account.setCurrentCycleDebit(new BigDecimal("200.00"));
        account.setAddressZip("12345");
        account.setGroupId("G1");
        account.setVersion(1L);

        Customer customer = CustomerTestFactory.newCustomer();
        customer.setCustId(2001L);
        customer.setFirstName("Pat");
        customer.setLastName("Lee");
        customer.setVersion(2L);

        AccountDetailDto detail = AccountDetailDto.from(account, customer);

        when(accountService.findAccountWithCustomer(1001L)).thenReturn(detail);
        when(accountService.findCardsForAccount(1001L)).thenReturn(List.of());

        mockMvc.perform(get("/account/view").param("acctId", "1001"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/view"))
                .andExpect(model().attributeExists("detail"));
    }

    @Test
    @WithMockUser
    void updateForm_noId_showsPrompt() throws Exception {
        mockMvc.perform(get("/account/update"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/edit"))
                .andExpect(model().attribute("promptForId", true));
    }
}
