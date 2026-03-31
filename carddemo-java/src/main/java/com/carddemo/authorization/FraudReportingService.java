package com.carddemo.authorization;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * COPAUS2C — fraud report insert/update against AUTHFRDS (fraud_reports).
 */
@Service
public class FraudReportingService {

    private final FraudReportRepository fraudReportRepository;

    public FraudReportingService(FraudReportRepository fraudReportRepository) {
        this.fraudReportRepository = fraudReportRepository;
    }

    public record FraudUpdateResult(boolean success, String message) {}

    @Transactional
    public FraudUpdateResult applyFraudAction(
            long acctId,
            long custId,
            PendingAuthDetail detail,
            char action) {
        Instant authTs = AuthTimestampUtil.toInstant(detail.getAuthOrigDate(), detail.getAuthTime9c());
        String fraudFlag = action == 'F' || action == 'f' ? "F" : "R";

        FraudReport row = new FraudReport();
        row.setCardNum(padCard(detail.getCardNum()));
        row.setAuthTs(authTs);
        row.setAuthType(trim(detail.getAuthType(), 4));
        row.setCardExpiryDate(trim(detail.getCardExpiryDate(), 4));
        row.setMessageType(trim(detail.getMessageType(), 6));
        row.setMessageSource(trim(detail.getMessageSource(), 6));
        row.setAuthIdCode(trim(detail.getAuthIdCode(), 6));
        row.setAuthRespCode(trim(detail.getAuthRespCode(), 2));
        row.setAuthRespReason(trim(detail.getAuthRespReason(), 4));
        row.setProcessingCode(trim(detail.getProcessingCode(), 6));
        row.setTransactionAmt(detail.getTransactionAmt());
        row.setApprovedAmt(detail.getApprovedAmt());
        row.setMerchantCategoryCode(trim(detail.getMerchantCategoryCode(), 4));
        row.setAcqrCountryCode(trim(detail.getAcqrCountryCode(), 3));
        row.setPosEntryMode(detail.getPosEntryMode());
        row.setMerchantId(trim(detail.getMerchantId(), 15));
        row.setMerchantName(trim(detail.getMerchantName(), 22));
        row.setMerchantCity(trim(detail.getMerchantCity(), 13));
        row.setMerchantState(trim(detail.getMerchantState(), 2));
        row.setMerchantZip(trim(detail.getMerchantZip(), 9));
        row.setTransactionId(trim(detail.getTransactionId(), 15));
        row.setMatchStatus(trim(detail.getMatchStatus(), 1));
        row.setAuthFraud(fraudFlag);
        row.setFraudRptDate(LocalDate.now());
        row.setAcctId(BigDecimal.valueOf(acctId));
        row.setCustId(BigDecimal.valueOf(custId));

        if (fraudReportRepository.existsByCardNumAndAuthTs(row.getCardNum(), authTs)) {
            FraudReport existing = fraudReportRepository.findById(new FraudReport.FraudReportId(row.getCardNum(), authTs))
                    .orElseThrow();
            existing.setAuthFraud(fraudFlag);
            existing.setFraudRptDate(LocalDate.now());
            fraudReportRepository.save(existing);
            return new FraudUpdateResult(true, "UPDT SUCCESS");
        }
        fraudReportRepository.save(row);
        return new FraudUpdateResult(true, "ADD SUCCESS");
    }

    private static String padCard(String cardNum) {
        if (cardNum == null) {
            return "                ";
        }
        String c = cardNum.trim();
        return String.format("%-16s", c).substring(0, 16);
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
