package com.carddemo.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.carddemo.card.CardXrefRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void listTransactions_mapsPageToDto() {
        Transaction entity = sampleEntity("0000000000000001");
        Pageable pageable = PageRequest.of(0, 10);
        when(transactionRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<TransactionDto> page = transactionService.listTransactions(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().transactionId()).isEqualTo("0000000000000001");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_returnsDtoWhenPresent() {
        Transaction entity = sampleEntity("0000000000000002");
        when(transactionRepository.findById("0000000000000002")).thenReturn(Optional.of(entity));

        Optional<TransactionDto> found = transactionService.findById("0000000000000002");

        assertThat(found).isPresent();
        assertThat(found.get().transactionId()).isEqualTo("0000000000000002");
    }

    @Test
    void findById_emptyWhenMissing() {
        when(transactionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(transactionService.findById("missing")).isEmpty();
    }

    @Test
    void findTransactionById_returnsEntityWhenPresent() {
        Transaction entity = sampleEntity("0000000000000004");
        when(transactionRepository.findById("0000000000000004")).thenReturn(Optional.of(entity));

        Optional<Transaction> found = transactionService.findTransactionById("0000000000000004");

        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo("0000000000000004");
    }

    @Test
    void findTransactionById_emptyWhenMissing() {
        when(transactionRepository.findById("none")).thenReturn(Optional.empty());

        assertThat(transactionService.findTransactionById("none")).isEmpty();
    }

    @Test
    void findByTransactionIdGreaterThanEqual_delegatesToRepository() {
        Transaction entity = sampleEntity("0000000000000003");
        Pageable pageable = PageRequest.of(0, 5);
        when(transactionRepository.findByTransactionIdGreaterThanEqual(eq("0000000000000001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

        Page<TransactionDto> page = transactionService.findByTransactionIdGreaterThanEqual("0000000000000001", pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(transactionRepository).findByTransactionIdGreaterThanEqual("0000000000000001", pageable);
    }

    @Test
    void createTransaction_rejectsUnknownCard() {
        when(cardXrefRepository.existsById("4111111111111111")).thenReturn(false);
        TransactionCreateRequest req = validRequest();

        assertThatThrownBy(() -> transactionService.createTransaction(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number not found");
    }

    @Test
    void createTransaction_savesWithGeneratedIdAndProcessedTimestamp() {
        when(cardXrefRepository.existsById("4111111111111111")).thenReturn(true);
        when(transactionRepository.getNextTransactionId()).thenReturn(100_042L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionCreateRequest req = validRequest();
        req.setDescription("  Dinner  ");
        req.setMerchantName("Cafe");

        Transaction saved = transactionService.createTransaction(req);

        verify(transactionRepository).save(saved);
        assertThat(saved.getTransactionId()).isEqualTo("0000000000100042");
        assertThat(saved.getCardNumber()).isEqualTo("4111111111111111");
        assertThat(saved.getTypeCode()).isEqualTo("01");
        assertThat(saved.getCategoryCode()).isEqualTo(3);
        assertThat(saved.getDescription()).isEqualTo("Dinner");
        assertThat(saved.getAmount()).isEqualByComparingTo("19.99");
        assertThat(saved.getMerchantName()).isEqualTo("Cafe");
        assertThat(saved.getOriginTimestamp()).isEqualTo("2026-03-30 12:00:00.000000");
        assertThat(saved.getProcessedTimestamp()).isNotNull();
        assertThat(saved.getProcessedTimestamp()).matches(
                "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{6}");
    }

    private static TransactionCreateRequest validRequest() {
        TransactionCreateRequest r = new TransactionCreateRequest();
        r.setCardNumber("4111111111111111");
        r.setTypeCode("01");
        r.setCategoryCode(3);
        r.setAmount(new BigDecimal("19.99"));
        r.setOriginTimestamp("2026-03-30 12:00:00.000000");
        return r;
    }

    private static Transaction sampleEntity(String id) {
        Transaction t = new Transaction();
        t.setTransactionId(id);
        t.setCardNumber("4111111111111111");
        t.setTypeCode("01");
        t.setCategoryCode(3);
        t.setDescription("Purchase");
        t.setAmount(new BigDecimal("12.34"));
        t.setOriginTimestamp("2026-03-30T10:00:00");
        return t;
    }
}
