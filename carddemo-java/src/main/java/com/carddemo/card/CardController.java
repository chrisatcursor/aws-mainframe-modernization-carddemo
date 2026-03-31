package com.carddemo.card;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.carddemo.common.PageResponse;
import com.carddemo.common.PaginationConstants;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Card list and detail (migrated from COCRDLIC.cbl).
 * Admins may browse all cards with optional filters; regular users are limited to their demo account
 * derived from the user id (e.g. USER0001 → account 1), matching seed data.
 */
@Controller
@RequestMapping("/card")
public class CardController {

    private static final Pattern DEMO_USER_ACCOUNT_PATTERN = Pattern.compile("^USER0*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final CardService cardService;
    private final CardRepository cardRepository;

    public CardController(CardService cardService, CardRepository cardRepository) {
        this.cardService = cardService;
        this.cardRepository = cardRepository;
    }

    /**
     * Read-only card detail by query param (COCRDSLC-style); same access rules as {@link #detail}.
     */
    @GetMapping("/detail")
    public String detailByQuery(
            @RequestParam(name = "cardNumber", required = false) String cardNumber,
            Authentication authentication,
            Model model) {
        if (cardNumber == null || cardNumber.isBlank()) {
            model.addAttribute("promptForId", true);
            return "card/detail";
        }
        String trimmed = cardNumber.trim();
        CardDto card = cardService.findByCardNumber(trimmed)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isAdmin(authentication) && !canAccessAccount(authentication.getName(), card.accountId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("card", card);
        return "card/detail";
    }

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + PaginationConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "cardNumber", required = false) String cardNumberPrefix,
            Authentication authentication,
            Model model) {

        CardSearchCriteria criteria = new CardSearchCriteria();
        if (isAdmin(authentication)) {
            criteria.setAccountId(accountId);
            criteria.setCardNumberPrefix(cardNumberPrefix);
        } else {
            criteria.setAccountId(resolveRestrictedAccountId(authentication.getName()));
            if (cardNumberPrefix != null && !cardNumberPrefix.isBlank()) {
                criteria.setCardNumberPrefix(cardNumberPrefix);
            }
        }

        int pageSize = size > 0 ? size : PaginationConstants.DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize, Sort.by("cardNumber"));

        var result = cardService.listCards(criteria, pageable);
        model.addAttribute("page", PageResponse.from(result));
        model.addAttribute("pageSize", result.getSize());
        model.addAttribute("criteria", criteria);
        model.addAttribute("adminBrowse", isAdmin(authentication));
        return "card/list";
    }

    @GetMapping("/{cardNumber:[0-9]+}")
    public String detail(
            @PathVariable String cardNumber,
            Authentication authentication,
            Model model) {

        CardDto card = cardService.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!isAdmin(authentication) && !canAccessAccount(authentication.getName(), card.accountId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("card", card);
        return "card/detail";
    }

    @GetMapping("/{cardNumber:[0-9]+}/edit")
    public String editForm(
            @PathVariable String cardNumber,
            Authentication authentication,
            Model model) {

        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isAdmin(authentication) && !canAccessAccount(authentication.getName(), card.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        CardUpdateRequest request = new CardUpdateRequest();
        request.setEmbossedName(card.getEmbossedName());
        request.setExpirationDate(card.getExpirationDate());
        request.setActiveStatus(card.getActiveStatus());
        request.setVersion(card.getVersion());

        model.addAttribute("card", card);
        model.addAttribute("request", request);
        return "card/edit";
    }

    @PostMapping("/{cardNumber:[0-9]+}/edit")
    public String updateCard(
            @PathVariable String cardNumber,
            @Valid @ModelAttribute("request") CardUpdateRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!isAdmin(authentication) && !canAccessAccount(authentication.getName(), card.getAccountId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("card", card);
            return "card/edit";
        }

        try {
            cardService.updateCard(cardNumber, request);
            redirectAttributes.addFlashAttribute("successMessage", "Card updated successfully.");
            return "redirect:/card/" + cardNumber;
        } catch (ObjectOptimisticLockingFailureException ex) {
            Card fresh = cardRepository.findById(cardNumber)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            CardUpdateRequest refreshed = new CardUpdateRequest();
            refreshed.setEmbossedName(fresh.getEmbossedName());
            refreshed.setExpirationDate(fresh.getExpirationDate());
            refreshed.setActiveStatus(fresh.getActiveStatus());
            refreshed.setVersion(fresh.getVersion());
            model.addAttribute("card", fresh);
            model.addAttribute("request", refreshed);
            model.addAttribute(
                    "optimisticLockError",
                    "This card was updated elsewhere. The form has been refreshed with the latest data; please try again.");
            return "card/edit";
        }
    }

    private static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static Long resolveRestrictedAccountId(String username) {
        return parseDemoAccountFromUsername(username).orElse(-1L);
    }

    private static Optional<Long> parseDemoAccountFromUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        Matcher m = DEMO_USER_ACCOUNT_PATTERN.matcher(username.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(m.group(1)));
    }

    private static boolean canAccessAccount(String username, Long accountId) {
        if (accountId == null) {
            return false;
        }
        return parseDemoAccountFromUsername(username)
                .map(id -> id.equals(accountId))
                .orElse(false);
    }
}
