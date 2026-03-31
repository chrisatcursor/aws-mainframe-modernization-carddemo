package com.carddemo.transaction;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

import com.carddemo.common.PageResponse;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startId,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        var result = (startId != null && !startId.isBlank())
                ? transactionService.findByTransactionIdGreaterThanEqual(startId.strip(), pageable)
                : transactionService.listTransactions(pageable);

        model.addAttribute("page", PageResponse.from(result));
        model.addAttribute("startId", startId != null ? startId : "");
        model.addAttribute("pageSize", size);
        return "transaction/list";
    }

    @GetMapping("/detail")
    public String detailByQuery(
            @RequestParam(name = "transactionId", required = false) String transactionId,
            Model model) {
        if (transactionId == null || transactionId.isBlank()) {
            model.addAttribute("promptForId", true);
            return "transaction/detail";
        }
        var txn = transactionService.findTransactionById(transactionId.strip())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("txn", txn);
        return "transaction/detail";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("request", new TransactionCreateRequest());
        return "transaction/add";
    }

    @PostMapping("/add")
    public String addSubmit(
            @Valid @ModelAttribute("request") TransactionCreateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "transaction/add";
        }
        try {
            Transaction created = transactionService.createTransaction(request);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Transaction created: " + created.getTransactionId());
            return "redirect:/transaction/list";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("error.transaction", ex.getMessage());
            return "transaction/add";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        var txn = transactionService.findTransactionById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("txn", txn);
        return "transaction/detail";
    }
}
