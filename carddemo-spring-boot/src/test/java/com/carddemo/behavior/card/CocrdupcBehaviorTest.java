package com.carddemo.behavior.card;

import com.carddemo.card.model.Card;
import com.carddemo.card.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy COCRDUPC (card update).
 * Target: mutable fields on {@link Card} (embossed name, expiration, active status, CVV).
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class CocrdupcBehaviorTest {

    @Autowired
    private CardRepository cardRepository;

    private String cardNumber;

    @BeforeEach
    void pickCard() {
        cardNumber = cardRepository.findAll().stream()
                .map(Card::getCardNumber)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @Disabled("Wire CardUpdateService (COCRDUPC) then remove; act is TODO")
    void updateEmbossedNameAndExpiration_persistOnCardRow_whenValid() {
        LocalDate newExp = LocalDate.of(2028, 12, 31);

        // TODO: cardUpdateService.updatePresentation(cardNumber, "NEW NAME", newExp, "Y");

        Card reloaded = cardRepository.findById(cardNumber).orElseThrow();
        assertThat(reloaded.getEmbossedName()).isEqualTo("NEW NAME");
        assertThat(reloaded.getExpirationDate()).isEqualTo(newExp);
        assertThat(reloaded.getActiveStatus()).isEqualTo("Y");
    }
}
