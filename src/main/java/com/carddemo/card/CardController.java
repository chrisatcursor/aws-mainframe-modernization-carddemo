package com.carddemo.card;

import com.carddemo.card.dto.CardDetailResponse;
import com.carddemo.card.dto.CardListResponse;
import com.carddemo.card.dto.CardUpdateRequest;
import com.carddemo.card.dto.CardUpdateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardListService cardListService;
    private final CardDetailService cardDetailService;
    private final CardUpdateService cardUpdateService;

    public CardController(
            CardListService cardListService,
            CardDetailService cardDetailService,
            CardUpdateService cardUpdateService) {
        this.cardListService = cardListService;
        this.cardDetailService = cardDetailService;
        this.cardUpdateService = cardUpdateService;
    }

    @GetMapping("/list")
    public CardListResponse list(
            @RequestParam(name = "accountId", required = false) String accountId,
            @RequestParam(name = "cardNumber", required = false) String cardNumber,
            @RequestParam(name = "cursor", required = false) String cursor) {
        return cardListService.list(accountId, cardNumber, cursor);
    }

    @GetMapping("/detail")
    public CardDetailResponse detail(
            @RequestParam("accountId") long accountId,
            @RequestParam("cardNumber") String cardNumber) {
        return cardDetailService.detail(accountId, cardNumber);
    }

    @PutMapping
    public CardUpdateResponse update(@Valid @RequestBody CardUpdateRequest request) {
        return cardUpdateService.update(request);
    }
}
