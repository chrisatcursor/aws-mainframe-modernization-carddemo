package com.carddemo.authorization;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PendingAuthPurgeSupportTest {

    @Test
    void dayDiff_matchesCbpaup0cFormula() {
        LocalDate today = LocalDate.of(2026, 3, 31);
        int yyddd = PendingAuthPurgeSupport.currentYyddd(today);
        int authDate9c = 99999 - yyddd;
        assertThat(PendingAuthPurgeSupport.dayDiff(authDate9c, today)).isZero();
    }

    @Test
    void isExpired_whenOlderThanExpiryDays() {
        LocalDate today = LocalDate.of(2026, 3, 31);
        int yyddd = PendingAuthPurgeSupport.currentYyddd(today);
        int oldAuthDate9c = 99999 - (yyddd - 10);
        assertThat(PendingAuthPurgeSupport.isExpired(oldAuthDate9c, today, 5)).isTrue();
    }

    @Test
    void adjustSummary_decrementsApproved() {
        PendingAuthSummary s = new PendingAuthSummary();
        s.setApprovedAuthCnt(1);
        s.setApprovedAuthAmt(new BigDecimal("50.00"));
        s.setDeclinedAuthCnt(0);
        s.setDeclinedAuthAmt(BigDecimal.ZERO);
        PendingAuthDetail d = new PendingAuthDetail();
        d.setAuthRespCode("00");
        d.setApprovedAmt(new BigDecimal("50.00"));
        d.setTransactionAmt(new BigDecimal("50.00"));
        PendingAuthPurgeSupport.adjustSummaryForDeletedDetail(s, d);
        assertThat(s.getApprovedAuthCnt()).isZero();
        assertThat(s.getApprovedAuthAmt()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
