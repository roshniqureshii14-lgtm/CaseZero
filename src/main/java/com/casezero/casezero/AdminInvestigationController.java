package com.casezero.casezero;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminInvestigationController {
    private final UserAccountRepository users;
    private final InvestigationDraftRepository investigations;

    public AdminInvestigationController(
            UserAccountRepository users,
            InvestigationDraftRepository investigations) {
        this.users = users;
        this.investigations = investigations;
    }

    @GetMapping("/admin/investigations")
    public String reviewQueue(Principal principal, Model model) {
        requireAdmin(principal);
        model.addAttribute("investigations",
                investigations.findAllByStatusOrderByUpdatedAtDesc("PENDING_REVIEW"));
        return "admin-investigations";
    }

    @PostMapping("/admin/investigations/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal,
            RedirectAttributes redirect) {
        requireAdmin(principal);
        InvestigationDraft draft = pending(id);
        draft.setStatus("APPROVED");
        investigations.save(draft);
        redirect.addFlashAttribute("successMessage",
                "The community investigation is now visible on its case page.");
        return "redirect:/admin/investigations";
    }

    @PostMapping("/admin/investigations/{id}/reject")
    public String reject(@PathVariable Long id, Principal principal,
            RedirectAttributes redirect) {
        requireAdmin(principal);
        InvestigationDraft draft = pending(id);
        draft.setStatus("REJECTED");
        investigations.save(draft);
        redirect.addFlashAttribute("successMessage",
                "The community investigation was rejected and remains private.");
        return "redirect:/admin/investigations";
    }

    private InvestigationDraft pending(Long id) {
        InvestigationDraft draft = investigations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Investigation not found."));
        if (!"PENDING_REVIEW".equals(draft.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This investigation has already been reviewed.");
        }
        return draft;
    }

    private void requireAdmin(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
        UserAccount account = users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Account not found."));
        if (!"ADMIN".equalsIgnoreCase(account.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
    }
}
