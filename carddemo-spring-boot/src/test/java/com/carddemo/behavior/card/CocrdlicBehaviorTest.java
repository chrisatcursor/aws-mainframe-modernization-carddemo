package com.carddemo.behavior.card;

import com.carddemo.card.model.Card;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.card.repository.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy COCRDLIC (card list for customer/account context).
 * Target: ordered listing of cards for a customer via {@link CardXref} joined to {@link Card}.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class CocrdlicBehaviorTest {

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private CardRepository cardRepository;

    private Long sampleCustomerId;

    @BeforeEach
    void pickCustomerWithCards() {
        sampleCustomerId = cardXrefRepository.findAll().stream()
                .mapToLong(CardXref::getCustomerId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void listCardsForCustomer_matchesXrefOrderingAndCardDetails() {
        List<CardXref> xrefs = cardXrefRepository.findByCustomerIdOrderByCardNumberAsc(sampleCustomerId);
        assertThat(xrefs).isNotEmpty();

        // TODO: CardListService.listByCustomer(sampleCustomerId) — assert same order and card numbers
        for (CardXref xref : xrefs) {
            Card card = cardRepository.findById(xref.getCardNumber()).orElseThrow();
            assertThat(card.getAccountId()).isEqualTo(xref.getAccountId());
            assertThat(card.getCardNumber()).isEqualTo(xref.getCardNumber());
        }
    }

    @Test
    void listCardsForAccount_usesPagingContract() {
        CardXref any = cardXrefRepository.findAll().stream().findFirst().orElseThrow();
        var page = cardRepository.findByAccountId(any.getAccountId(), PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().getFirst().getAccountId()).isEqualTo(any.getAccountId());
    }
}
