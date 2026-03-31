package com.carddemo.account;

import com.carddemo.card.Card;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/view")
    public String viewAccount(@RequestParam(name = "acctId", required = false) Long acctId, Model model) {
        if (acctId == null) {
            model.addAttribute("promptForId", true);
            return "account/view";
        }

        AccountDetailDto detail = accountService.findAccountWithCustomer(acctId);
        List<Card> cards = accountService.findCardsForAccount(acctId);
        model.addAttribute("detail", detail);
        model.addAttribute("cards", cards);
        return "account/view";
    }

    @GetMapping("/update")
    public String updateForm(@RequestParam(name = "acctId", required = false) Long acctId, Model model) {
        if (acctId == null) {
            model.addAttribute("promptForId", true);
            return "account/edit";
        }

        AccountDetailDto detail = accountService.findAccountWithCustomer(acctId);
        if (detail.custId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No customer linked to this account for update");
        }

        model.addAttribute("detail", detail);
        model.addAttribute("request", AccountUpdateRequest.fromDetail(detail));
        return "account/edit";
    }

    @PostMapping("/update")
    public String updateAccount(
            @Valid @ModelAttribute("request") AccountUpdateRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long acctId = request.getAcctId();

        if (bindingResult.hasErrors()) {
            model.addAttribute("detail", accountService.findAccountWithCustomer(acctId));
            return "account/edit";
        }

        try {
            accountService.updateAccount(acctId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Account updated successfully.");
            return "redirect:/account/view?acctId=" + acctId;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("global", ex.getMessage());
            model.addAttribute("detail", accountService.findAccountWithCustomer(acctId));
            return "account/edit";
        } catch (ObjectOptimisticLockingFailureException ex) {
            AccountDetailDto fresh = accountService.findAccountWithCustomer(acctId);
            if (fresh.custId() == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No customer linked to this account for update");
            }
            model.addAttribute("detail", fresh);
            model.addAttribute("request", AccountUpdateRequest.fromDetail(fresh));
            model.addAttribute(
                    "optimisticLockError",
                    "This account or customer was updated elsewhere. The form has been refreshed with the latest data; please try again.");
            return "account/edit";
        }
    }
}
