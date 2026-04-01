package com.carddemo.card;

import com.carddemo.card.dto.CardUpdateRequest;
import com.carddemo.card.dto.CardUpdateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class CardUpdateService {

    private final CardRepository cardRepository;
    private final CardXrefRepository cardXrefRepository;

    public CardUpdateService(CardRepository cardRepository, CardXrefRepository cardXrefRepository) {
        this.cardRepository = cardRepository;
        this.cardXrefRepository = cardXrefRepository;
    }

    @Transactional
    public CardUpdateResponse update(CardUpdateRequest req) {
        Optional<Card> opt = cardRepository.findById(req.cardNumber());
        if (opt.isEmpty()) {
            return new CardUpdateResponse(false, "Did not find cards for this search condition", null);
        }
        Card c = opt.get();
        if (!c.getAccountId().equals(req.accountId())) {
            return new CardUpdateResponse(false, "Card does not belong to account", null);
        }
        if (!c.getVersion().equals(req.expectedVersion())) {
            return new CardUpdateResponse(false, "Record was updated by another user; refresh and retry", null);
        }

        String exp = c.getExpirationDate() == null ? "" : c.getExpirationDate();
        String y = exp.length() >= 4 ? exp.substring(0, 4) : "";
        String m = exp.length() >= 7 ? exp.substring(5, 7) : "";
        String d = exp.length() >= 10 ? exp.substring(8, 10) : "";

        if (!eq(req.oldCvvCode(), c.getCvvCode())
                || !eqUpper(req.oldEmbossedName(), c.getEmbossedName())
                || !req.oldExpiryYear().equals(y)
                || !req.oldExpiryMonth().equals(m)
                || !req.oldExpiryDay().equals(d)
                || !req.oldActiveStatus().equals(c.getActiveStatus())) {
            return new CardUpdateResponse(false, "Record changed since last read; refresh and retry", null);
        }

        if (req.newCvvCode() != null) {
            c.setCvvCode(req.newCvvCode());
        }
        if (req.newEmbossedName() != null) {
            c.setEmbossedName(req.newEmbossedName().toUpperCase(Locale.ROOT));
        }
        if (req.newExpiryYear() != null && req.newExpiryMonth() != null && req.newExpiryDay() != null) {
            c.setExpirationDate(req.newExpiryYear() + "-" + req.newExpiryMonth() + "-" + req.newExpiryDay());
        }
        if (req.newActiveStatus() != null) {
            c.setActiveStatus(req.newActiveStatus());
        }

        String targetKey = req.newCardNumber() != null && !req.newCardNumber().isBlank()
                ? req.newCardNumber().trim()
                : req.cardNumber();
        if (targetKey.length() != 16) {
            return new CardUpdateResponse(false, "New card number must be 16 digits", null);
        }

        if (!targetKey.equals(c.getCardNumber())) {
            String oldKey = c.getCardNumber();
            cardXrefRepository.findById(oldKey).ifPresent(xref -> {
                cardXrefRepository.delete(xref);
                CardXref nx = new CardXref();
                nx.setCardNumber(targetKey);
                nx.setAccountId(xref.getAccountId());
                nx.setCustomerId(xref.getCustomerId());
                cardXrefRepository.save(nx);
            });
            cardRepository.delete(c);
            Card moved = new Card();
            moved.setCardNumber(targetKey);
            moved.setAccountId(c.getAccountId());
            moved.setCvvCode(c.getCvvCode());
            moved.setEmbossedName(c.getEmbossedName());
            moved.setExpirationDate(c.getExpirationDate());
            moved.setActiveStatus(c.getActiveStatus());
            cardRepository.save(moved);
            return new CardUpdateResponse(true, "OK", moved.getVersion());
        }

        cardRepository.save(c);
        return new CardUpdateResponse(true, "OK", c.getVersion());
    }

    private static boolean eqUpper(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.toUpperCase(Locale.ROOT).equals(b.toUpperCase(Locale.ROOT));
    }

    private static boolean eq(Integer a, Integer b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
