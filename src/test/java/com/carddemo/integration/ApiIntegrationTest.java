package com.carddemo.integration;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardXrefRepository cardXrefRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void seed() {
        transactionRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer c = new Customer();
        c.setId(42L);
        c.setFirstName("Pat");
        c.setLastName("Lee");
        c.setSsn("987654321");
        c.setFicoCreditScore(700);
        c.setAddrLine1("1 Main");
        c.setAddrLine3("Boston");
        c.setAddrStateCd("MA");
        c.setAddrZip("02101");
        c.setAddrCountryCd("USA");
        c.setDobYyyyMmDd("1990-01-02");
        customerRepository.save(c);

        Account a = new Account();
        a.setId(30000000003L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(new BigDecimal("250.00"));
        a.setCreditLimit(new BigDecimal("3000.00"));
        a.setCashCreditLimit(new BigDecimal("500.00"));
        a.setOpenDate("2021-05-05");
        a.setExpirationDate("2099-06-30");
        a.setReissueDate("2024-01-01");
        a.setCurrentCycleCredit(new BigDecimal("100.00"));
        a.setCurrentCycleDebit(new BigDecimal("50.00"));
        a.setGroupId("DEFAULT");
        accountRepository.save(a);

        Card card = new Card();
        card.setCardNumber("5444444444444444");
        card.setAccountId(30000000003L);
        card.setCvvCode(321);
        card.setEmbossedName("PAT LEE");
        card.setExpirationDate("2029-11-15");
        card.setActiveStatus("Y");
        cardRepository.save(card);

        CardXref x = new CardXref();
        x.setCardNumber("5444444444444444");
        x.setCustomerId(42L);
        x.setAccountId(30000000003L);
        cardXrefRepository.save(x);
    }

    @Test
    void accountViewReturnsCustomerAndAccountFields() throws Exception {
        mockMvc.perform(get("/api/accounts/view").param("accountId", "30000000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(30000000003L))
                .andExpect(jsonPath("$.customerSsnFormatted").value("987-65-4321"))
                .andExpect(jsonPath("$.currentBalance").value(250.0));
    }

    @Test
    void accountViewValidationMessageWhenBlank() throws Exception {
        mockMvc.perform(get("/api/accounts/view").param("accountId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorMessage").value("Account number not provided"));
    }

    @Test
    void cardListAndDetailAndUpdate() throws Exception {
        mockMvc.perform(get("/api/cards/list").param("accountId", "30000000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].cardNumber").value("5444444444444444"));

        mockMvc.perform(get("/api/cards/detail")
                        .param("accountId", "30000000003")
                        .param("cardNumber", "5444444444444444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embossedName").value("PAT LEE"))
                .andExpect(jsonPath("$.expiryYear").value("2029"));

        String body = """
                {
                  "accountId": 30000000003,
                  "cardNumber": "5444444444444444",
                  "expectedVersion": 0,
                  "oldCvvCode": 321,
                  "oldEmbossedName": "PAT LEE",
                  "oldExpiryYear": "2029",
                  "oldExpiryMonth": "11",
                  "oldExpiryDay": "15",
                  "oldActiveStatus": "Y",
                  "newEmbossedName": "PAT LEE JR",
                  "newActiveStatus": "Y"
                }
                """;
        mockMvc.perform(put("/api/cards").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cards/detail")
                        .param("accountId", "30000000003")
                        .param("cardNumber", "5444444444444444"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embossedName").value("PAT LEE JR"));
    }

    @Test
    void accountUpdateWithOptimisticLock() throws Exception {
        Account a = accountRepository.findById(30000000003L).orElseThrow();
        Customer c = customerRepository.findById(42L).orElseThrow();
        String json = """
                {
                  "accountId": %d,
                  "expectedAccountVersion": %d,
                  "expectedCustomerVersion": %d,
                  "customerId": %d,
                  "creditLimit": 3100.00,
                  "firstName": "Patricia"
                }
                """.formatted(a.getId(), a.getVersion(), c.getVersion(), c.getId());
        mockMvc.perform(put("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/accounts/view").param("accountId", "30000000003"))
                .andExpect(jsonPath("$.firstName").value("Patricia"))
                .andExpect(jsonPath("$.creditLimit").value(3100.0));
    }

    @Test
    void batchEndpointsReturnOk() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/batch/transaction-posting"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("STARTED")));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/batch/interest-calculation").param("parmDate", "2026-04-01"))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/batch/statement-generation"))
                .andExpect(status().isOk());
    }
}
