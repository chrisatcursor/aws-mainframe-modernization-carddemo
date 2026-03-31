package com.carddemo.card;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Page<CardDto> listCards(CardSearchCriteria criteria, Pageable pageable) {
        Specification<Card> spec = (root, query, cb) -> cb.conjunction();
        if (criteria.getAccountId() != null) {
            spec = spec.and(CardSpecifications.hasAccountId(criteria.getAccountId()));
        }
        String prefix = criteria.getCardNumberPrefix() == null ? "" : criteria.getCardNumberPrefix();
        spec = spec.and(CardSpecifications.cardNumberStartsWith(prefix));
        return cardRepository.findAll(spec, pageable).map(CardDto::from);
    }

    public Optional<CardDto> findByCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return Optional.empty();
        }
        return cardRepository.findById(cardNumber.trim()).map(CardDto::from);
    }

    public List<Card> findByAccountId(Long accountId) {
        return cardRepository.findByAccountId(accountId);
    }

    /**
     * Updates mutable card fields (migrated from COCRDUPC.cbl).
     */
    @Transactional
    public Card updateCard(String cardNumber, CardUpdateRequest request) {
        String id = cardNumber == null ? "" : cardNumber.trim();
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + id));
        if (!Objects.equals(card.getVersion(), request.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Card.class, id);
        }
        card.setEmbossedName(request.getEmbossedName() != null ? request.getEmbossedName().trim() : null);
        card.setExpirationDate(request.getExpirationDate() != null ? request.getExpirationDate().trim() : null);
        card.setActiveStatus(request.getActiveStatus() != null ? request.getActiveStatus().trim() : null);
        return cardRepository.save(card);
    }
}
