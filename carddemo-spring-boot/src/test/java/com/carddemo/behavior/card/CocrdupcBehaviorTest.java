package com.carddemo.behavior.card;

import com.carddemo.card.model.Card;
import com.carddemo.card.api.CardUpdateRequest;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.card.service.CardService;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private CardService cardService;

    private String cardNumber;
    private Long accountId;

    @BeforeEach
    void pickCard() {
        Card card = cardRepository.findAll().stream()
                .findFirst()
                .orElseThrow();
        cardNumber = card.getCardNumber();
        accountId = card.getAccountId();
    }

    @Test
    @Transactional
    void updateEmbossedNameAndExpiration_persistOnCardRow_whenValid() {
        Card before = cardRepository.findById(cardNumber).orElseThrow();
        LocalDate newExp = LocalDate.of(2028, 12, 31);
        cardService.updateCard(cardNumber, accountId, new CardUpdateRequest(
                "NEW NAME",
                "Y",
                newExp,
                before.getEmbossedName(),
                before.getActiveStatus(),
                before.getExpirationDate()));

        Card reloaded = cardRepository.findById(cardNumber).orElseThrow();
        assertThat(reloaded.getEmbossedName()).isEqualTo("NEW NAME");
        assertThat(reloaded.getExpirationDate()).isEqualTo(newExp);
        assertThat(reloaded.getActiveStatus()).isEqualTo("Y");
    }
}
