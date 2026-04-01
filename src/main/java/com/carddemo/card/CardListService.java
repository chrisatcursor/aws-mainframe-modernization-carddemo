package com.carddemo.card;

import com.carddemo.card.dto.CardListResponse;
import com.carddemo.card.dto.CardRowDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@Service
public class CardListService {

    private static final int PAGE_SIZE = 7;

    private final CardRepository cardRepository;

    public CardListService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public CardListResponse list(String accountFilterRaw, String cardFilterRaw, String cursorRaw) {
        Long accountFilter = parseAccountFilter(accountFilterRaw);
        String cardFilter = normalizeCardFilter(cardFilterRaw);
        String cursor = cursorRaw == null || cursorRaw.isBlank() ? null : cursorRaw.trim();

        if (accountFilter == null && (cardFilter == null || cardFilter.isEmpty())) {
            return CardListResponse.error("No input received");
        }
        if (accountFilter != null && accountFilter == Long.MIN_VALUE) {
            return CardListResponse.error("ACCOUNT FILTER,IF SUPPLIED MUST BE A 11 DIGIT NUMBER");
        }
        if (cardFilter != null && cardFilter.isEmpty()
                && cardFilterRaw != null
                && !cardFilterRaw.isBlank()
                && !"*".equals(cardFilterRaw.trim())) {
            return CardListResponse.error("CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER");
        }

        String startKey = (cursor != null && !cursor.isBlank())
                ? cursor.trim()
                : (cardFilter != null && !cardFilter.isEmpty() ? cardFilter : "");
        var page = PageRequest.of(0, 500);
        List<Card> chunk = cardRepository.findByCardNumberGreaterThanEqualOrderByCardNumberAsc(startKey, page);

        List<Card> filtered = new ArrayList<>();
        for (Card c : chunk) {
            if (accountFilter != null && !c.getAccountId().equals(accountFilter)) {
                continue;
            }
            if (cardFilter != null && !cardFilter.isEmpty() && !c.getCardNumber().equals(cardFilter)) {
                continue;
            }
            filtered.add(c);
        }

        boolean hasNext = filtered.size() > PAGE_SIZE;
        String nextCursor = hasNext ? filtered.get(PAGE_SIZE).getCardNumber() : null;
        List<CardRowDto> rows = filtered.stream()
                .limit(PAGE_SIZE)
                .map(c -> new CardRowDto(c.getCardNumber(), c.getAccountId(), c.getActiveStatus()))
                .toList();

        return new CardListResponse(null, hasNext, nextCursor, rows);
    }

    private static Long parseAccountFilter(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || "*".equals(t)) {
            return null;
        }
        if (t.length() != 11 || !t.chars().allMatch(Character::isDigit)) {
            return Long.MIN_VALUE;
        }
        return Long.parseLong(t);
    }

    private static String normalizeCardFilter(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty() || "*".equals(t)) {
            return null;
        }
        if (t.length() != 16 || !t.chars().allMatch(Character::isDigit)) {
            return "";
        }
        return t;
    }
}
