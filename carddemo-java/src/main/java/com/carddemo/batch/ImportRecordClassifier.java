package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.card.Card;
import com.carddemo.card.CardXref;
import com.carddemo.transaction.Transaction;
import org.springframework.batch.item.ItemWriter;
import org.springframework.classify.Classifier;

/**
 * Routes CBIMPORT-mapped items to the repository writer for each entity type (see
 * {@link DataImportJobConfig#importCompositeWriter}).
 */
public final class ImportRecordClassifier implements Classifier<Object, ItemWriter<? super Object>> {

    private final ItemWriter<? super Object> customerWriter;
    private final ItemWriter<? super Object> accountWriter;
    private final ItemWriter<? super Object> xrefWriter;
    private final ItemWriter<? super Object> transactionWriter;
    private final ItemWriter<? super Object> cardWriter;
    private final ItemWriter<? super Object> invalidWriter;

    public ImportRecordClassifier(
            ItemWriter<? super Object> customerWriter,
            ItemWriter<? super Object> accountWriter,
            ItemWriter<? super Object> xrefWriter,
            ItemWriter<? super Object> transactionWriter,
            ItemWriter<? super Object> cardWriter,
            ItemWriter<? super Object> invalidWriter) {
        this.customerWriter = customerWriter;
        this.accountWriter = accountWriter;
        this.xrefWriter = xrefWriter;
        this.transactionWriter = transactionWriter;
        this.cardWriter = cardWriter;
        this.invalidWriter = invalidWriter;
    }

    @Override
    public ItemWriter<? super Object> classify(Object item) {
        return switch (item) {
            case Customer c -> customerWriter;
            case Account a -> accountWriter;
            case CardXref x -> xrefWriter;
            case Transaction t -> transactionWriter;
            case Card d -> cardWriter;
            case ImportRecordMapper.InvalidImportRecord r -> invalidWriter;
            default -> invalidWriter;
        };
    }
}
