package com.carddemo.transaction;

import com.carddemo.account.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final AccountRepository accountRepository;
    private final PaymentService paymentService;

    public PaymentController(AccountRepository accountRepository, PaymentService paymentService) {
        this.accountRepository = accountRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/bill")
    public String billForm(@RequestParam(required = false) Long acctId, Model model) {
        if (acctId != null) {
            accountRepository
                    .findById(acctId)
                    .ifPresentOrElse(
                            account -> model.addAttribute("account", account),
                            () -> model.addAttribute("loadError", "Account not found: " + acctId));
        }
        return "payment/bill";
    }

    @PostMapping("/bill")
    public String billSubmit(@RequestParam Long acctId, RedirectAttributes redirectAttributes) {
        try {
            PaymentResult result = paymentService.payBill(acctId);
            if (result.success()) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Bill payment posted. Transaction "
                                + result.transactionId()
                                + ", amount "
                                + result.paidAmount());
                return "redirect:/payment/bill";
            }
            redirectAttributes.addFlashAttribute("errorMessage", result.message());
            return "redirect:/payment/bill?acctId=" + acctId;
        } catch (EntityNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/payment/bill?acctId=" + acctId;
        }
    }
}
