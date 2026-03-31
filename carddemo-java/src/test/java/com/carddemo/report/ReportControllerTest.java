package com.carddemo.report;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.carddemo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void showReportForm_rendersSubmitTemplate() throws Exception {
        mockMvc.perform(get("/report/submit"))
                .andExpect(status().isOk())
                .andExpect(view().name("report/submit"));
    }

    @Test
    void showReportForm_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/report/submit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser
    void submitReport_validDates_redirectsWithSuccessFlash() throws Exception {
        String msg = "Transaction report request submitted for 2024-01-01 to 2024-01-31";
        when(reportService.submitReport("2024-01-01", "2024-01-31"))
                .thenReturn(ReportService.ReportResult.success(msg));

        mockMvc.perform(post("/report/submit")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/report/submit"))
                .andExpect(flash().attribute("successMessage", msg));

        verify(reportService).submitReport("2024-01-01", "2024-01-31");
    }

    @Test
    @WithMockUser
    void submitReport_invalidStartDate_redirectsWithErrorFlash() throws Exception {
        String err = "Invalid start date format. Use YYYY-MM-DD.";
        when(reportService.submitReport(eq("not-a-date"), eq("2024-01-15")))
                .thenReturn(ReportService.ReportResult.error(err));

        mockMvc.perform(post("/report/submit")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2024-01-15")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/report/submit"))
                .andExpect(flash().attribute("errorMessage", err));
    }

    @Test
    @WithMockUser
    void submitReport_startAfterEnd_redirectsWithErrorFlash() throws Exception {
        String err = "Start date must be before end date";
        when(reportService.submitReport(eq("2024-02-01"), eq("2024-01-01")))
                .thenReturn(ReportService.ReportResult.error(err));

        mockMvc.perform(post("/report/submit")
                        .param("startDate", "2024-02-01")
                        .param("endDate", "2024-01-01")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", err));
    }

    @Test
    @WithMockUser
    void submitReport_missingCsrf_returns403() throws Exception {
        when(reportService.submitReport(anyString(), anyString()))
                .thenReturn(ReportService.ReportResult.success("ok"));

        mockMvc.perform(post("/report/submit")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isForbidden());
    }
}
