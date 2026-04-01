package com.carddemo.card;

import com.carddemo.card.dto.CardDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CardDetailService {

    private final CardRepository cardRepository;

    public CardDetailService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public CardDetailResponse detail(long accountId, String cardNumberRaw) {
        String cardNum = cardNumberRaw == null ? "" : cardNumberRaw.trim();
        if (cardNum.isEmpty() || "*".equals(cardNum)) {
            return CardDetailResponse.error("Card number not provided");
        }
        if (cardNum.length() != 16 || !cardNum.chars().allMatch(Character::isDigit)) {
            return CardDetailResponse.error("CARD ID FILTER,IF SUPPLIED MUST BE A 16 DIGIT NUMBER");
        }
        Optional<Card> opt = cardRepository.findByCardNumberAndAccountId(cardNum, accountId);
        if (opt.isEmpty()) {
            return CardDetailResponse.error("Did not find cards for this search condition");
        }
        Card c = opt.get();
        String exp = c.getExpirationDate() == null ? "" : c.getExpirationDate();
        String year = exp.length() >= 4 ? exp.substring(0, 4) : "";
        String month = exp.length() >= 7 ? exp.substring(5, 7) : "";
        return new CardDetailResponse(null, c.getAccountId(), c.getCardNumber(), c.getEmbossedName(),
                year, month, c.getActiveStatus());
    }
}
