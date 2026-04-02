package com.carddemo.auth.pending;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;

@DataJpaTest
@Import(FraudReportService.class)
@ActiveProfiles("test")
class FraudReportServiceTest {

    @Autowired
    private FraudReportService fraudReportService;
    @Autowired
    private AuthFraudReportRepository fraudRepository;
    @Autowired
    private PendingAuthDetailRepository detailRepository;
    @Autowired
    private PendingAuthSummaryRepository summaryRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void insertAndUpdateFraudReport() {
        Account a = new Account();
        a.setAccountId(99L);
        a.setActiveStatus("A");
        a.setCurrentBalance(BigDecimal.ZERO);
        a.setCreditLimit(BigDecimal.TEN);
        a.setCashCreditLimit(BigDecimal.ONE);
        accountRepository.save(a);

        PendingAuthSummary sum = new PendingAuthSummary();
        sum.setAcctId(99L);
        sum.setCustId(1L);
        summaryRepository.save(sum);

        PendingAuthDetail d = new PendingAuthDetail();
        d.setAcctId(99L);
        d.setAuthTs(LocalDateTime.of(2024, 6, 1, 12, 0));
        d.setCardNum("4111111111111111");
        d.setTransactionId("TXN001");
        d.setAuthRespCode("00");
        d.setMerchantCategoryCd("5411");
        detailRepository.save(d);

        fraudReportService.applyFraudAction(d, 99L, 1L, "F");
        detailRepository.save(d);

        assertThat(fraudRepository.findById(new AuthFraudReportId("4111111111111111", d.getAuthTs())))
                .isPresent();
        assertThat(d.getFraudFlag()).isEqualTo("F");

        fraudReportService.applyFraudAction(d, 99L, 1L, "R");
        detailRepository.save(d);
        AuthFraudReport row = fraudRepository
                .findById(new AuthFraudReportId("4111111111111111", d.getAuthTs()))
                .orElseThrow();
        assertThat(row.getAuthFraud()).isEqualTo("R");
    }
}
