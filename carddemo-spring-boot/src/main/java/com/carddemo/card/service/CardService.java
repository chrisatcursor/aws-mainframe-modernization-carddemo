package com.carddemo.card.service;

import com.carddemo.account.repository.AccountRepository;
import com.carddemo.card.api.CardDetailResponse;
import com.carddemo.card.api.CardListResponse;
import com.carddemo.card.api.CardUpdateRequest;
import com.carddemo.card.api.CardUpdateResponse;
import com.carddemo.card.model.Card;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardRepository;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.common.error.ConflictException;
import com.carddemo.common.error.ResourceNotFoundException;
import com.carddemo.common.error.ValidationException;
import com.carddemo.common.validation.LegacyValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;

    public CardService(CardRepository cardRepository,
                       CardXrefRepository cardXrefRepository,
                       AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public CardListResponse listCards(Long accountId, String cardNumber, int page, int size) {
        if (accountId != null && !LegacyValidation.isValidAccountId(accountId)) {
            throw new ValidationException("Account number must be a non zero 11 digit number");
        }
        String normalizedCard = normalizeCardNumber(cardNumber);
        if (normalizedCard != null && !LegacyValidation.isValidCardNumber(normalizedCard)) {
            throw new ValidationException("Card number must be a 16 digit number");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Page<Card> cardPage = (accountId == null)
                ? cardRepository.findAll(PageRequest.of(safePage, safeSize, Sort.by("cardNumber").ascending()))
                : cardRepository.findByAccountId(accountId, PageRequest.of(safePage, safeSize, Sort.by("cardNumber").ascending()));

        List<Card> items = cardPage.getContent();
        if (normalizedCard != null) {
            items = items.stream().filter(c -> c.getCardNumber().equals(normalizedCard)).toList();
        }

        List<CardListResponse.CardRow> rows = items.stream()
                .map(card -> new CardListResponse.CardRow(
                        card.getCardNumber(),
                        card.getAccountId(),
                        card.getEmbossedName(),
                        card.getExpirationDate(),
                        card.getActiveStatus()))
                .toList();

        long totalElements = (normalizedCard == null) ? cardPage.getTotalElements() : rows.size();
        int totalPages = (normalizedCard == null) ? cardPage.getTotalPages() : (rows.isEmpty() ? 0 : 1);
        return new CardListResponse(rows, safePage, safeSize, totalElements, totalPages);
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getCardDetail(String cardNumber, Long accountId) {
        String normalizedCard = normalizeCardNumber(cardNumber);
        validateCardKey(normalizedCard, accountId);

        Card card = cardRepository.findById(normalizedCard)
                .orElseThrow(() -> new ResourceNotFoundException("Did not find cards for this search condition"));
        if (!Objects.equals(card.getAccountId(), accountId)) {
            throw new ResourceNotFoundException("Did not find cards for this search condition");
        }

        CardXref xref = cardXrefRepository.findByAccountIdAndCardNumber(accountId, normalizedCard)
                .orElseThrow(() -> new ResourceNotFoundException("Did not find cards for this search condition"));

        return new CardDetailResponse(
                card.getCardNumber(),
                card.getAccountId(),
                xref.getCustomerId(),
                card.getCvv(),
                card.getEmbossedName(),
                card.getExpirationDate(),
                card.getActiveStatus()
        );
    }

    @Transactional
    public CardUpdateResponse updateCard(String cardNumber, Long accountId, CardUpdateRequest request) {
        String normalizedCard = normalizeCardNumber(cardNumber);
        validateCardKey(normalizedCard, accountId);
        validateUpdateRequest(request);

        Card current = cardRepository.findById(normalizedCard)
                .orElseThrow(() -> new ResourceNotFoundException("Did not find cards for this search condition"));
        if (!Objects.equals(current.getAccountId(), accountId)) {
            throw new ResourceNotFoundException("Did not find cards for this search condition");
        }

        if (!Objects.equals(current.getEmbossedName(), request.originalEmbossedName())
                || !Objects.equals(current.getActiveStatus(), request.originalActiveStatus())
                || !Objects.equals(current.getExpirationDate(), request.originalExpirationDate())) {
            throw new ConflictException("Record changed by someone else. Please review");
        }

        current.setEmbossedName(request.embossedName().trim());
        current.setActiveStatus(request.activeStatus().trim().toUpperCase());
        current.setExpirationDate(request.expirationDate());
        cardRepository.save(current);

        return new CardUpdateResponse(
                current.getCardNumber(),
                current.getAccountId(),
                current.getEmbossedName(),
                current.getActiveStatus(),
                current.getExpirationDate(),
                "Changes committed"
        );
    }

    private void validateCardKey(String cardNumber, Long accountId) {
        if (!LegacyValidation.isValidCardNumber(cardNumber)) {
            throw new ValidationException("Card number must be a 16 digit number");
        }
        if (!LegacyValidation.isValidAccountId(accountId)) {
            throw new ValidationException("Account number must be a non zero 11 digit number");
        }
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new ResourceNotFoundException("Account number not found");
        }
    }

    private void validateUpdateRequest(CardUpdateRequest request) {
        LegacyValidation.requireNotNull(request, "Card update payload is required");
        LegacyValidation.requireCardName(request.embossedName(), "Card name");
        LegacyValidation.requireStatusYn(request.activeStatus(), "Card status");
        LegacyValidation.requireDate(request.expirationDate(), "Card expiry date");
        LegacyValidation.requireCardExpiry(request.expirationDate().getMonthValue(), request.expirationDate().getYear());
    }

    private String normalizeCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        String digits = cardNumber.trim();
        return digits.isBlank() ? null : digits;
    }
}
