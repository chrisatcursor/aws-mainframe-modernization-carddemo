package com.carddemo.auth.pending;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.config.CardDemoProperties;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PendingAuthPurgeTest {

    @Autowired
    private PendingAuthService pendingAuthService;
    @Autowired
    private PendingAuthDetailRepository detailRepository;
    @Autowired
    private PendingAuthSummaryRepository summaryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CardDemoProperties properties;

    @Test
    void purgeRemovesOldDetails() {
        Account a = new Account();
        a.setAccountId(88L);
        a.setActiveStatus("A");
        a.setCurrentBalance(BigDecimal.ZERO);
        a.setCreditLimit(BigDecimal.TEN);
        a.setCashCreditLimit(BigDecimal.ONE);
        accountRepository.save(a);

        PendingAuthSummary sum = new PendingAuthSummary();
        sum.setAcctId(88L);
        sum.setCustId(2L);
        sum.setApprovedAuthCount(1);
        sum.setDeclinedAuthCount(0);
        sum.setApprovedAuthAmt(BigDecimal.ONE);
        summaryRepository.save(sum);

        PendingAuthDetail old = new PendingAuthDetail();
        old.setAcctId(88L);
        old.setAuthTs(LocalDateTime.now().minusDays(properties.getPendingAuth().getPurgeExpiryDays() + 1));
        old.setCardNum("4222222222222222");
        old.setTransactionId("OLD1");
        old.setAuthRespCode("00");
        old.setApprovedAmt(BigDecimal.ONE);
        detailRepository.save(old);

        PendingAuthService.PurgeResult r = pendingAuthService.purgeExpired();
        assertThat(r.detailsDeleted()).isEqualTo(1);
        assertThat(detailRepository.findByAcctIdOrderByAuthTsDesc(88L)).isEmpty();
    }
}
