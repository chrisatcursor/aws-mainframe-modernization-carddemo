package com.carddemo.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionContextTest {

    @Test
    void generalInfo_adminCheck() {
        var info = new SessionContext.GeneralInfo(
            "CSSG", "COSGN00C", "CMEN", "COMEN01C",
            "ADMIN001", "A", SessionContext.GeneralInfo.PGM_ENTER
        );

        assertThat(info.isAdmin()).isTrue();
        assertThat(info.programContext()).isEqualTo(SessionContext.GeneralInfo.PGM_ENTER);
    }

    @Test
    void generalInfo_regularUser() {
        var info = new SessionContext.GeneralInfo(
            "CSSG", "COSGN00C", "CMEN", "COMEN01C",
            "USER0001", "U", SessionContext.GeneralInfo.PGM_REENTER
        );

        assertThat(info.isAdmin()).isFalse();
    }

    @Test
    void fullContext_construction() {
        var ctx = new SessionContext(
            new SessionContext.GeneralInfo("", "", "", "", "USR1", "U", 0),
            new SessionContext.CustomerInfo(123456789L, "John", "M", "Doe"),
            new SessionContext.AccountInfo(10000000001L, "Y"),
            new SessionContext.CardInfo(4111111111111111L),
            new SessionContext.ScreenInfo("COSGN0A", "COSGN00")
        );

        assertThat(ctx.customerInfo().firstName()).isEqualTo("John");
        assertThat(ctx.accountInfo().accountId()).isEqualTo(10000000001L);
        assertThat(ctx.cardInfo().cardNumber()).isEqualTo(4111111111111111L);
        assertThat(ctx.screenInfo().lastMap()).isEqualTo("COSGN0A");
    }
}
