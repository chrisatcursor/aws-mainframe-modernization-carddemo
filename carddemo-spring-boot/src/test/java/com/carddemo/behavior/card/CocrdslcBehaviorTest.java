package com.carddemo.behavior.card;

import com.carddemo.card.api.CardDetailResponse;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.card.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy COCRDSLC (select card in account context).
 * Target: resolve a single {@link CardXref} by account and card number.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class CocrdslcBehaviorTest {

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private CardService cardService;

    @Test
    void selectCard_resolvesXref_whenAccountAndCardNumberMatch() {
        CardXref seed = cardXrefRepository.findAll().stream().findFirst().orElseThrow();

        CardDetailResponse detail = cardService.getCardDetail(seed.getCardNumber(), seed.getAccountId());
        assertThat(detail.customerId()).isEqualTo(seed.getCustomerId());
        assertThat(detail.accountId()).isEqualTo(seed.getAccountId());
        assertThat(detail.cardNumber()).isEqualTo(seed.getCardNumber());
    }
}
