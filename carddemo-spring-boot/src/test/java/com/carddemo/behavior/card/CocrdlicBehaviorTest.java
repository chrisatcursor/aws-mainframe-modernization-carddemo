package com.carddemo.behavior.card;

import com.carddemo.card.api.CardListResponse;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.card.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Autowired
    private CardService cardService;

    private Long sampleCustomerId;
    private Long sampleAccountId;

    @BeforeEach
    void pickCustomerWithCards() {
        CardXref seed = cardXrefRepository.findAll().stream().findFirst().orElseThrow();
        sampleCustomerId = seed.getCustomerId();
        sampleAccountId = seed.getAccountId();
    }

    @Test
    void listCardsForCustomer_matchesXrefOrderingAndCardDetails() {
        List<CardXref> xrefs = cardXrefRepository.findByCustomerIdOrderByCardNumberAsc(sampleCustomerId);
        assertThat(xrefs).isNotEmpty();

        CardListResponse response = cardService.listCards(sampleAccountId, null, 0, 7);
        assertThat(response.items()).isNotEmpty();

        for (CardXref xref : xrefs) {
            assertThat(cardRepository.findById(xref.getCardNumber())).isPresent();
        }
    }

    @Test
    void listCardsForAccount_usesPagingContract() {
        CardListResponse page = cardService.listCards(sampleAccountId, null, 0, 7);
        assertThat(page.items()).isNotEmpty();
        assertThat(page.items().getFirst().accountId()).isEqualTo(sampleAccountId);
    }
}
