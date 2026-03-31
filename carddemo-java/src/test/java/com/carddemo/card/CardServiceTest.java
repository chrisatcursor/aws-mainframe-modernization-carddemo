package com.carddemo.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    @SuppressWarnings("unchecked")
    void listCards_delegatesToRepositoryWithSpecification() {
        Pageable pageable = PageRequest.of(0, 10);
        Card card = sampleCard();
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));

        CardSearchCriteria criteria = new CardSearchCriteria();
        criteria.setAccountId(1L);
        criteria.setCardNumberPrefix("96");

        Page<CardDto> page = cardService.listCards(criteria, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().cardNumber()).isEqualTo("9680294154603697");
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void findByCardNumber_returnsDtoWhenPresent() {
        Card card = sampleCard();
        when(cardRepository.findById("9680294154603697")).thenReturn(Optional.of(card));

        Optional<CardDto> dto = cardService.findByCardNumber("9680294154603697");

        assertThat(dto).isPresent();
        assertThat(dto.get().accountId()).isEqualTo(1L);
    }

    @Test
    void findByCardNumber_blank_returnsEmpty() {
        assertThat(cardService.findByCardNumber("")).isEmpty();
        assertThat(cardService.findByCardNumber("   ")).isEmpty();
    }

    private static Card sampleCard() {
        Card card = new Card();
        card.setCardNumber("9680294154603697");
        card.setAccountId(1L);
        card.setEmbossedName("Immanuel Kessler");
        card.setExpirationDate("2025-05-20");
        card.setActiveStatus("Y");
        return card;
    }
}
