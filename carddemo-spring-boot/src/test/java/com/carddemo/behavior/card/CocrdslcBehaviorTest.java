package com.carddemo.behavior.card;

import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
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

    @Test
    void selectCard_resolvesXref_whenAccountAndCardNumberMatch() {
        CardXref seed = cardXrefRepository.findAll().stream().findFirst().orElseThrow();

        // TODO: CardSelectionService.resolve(seed.getAccountId(), seed.getCardNumber())
        var found = cardXrefRepository.findByAccountIdAndCardNumber(seed.getAccountId(), seed.getCardNumber());

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(seed.getCustomerId());
        assertThat(found.get().getAccountId()).isEqualTo(seed.getAccountId());
    }
}
