package com.carddemo.card.controller;

import com.carddemo.card.api.CardDetailResponse;
import com.carddemo.card.api.CardListResponse;
import com.carddemo.card.api.CardUpdateRequest;
import com.carddemo.card.api.CardUpdateResponse;
import com.carddemo.card.service.CardService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public CardListResponse listCards(@RequestParam(required = false) Long accountId,
                                      @RequestParam(required = false) String cardNumber,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "7") int size) {
        return cardService.listCards(accountId, cardNumber, page, size);
    }

    @GetMapping("/{cardNumber}")
    public CardDetailResponse getCardDetail(@PathVariable String cardNumber,
                                            @RequestParam Long accountId) {
        return cardService.getCardDetail(cardNumber, accountId);
    }

    @PutMapping("/{cardNumber}")
    public CardUpdateResponse updateCard(@PathVariable String cardNumber,
                                         @RequestParam Long accountId,
                                         @Valid @RequestBody CardUpdateRequest request) {
        return cardService.updateCard(cardNumber, accountId, request);
    }
}
