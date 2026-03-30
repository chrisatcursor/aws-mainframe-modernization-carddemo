package com.carddemo.integration.account;

import com.carddemo.account.api.AccountUpdateRequest;
import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "carddemo.bootstrap.enabled=true"
})
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    private Long accountId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        CardXref xref = cardXrefRepository.findAll().stream().findFirst().orElseThrow();
        accountId = xref.getAccountId();
        customerId = xref.getCustomerId();
    }

    @Test
    void getAccount_returnsCombinedAccountAndCustomerView() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(accountId.intValue())))
                .andExpect(jsonPath("$.customerId", is(customerId.intValue())))
                .andExpect(jsonPath("$.activeStatus", is(account.getActiveStatus())))
                .andExpect(jsonPath("$.firstName", is(customer.getFirstName())));
    }

    @Test
    void updateAccount_persistsAccountAndCustomerChanges() throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        String validSsn = "123456789";

        AccountUpdateRequest request = new AccountUpdateRequest(
                account.getActiveStatus(),
                account.getCreditLimit().add(java.math.BigDecimal.valueOf(100)),
                account.getCashCreditLimit().add(java.math.BigDecimal.valueOf(100)),
                account.getExpirationDate().plusYears(1),
                account.getReissueDate().plusMonths(1),
                fixed(customer.getFirstName()),
                fixed(customer.getMiddleName()),
                fixed(customer.getLastName()),
                fixed50(customer.getAddressLine1()),
                fixed50(customer.getAddressLine2()),
                fixed50(customer.getAddressLine3()),
                customer.getStateCode(),
                customer.getCountryCode(),
                fixed10(customer.getZip()),
                fixed15(customer.getPhone1()),
                fixed15(customer.getPhone2()),
                validSsn,
                customer.getDateOfBirth() == null ? LocalDate.of(1990, 1, 1) : customer.getDateOfBirth(),
                customer.getFicoScore() == null ? 700 : customer.getFicoScore(),
                account.getVersion()
        );

        mockMvc.perform(put("/api/accounts/{accountId}", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "activeStatus":"%s",
                                  "creditLimit":%s,
                                  "cashCreditLimit":%s,
                                  "expirationDate":"%s",
                                  "reissueDate":"%s",
                                  "firstName":"%s",
                                  "middleName":"%s",
                                  "lastName":"%s",
                                  "addressLine1":"%s",
                                  "addressLine2":"%s",
                                  "addressLine3":"%s",
                                  "stateCode":"%s",
                                  "countryCode":"%s",
                                  "zip":"%s",
                                  "phone1":"%s",
                                  "phone2":"%s",
                                  "ssn":"%s",
                                  "dateOfBirth":"%s",
                                  "ficoScore":%d,
                                  "version":%d
                                }
                                """.formatted(
                                        request.activeStatus(),
                                        request.creditLimit(),
                                        request.cashCreditLimit(),
                                        request.expirationDate(),
                                        request.reissueDate(),
                                        request.firstName(),
                                        request.middleName(),
                                        request.lastName(),
                                        request.addressLine1(),
                                        request.addressLine2(),
                                        request.addressLine3(),
                                        request.stateCode(),
                                        request.countryCode(),
                                        request.zip(),
                                        request.phone1(),
                                        request.phone2(),
                                        request.ssn(),
                                        request.dateOfBirth(),
                                        request.ficoScore(),
                                        request.version()
                                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", is(accountId.intValue())))
                .andExpect(jsonPath("$.customerId", is(customerId.intValue())))
                .andExpect(jsonPath("$.message", is("Account updated")));

        Customer updated = customerRepository.findById(customerId).orElseThrow();
        assertThat(updated.getSsn()).isEqualTo(validSsn);
    }

    private static String fixed(String value) {
        return padRight(value, 25);
    }

    private static String fixed50(String value) {
        return padRight(value, 50);
    }

    private static String fixed15(String value) {
        return padRight(value, 15);
    }

    private static String fixed10(String value) {
        return padRight(value, 10);
    }

    private static String fixed9(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        return (digits + "000000000").substring(0, 9);
    }

    private static String padRight(String value, int len) {
        String src = value == null ? "" : value.trim();
        if (src.length() >= len) {
            return src.substring(0, len);
        }
        return src + " ".repeat(len - src.length());
    }
}
