package com.carddemo.auth.pending;

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
@RequestMapping("/admin/pending-auth")
@PreAuthorize("hasRole('ADMIN')")
public class PendingAuthController {

    private final PendingAuthService pendingAuthService;

    public PendingAuthController(PendingAuthService pendingAuthService) {
        this.pendingAuthService = pendingAuthService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("summaries", pendingAuthService.listSummaries());
        return "pending-auth/list";
    }

    @GetMapping("/{acctId}")
    public String detail(@PathVariable long acctId, Model model) {
        model.addAttribute("acctId", acctId);
        model.addAttribute("details", pendingAuthService.listDetails(acctId));
        return "pending-auth/detail";
    }

    @PostMapping("/{acctId}/detail/{detailId}/fraud")
    public String fraud(
            @PathVariable long acctId,
            @PathVariable long detailId,
            @RequestParam String action,
            RedirectAttributes ra) {
        pendingAuthService.applyFraudToggle(acctId, detailId, action);
        ra.addFlashAttribute("message", "Fraud flag updated");
        return "redirect:/admin/pending-auth/" + acctId;
    }
}
