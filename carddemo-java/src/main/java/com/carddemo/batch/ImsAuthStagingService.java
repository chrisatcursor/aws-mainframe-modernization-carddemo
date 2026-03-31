package com.carddemo.batch;

import com.carddemo.authorization.PendingAuthDetail;
import com.carddemo.authorization.PendingAuthDetailRepository;
import com.carddemo.authorization.PendingAuthSummary;
import com.carddemo.authorization.PendingAuthSummaryRepository;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PAUDBLOD / PAUDBUNL / DBUNLDGS — pipe-delimited staging (readable substitute for IMS GSAM binary).
 */
@Service
public class ImsAuthStagingService {

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;

    public ImsAuthStagingService(
            PendingAuthSummaryRepository summaryRepository,
            PendingAuthDetailRepository detailRepository) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
    }

    @Transactional
    public void loadFromStagingFiles(Resource summaryFile, Resource detailFile) throws Exception {
        if (summaryFile.exists()) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(summaryFile.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] p = line.split("\\|");
                    if (p.length < 6) {
                        continue;
                    }
                    long acctId = Long.parseLong(p[0].trim());
                    PendingAuthSummary s = summaryRepository.findById(acctId).orElseGet(PendingAuthSummary::new);
                    s.setAcctId(acctId);
                    s.setCustId(Long.parseLong(p[1].trim()));
                    s.setApprovedAuthCnt(Integer.parseInt(p[2].trim()));
                    s.setDeclinedAuthCnt(Integer.parseInt(p[3].trim()));
                    s.setApprovedAuthAmt(new BigDecimal(p[4].trim()));
                    s.setDeclinedAuthAmt(new BigDecimal(p[5].trim()));
                    if (p.length > 6) {
                        s.setCreditLimit(new BigDecimal(p[6].trim()));
                    }
                    if (p.length > 7) {
                        s.setCashLimit(new BigDecimal(p[7].trim()));
                    }
                    s.setAuthStatus("A");
                    summaryRepository.save(s);
                }
            }
        }
        if (detailFile.exists()) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(detailFile.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] p = line.split("\\|");
                    if (p.length < 8) {
                        continue;
                    }
                    PendingAuthDetail d = new PendingAuthDetail();
                    d.setAcctId(Long.parseLong(p[0].trim()));
                    d.setAuthDate9c(Integer.parseInt(p[1].trim()));
                    d.setAuthTime9c(Integer.parseInt(p[2].trim()));
                    d.setAuthOrigDate(p[3].trim());
                    d.setAuthOrigTime(p[4].trim());
                    d.setCardNum(p[5].trim());
                    d.setAuthRespCode(p[6].trim());
                    d.setTransactionAmt(new BigDecimal(p[7].trim()));
                    d.setApprovedAmt(p.length > 8 ? new BigDecimal(p[8].trim()) : BigDecimal.ZERO);
                    d.setAuthType(p.length > 9 ? p[9].trim() : "AUTH");
                    d.setMessageType(p.length > 10 ? p[10].trim() : "LOAD");
                    d.setMessageSource(p.length > 11 ? p[11].trim() : "BATCH");
                    d.setMatchStatus(p.length > 12 ? p[12].trim() : "P");
                    d.setAuthFraud(" ");
                    d.setFraudRptDate("        ");
                    detailRepository.save(d);
                }
            }
        }
    }

    public void unloadToFiles(WritableResource summaryOut, WritableResource detailOut) throws Exception {
        List<PendingAuthSummary> summaries = summaryRepository.findAll();
        try (BufferedWriter sw = new BufferedWriter(
                new OutputStreamWriter(summaryOut.getOutputStream(), StandardCharsets.UTF_8))) {
            sw.write("# acctId|custId|apprCnt|declCnt|apprAmt|declAmt|creditLimit|cashLimit\n");
            for (PendingAuthSummary s : summaries) {
                sw.write(String.format("%d|%d|%d|%d|%s|%s|%s|%s%n",
                        s.getAcctId(),
                        s.getCustId(),
                        s.getApprovedAuthCnt(),
                        s.getDeclinedAuthCnt(),
                        s.getApprovedAuthAmt().toPlainString(),
                        s.getDeclinedAuthAmt().toPlainString(),
                        s.getCreditLimit() != null ? s.getCreditLimit().toPlainString() : "0",
                        s.getCashLimit() != null ? s.getCashLimit().toPlainString() : "0"));
            }
        }
        try (BufferedWriter dw = new BufferedWriter(
                new OutputStreamWriter(detailOut.getOutputStream(), StandardCharsets.UTF_8))) {
            dw.write("# acctId|authDate9c|authTime9c|authOrigDate|authOrigTime|cardNum|authResp|transAmt|apprAmt|authType|msgType|msgSrc|match\n");
            for (PendingAuthSummary s : summaries) {
                for (PendingAuthDetail d : detailRepository.findByAcctIdOrderByAuthDate9cDescAuthTime9cDesc(s.getAcctId())) {
                    dw.write(String.format("%d|%d|%d|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s%n",
                            d.getAcctId(),
                            d.getAuthDate9c(),
                            d.getAuthTime9c(),
                            nullToEmpty(d.getAuthOrigDate()),
                            nullToEmpty(d.getAuthOrigTime()),
                            d.getCardNum(),
                            nullToEmpty(d.getAuthRespCode()),
                            d.getTransactionAmt() != null ? d.getTransactionAmt().toPlainString() : "0",
                            d.getApprovedAmt() != null ? d.getApprovedAmt().toPlainString() : "0",
                            nullToEmpty(d.getAuthType()),
                            nullToEmpty(d.getMessageType()),
                            nullToEmpty(d.getMessageSource()),
                            nullToEmpty(d.getMatchStatus())));
                }
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
