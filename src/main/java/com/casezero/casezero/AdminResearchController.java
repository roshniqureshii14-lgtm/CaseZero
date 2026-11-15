package com.casezero.casezero;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminResearchController {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "Missing Persons", "Unidentified Persons", "Unsolved Crimes", "Other Unresolved Cases");

    private final UserAccountRepository userRepository;
    private final CaseResearchReportRepository reportRepository;
    private final CaseRecordRepository caseRecordRepository;
    private final CaseIntakeService caseIntakeService;
    private final CaseDraftSuggestionService suggestionService;

    public AdminResearchController(UserAccountRepository userRepository,
            CaseResearchReportRepository reportRepository,
            CaseRecordRepository caseRecordRepository,
            CaseIntakeService caseIntakeService,
            CaseDraftSuggestionService suggestionService) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.caseRecordRepository = caseRecordRepository;
        this.caseIntakeService = caseIntakeService;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/admin/research")
    public String reviewQueue(Principal principal, Model model) {
        requireAdmin(principal);
        var reports = reportRepository.findAllByStatusOrderByCollectedAtDesc("PENDING");
        Map<Long, CaseDraftSuggestion> suggestions = new HashMap<>();
        for (CaseResearchReport report : reports) {
            suggestions.put(report.getId(), suggestionService.suggest(report));
        }
        model.addAttribute("reports", reports);
        model.addAttribute("suggestions", suggestions);
        return "admin-research";
    }

    @PostMapping("/admin/research/collect")
    public String collectReports(Principal principal, RedirectAttributes redirectAttributes) {
        requireAdmin(principal);
        try {
            int saved = caseIntakeService.collectPakistanReports();
            redirectAttributes.addFlashAttribute("successMessage",
                    saved + " new report(s) saved to the review queue. Duplicate source links were skipped.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "No configured publisher feed returned usable data. Check the application log; individual feed failures do not stop other publishers.");
        }
        return "redirect:/admin/research";
    }

    @PostMapping("/admin/research/{id}/approve")
    public String approveReport(@PathVariable Long id, @RequestParam String category,
            @RequestParam String location, @RequestParam String summary,
            Principal principal, RedirectAttributes redirectAttributes) {
        requireAdmin(principal);
        CaseResearchReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found."));
        if (!"PENDING".equals(report.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report already reviewed.");
        }
        String cleanLocation = location == null ? "" : location.trim();
        String cleanSummary = summary == null ? "" : summary.trim();
        if (!ALLOWED_CATEGORIES.contains(category) || cleanLocation.isBlank()
                || cleanLocation.length() > 120 || cleanSummary.isBlank()
                || cleanSummary.length() > 1200) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Choose a category and verify the location (max 120 characters) and summary (max 1,200 characters).");
            return "redirect:/admin/research";
        }
        if (caseRecordRepository.existsBySourceUrl(report.getSourceUrl())) {
            report.setStatus("DUPLICATE");
            reportRepository.save(report);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "A case with this source link already exists. This report was marked as a duplicate.");
            return "redirect:/admin/research";
        }
        CaseRecord record = new CaseRecord(
                report.getTitle().length() > 150 ? report.getTitle().substring(0, 150) : report.getTitle(),
                category, cleanLocation, cleanSummary, "Under Review",
                report.getSourceName(), report.getSourceUrl());
        record.setPublished(true);
        caseRecordRepository.save(record);
        report.setStatus("APPROVED");
        reportRepository.save(report);
        redirectAttributes.addFlashAttribute("successMessage",
                "The reviewed report has been added to the public case archive.");
        return "redirect:/admin/research";
    }

    @PostMapping("/admin/research/{id}/reject")
    public String rejectReport(@PathVariable Long id, Principal principal,
            RedirectAttributes redirectAttributes) {
        requireAdmin(principal);
        CaseResearchReport report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found."));
        if ("PENDING".equals(report.getStatus())) {
            report.setStatus("REJECTED");
            reportRepository.save(report);
        }
        redirectAttributes.addFlashAttribute("successMessage", "The report was rejected.");
        return "redirect:/admin/research";
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
        UserAccount account = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not found."));
        if (!"ADMIN".equalsIgnoreCase(account.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
    }
}
