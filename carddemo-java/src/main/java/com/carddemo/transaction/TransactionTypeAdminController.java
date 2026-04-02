package com.carddemo.transaction;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/transaction-types")
@PreAuthorize("hasRole('ADMIN')")
public class TransactionTypeAdminController {

    private final TransactionTypeRepository repository;

    public TransactionTypeAdminController(TransactionTypeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", repository.findAllByOrderByTrTypeAsc());
        return "transaction-type/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("trType", "");
        model.addAttribute("description", "");
        return "transaction-type/edit";
    }

    @GetMapping("/{trType}/edit")
    public String editForm(@PathVariable String trType, Model model) {
        TransactionType t = repository.findById(trType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown type"));
        model.addAttribute("trType", t.getTrType());
        model.addAttribute("description", t.getDescription());
        return "transaction-type/edit";
    }

    @PostMapping
    public String save(
            @RequestParam String trType,
            @RequestParam String description,
            RedirectAttributes ra) {
        String code = trType.trim();
        if (code.length() != 2) {
            ra.addFlashAttribute("error", "Transaction type must be 2 characters");
            return "redirect:/admin/transaction-types";
        }
        TransactionType t = repository.findById(code).orElseGet(TransactionType::new);
        t.setTrType(code);
        t.setDescription(description.trim());
        repository.save(t);
        ra.addFlashAttribute("message", "Saved");
        return "redirect:/admin/transaction-types";
    }

    @PostMapping("/{trType}/delete")
    public String delete(@PathVariable String trType, RedirectAttributes ra) {
        repository.deleteById(trType);
        ra.addFlashAttribute("message", "Deleted");
        return "redirect:/admin/transaction-types";
    }
}
