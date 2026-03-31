package com.carddemo.batch;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/batch")
public class AdminBatchController {

    private final BatchJobLaunchService batchJobLaunchService;

    public AdminBatchController(BatchJobLaunchService batchJobLaunchService) {
        this.batchJobLaunchService = batchJobLaunchService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("jobs", java.util.List.of(
                "purgeExpiredAuth",
                "transactionTypeFileUpdate",
                "imsAuthStagingLoad",
                "imsAuthStagingUnload"));
        return "admin/batch";
    }

    @PostMapping("/run")
    public String run(
            @RequestParam String job,
            RedirectAttributes redirectAttributes) {
        try {
            switch (job) {
                case "purgeExpiredAuth" -> batchJobLaunchService.runPendingAuthPurge();
                case "transactionTypeFileUpdate" -> batchJobLaunchService.runTransactionTypeBatch();
                case "imsAuthStagingLoad" -> batchJobLaunchService.runImsAuthLoad();
                case "imsAuthStagingUnload" -> batchJobLaunchService.runImsAuthUnload();
                default -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Unknown job");
                    return "redirect:/admin/batch";
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Job started: " + job);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/batch";
    }
}
