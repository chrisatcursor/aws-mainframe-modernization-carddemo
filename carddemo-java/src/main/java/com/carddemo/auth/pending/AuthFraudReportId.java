package com.carddemo.auth.pending;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class AuthFraudReportId implements Serializable {

    private String cardNum;
    private LocalDateTime authTs;

    public AuthFraudReportId() {
    }

    public AuthFraudReportId(String cardNum, LocalDateTime authTs) {
        this.cardNum = cardNum;
        this.authTs = authTs;
    }

    public String getCardNum() {
        return cardNum;
    }

    public void setCardNum(String cardNum) {
        this.cardNum = cardNum;
    }

    public LocalDateTime getAuthTs() {
        return authTs;
    }

    public void setAuthTs(LocalDateTime authTs) {
        this.authTs = authTs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthFraudReportId that = (AuthFraudReportId) o;
        return Objects.equals(cardNum, that.cardNum) && Objects.equals(authTs, that.authTs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNum, authTs);
    }
}
