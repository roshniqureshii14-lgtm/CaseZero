package com.casezero.casezero;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final CaseResearchReportRepository reports;
    private final InvestigationDraftRepository investigations;

    public AdminDashboardController(CaseResearchReportRepository reports,
            InvestigationDraftRepository investigations) {
        this.reports = reports;
        this.investigations = investigations;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("pendingResearchCount",
                reports.findAllByStatusOrderByCollectedAtDesc("PENDING").size());
        model.addAttribute("pendingInvestigationCount",
                investigations.findAllByStatusOrderByUpdatedAtDesc("PENDING_REVIEW").size());
        return "admin-dashboard";
    }
}
