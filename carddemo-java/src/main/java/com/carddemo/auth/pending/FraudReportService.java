package com.carddemo.auth.pending;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * COPAUS2C — AUTHFRDS insert/update (report fraud F / remove fraud R).
 */
@Service
public class FraudReportService {

    private final AuthFraudReportRepository fraudRepository;

    public FraudReportService(AuthFraudReportRepository fraudRepository) {
        this.fraudRepository = fraudRepository;
    }

    @Transactional
    public void applyFraudAction(
            PendingAuthDetail detail,
            long acctId,
            long custId,
            String fraudAction) {
        if (!"F".equals(fraudAction) && !"R".equals(fraudAction)) {
            throw new IllegalArgumentException("fraudAction must be F or R");
        }
        var id = new AuthFraudReportId(detail.getCardNum(), detail.getAuthTs());
        AuthFraudReport row = fraudRepository.findById(id).map(existing -> {
            existing.setAuthFraud(fraudAction);
            existing.setFraudRptDate(LocalDate.now());
            existing.setAcctId(acctId);
            existing.setCustId(custId);
            return existing;
        }).orElseGet(() -> mapRow(detail, acctId, custId, fraudAction));
        fraudRepository.save(row);
        detail.setFraudFlag(fraudAction);
        detail.setFraudReportDate(LocalDate.now().toString().replace("-", ""));
    }

    private static AuthFraudReport mapRow(PendingAuthDetail d, long acctId, long custId, String fraudAction) {
        AuthFraudReport r = new AuthFraudReport();
        r.setCardNum(d.getCardNum());
        r.setAuthTs(d.getAuthTs());
        r.setAuthType(d.getAuthType());
        r.setCardExpiryDate(d.getCardExpiryDate());
        r.setMessageType(d.getMessageType());
        r.setMessageSource(d.getMessageSource());
        r.setAuthIdCode(d.getAuthIdCode());
        r.setAuthRespCode(d.getAuthRespCode());
        r.setAuthRespReason(d.getAuthRespReason());
        r.setProcessingCode(d.getProcessingCode());
        r.setTransactionAmt(d.getTransactionAmt());
        r.setApprovedAmt(d.getApprovedAmt());
        r.setMerchantCatagoryCd(d.getMerchantCategoryCd());
        r.setAcqrCountryCode(d.getAcqrCountryCode());
        r.setPosEntryMode(d.getPosEntryMode() == null ? null : d.getPosEntryMode().shortValue());
        r.setMerchantId(d.getMerchantId());
        r.setMerchantName(d.getMerchantName());
        r.setMerchantCity(d.getMerchantCity());
        r.setMerchantState(d.getMerchantState());
        r.setMerchantZip(d.getMerchantZip());
        r.setTransactionId(d.getTransactionId());
        r.setMatchStatus(d.getMatchStatus());
        r.setAuthFraud(fraudAction);
        r.setFraudRptDate(LocalDate.now());
        r.setAcctId(acctId);
        r.setCustId(custId);
        return r;
    }
}
