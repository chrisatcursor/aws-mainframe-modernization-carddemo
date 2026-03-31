package com.carddemo.report;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/submit")
    public String showReportForm() {
        return "report/submit";
    }

    @PostMapping("/submit")
    public String submitReport(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            RedirectAttributes redirectAttributes) {

        ReportService.ReportResult result = reportService.submitReport(startDate, endDate);
        if (result.success()) {
            redirectAttributes.addFlashAttribute("successMessage", result.message());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.message());
        }
        return "redirect:/report/submit";
    }
}
