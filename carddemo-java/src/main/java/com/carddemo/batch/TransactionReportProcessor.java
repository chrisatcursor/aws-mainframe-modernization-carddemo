package com.carddemo.batch;

import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategory;
import com.carddemo.transaction.TransactionCategory.TransactionCategoryKey;
import com.carddemo.transaction.TransactionCategoryRepository;
import com.carddemo.transaction.TransactionType;
import com.carddemo.transaction.TransactionTypeRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

/**
 * Enriches {@link Transaction} rows with xref account id and type/category descriptions (CBTRN03C).
 */
public class TransactionReportProcessor implements ItemProcessor<Transaction, TransactionReportLine> {

    private static final Logger log = LoggerFactory.getLogger(TransactionReportProcessor.class);

    static final char CODE_DESC_SEPARATOR = '\u001e';

    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final CardXrefRepository cardXrefRepository;

    public TransactionReportProcessor(
            TransactionTypeRepository transactionTypeRepository,
            TransactionCategoryRepository transactionCategoryRepository,
            CardXrefRepository cardXrefRepository) {
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    @Override
    public TransactionReportLine process(Transaction item) {
        if (item.getCardNumber() == null || item.getCardNumber().isBlank()) {
            log.warn("Skipping transaction {}: missing card number", item.getTransactionId());
            return null;
        }

        Optional<CardXref> xrefOpt = cardXrefRepository.findById(item.getCardNumber().trim());
        if (xrefOpt.isEmpty()) {
            log.warn("Skipping transaction {}: no card xref for {}", item.getTransactionId(), item.getCardNumber());
            return null;
        }

        Long acctId = xrefOpt.get().getAccountId();
        String accountIdStr = acctId == null ? "" : String.valueOf(acctId);

        String typeCode = item.getTypeCode() != null ? item.getTypeCode().trim() : "  ";
        if (typeCode.length() > 2) {
            typeCode = typeCode.substring(0, 2);
        }

        String typeDesc = transactionTypeRepository
                .findById(typeCode)
                .map(TransactionType::getTypeDescription)
                .orElse("Unknown type");

        Integer catCd = item.getCategoryCode() != null ? item.getCategoryCode() : 0;
        String catDesc = transactionCategoryRepository
                .findById(new TransactionCategoryKey(typeCode, catCd))
                .map(TransactionCategory::getDescription)
                .orElse("Unknown category");

        String procTs = item.getProcessedTimestamp();
        String datePrefix = procTs != null && procTs.length() >= 10 ? procTs.substring(0, 10) : "";

        String source = item.getSource() != null ? item.getSource() : "";

        String typeField = typeCode + CODE_DESC_SEPARATOR + typeDesc;
        String catField = String.format("%04d", catCd) + CODE_DESC_SEPARATOR + catDesc;

        return new TransactionReportLine(
                item.getTransactionId(),
                item.getCardNumber(),
                accountIdStr,
                typeField,
                catField,
                item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO,
                datePrefix,
                source);
    }
}
