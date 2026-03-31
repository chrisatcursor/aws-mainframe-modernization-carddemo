package com.carddemo.transaction;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carddemo.card.CardXrefRepository;
import com.carddemo.common.DateTimeHelper;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardXrefRepository cardXrefRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CardXrefRepository cardXrefRepository) {
        this.transactionRepository = transactionRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    public Page<TransactionDto> listTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(TransactionDto::from);
    }

    public Optional<TransactionDto> findById(String id) {
        return transactionRepository.findById(id).map(TransactionDto::from);
    }

    public Optional<Transaction> findTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public Page<TransactionDto> findByTransactionIdGreaterThanEqual(String startId, Pageable pageable) {
        return transactionRepository.findByTransactionIdGreaterThanEqual(startId, pageable).map(TransactionDto::from);
    }

    @Transactional
    public Transaction createTransaction(TransactionCreateRequest request) {
        String cardNumber = request.getCardNumber() == null ? "" : request.getCardNumber().strip();
        if (!cardXrefRepository.existsById(cardNumber)) {
            throw new IllegalArgumentException("Card number not found in cross-reference.");
        }
        long seq = transactionRepository.getNextTransactionId();
        String transactionId = String.format("%016d", seq);

        Transaction t = TransactionFactory.newTransaction();
        t.setTransactionId(transactionId);
        t.setCardNumber(cardNumber);
        t.setTypeCode(request.getTypeCode() == null ? null : blankToNull(request.getTypeCode().strip()));
        t.setCategoryCode(request.getCategoryCode());
        t.setDescription(blankToNull(request.getDescription()));
        t.setAmount(request.getAmount());
        t.setMerchantName(blankToNull(request.getMerchantName()));
        t.setMerchantCity(blankToNull(request.getMerchantCity()));
        t.setMerchantZip(blankToNull(request.getMerchantZip()));
        t.setOriginTimestamp(request.getOriginTimestamp() == null ? null : request.getOriginTimestamp().strip());
        t.setProcessedTimestamp(DateTimeHelper.toTimestamp(LocalDateTime.now()));

        return transactionRepository.save(t);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.strip();
    }
}
