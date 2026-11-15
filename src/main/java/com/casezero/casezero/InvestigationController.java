package com.casezero.casezero;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/investigations")
public class InvestigationController {
    private final InvestigationDraftRepository drafts;
    private final UserAccountRepository users;
    private final CaseRecordRepository cases;
    private final InvestigationAttachmentRepository attachments;
    private final SupabaseStorageService storage;

    public InvestigationController(
            InvestigationDraftRepository drafts,
            UserAccountRepository users,
            CaseRecordRepository cases,
            InvestigationAttachmentRepository attachments,
            SupabaseStorageService storage) {
        this.drafts = drafts;
        this.users = users;
        this.cases = cases;
        this.attachments = attachments;
        this.storage = storage;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "all") String view,
            Principal principal,
            Model model) {
        List<InvestigationDraft> all = drafts
                .findAllByAuthor_EmailIgnoreCaseOrderByUpdatedAtDesc(principal.getName());
        List<InvestigationDraft> shown = switch (view) {
            case "drafts" -> all.stream().filter(d -> "DRAFT".equals(d.getStatus())).toList();
            case "submitted" -> all.stream().filter(d -> !"DRAFT".equals(d.getStatus())).toList();
            default -> all;
        };
        model.addAttribute("drafts", shown);
        model.addAttribute("view", view);
        return "investigations";
    }

    @GetMapping("/evidence")
    public String evidenceVault(Principal principal, Model model) {
        model.addAttribute("attachments",
                attachments.findAllByDraft_Author_EmailIgnoreCaseOrderByUploadedAtDesc(principal.getName()));
        return "evidence-vault";
    }

    @GetMapping("/new")
    public String createPage(@RequestParam Long caseId, Model model) {
        CaseRecord caseRecord = publicCase(caseId);
        InvestigationForm form = new InvestigationForm();
        form.setCaseId(caseRecord.getId());
        model.addAttribute("form", form);
        model.addAttribute("caseRecord", caseRecord);
        model.addAttribute("pageTitle", "Start an Investigation");
        model.addAttribute("attachments", List.of());
        return "investigation-editor";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") InvestigationForm form,
            BindingResult binding,
            Principal principal,
            Model model) {
        CaseRecord caseRecord = form.getCaseId() == null ? null
                : cases.findById(form.getCaseId()).filter(CaseRecord::isPublished).orElse(null);
        if (caseRecord == null) {
            binding.rejectValue("caseId", "case.invalid", "The selected public case could not be found.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("caseRecord", caseRecord);
            model.addAttribute("pageTitle", "Start an Investigation");
            model.addAttribute("attachments", List.of());
            return "investigation-editor";
        }

        InvestigationDraft draft = new InvestigationDraft();
        draft.setCaseRecord(caseRecord);
        draft.setAuthor(currentUser(principal));
        copy(form, draft);
        draft.setStatus("DRAFT");
        InvestigationDraft saved = drafts.save(draft);
        return "redirect:/investigations/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Principal principal, Model model) {
        InvestigationDraft draft = owned(id, principal);
        model.addAttribute("draft", draft);
        model.addAttribute("attachments", attachments.findAllByDraft_IdOrderByUploadedAtDesc(id));
        return "investigation-view";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Principal principal, Model model) {
        InvestigationDraft draft = owned(id, principal);
        requireDraft(draft);
        populateEditor(model, draft, form(draft));
        return "investigation-editor";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") InvestigationForm form,
            BindingResult binding,
            Principal principal,
            Model model) {
        InvestigationDraft draft = owned(id, principal);
        requireDraft(draft);
        if (binding.hasErrors()) {
            populateEditor(model, draft, form);
            return "investigation-editor";
        }
        copy(form, draft);
        drafts.save(draft);
        return "redirect:/investigations/" + id + "/edit";
    }

    @PostMapping("/{id}/attachments")
    public String uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal,
            RedirectAttributes redirect) {
        InvestigationDraft draft = owned(id, principal);
        requireDraft(draft);
        try {
            SupabaseStorageService.UploadResult result = storage.upload(id, file);
            InvestigationAttachment attachment = new InvestigationAttachment();
            attachment.setDraft(draft);
            attachment.setOriginalFilename(result.filename());
            attachment.setStoragePath(result.path());
            attachment.setContentType(result.contentType());
            attachment.setFileSize(result.size());
            attachments.save(attachment);
            redirect.addFlashAttribute("successMessage", "Evidence attached to your private draft.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/investigations/" + id + "/edit";
    }

    @PostMapping("/attachments/{attachmentId}/delete")
    public String deleteAttachment(
            @PathVariable Long attachmentId,
            Principal principal,
            RedirectAttributes redirect) {
        InvestigationAttachment attachment = ownedAttachment(attachmentId, principal);
        requireDraft(attachment.getDraft());
        Long draftId = attachment.getDraft().getId();
        storage.delete(attachment.getStoragePath());
        attachments.delete(attachment);
        redirect.addFlashAttribute("successMessage", "Evidence attachment removed.");
        return "redirect:/investigations/" + draftId + "/edit";
    }

    @GetMapping("/attachments/{attachmentId}/preview")
    public ResponseEntity<byte[]> previewAttachment(
            @PathVariable Long attachmentId, Principal principal) {
        return attachmentResponse(ownedAttachment(attachmentId, principal), false);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long attachmentId, Principal principal) {
        return attachmentResponse(ownedAttachment(attachmentId, principal), true);
    }

    @PostMapping("/{id}/submit")
    public String submit(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes redirect) {
        InvestigationDraft draft = owned(id, principal);
        requireDraft(draft);
        draft.setStatus("PENDING_REVIEW");
        drafts.save(draft);
        redirect.addFlashAttribute("successMessage",
                "Submitted for review. Your investigation and attachments remain private while awaiting review.");
        return "redirect:/investigations";
    }

    private ResponseEntity<byte[]> attachmentResponse(
            InvestigationAttachment attachment, boolean download) {
        byte[] bytes = storage.download(attachment.getStoragePath());
        String disposition = download ? "attachment" : "inline";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.builder(disposition)
                                .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    private void populateEditor(Model model, InvestigationDraft draft, InvestigationForm form) {
        model.addAttribute("form", form);
        model.addAttribute("caseRecord", draft.getCaseRecord());
        model.addAttribute("draftId", draft.getId());
        model.addAttribute("pageTitle", "Edit Investigation Draft");
        model.addAttribute("attachments",
                attachments.findAllByDraft_IdOrderByUploadedAtDesc(draft.getId()));
    }

    private void requireDraft(InvestigationDraft draft) {
        if (!"DRAFT".equals(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This investigation is no longer editable.");
        }
    }

    private UserAccount currentUser(Principal principal) {
        return users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private CaseRecord publicCase(Long id) {
        return cases.findById(id).filter(CaseRecord::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private InvestigationDraft owned(Long id, Principal principal) {
        return drafts.findByIdAndAuthor_EmailIgnoreCase(id, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private InvestigationAttachment ownedAttachment(Long id, Principal principal) {
        return attachments.findByIdAndDraft_Author_EmailIgnoreCase(id, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void copy(InvestigationForm form, InvestigationDraft draft) {
        draft.setTitle(form.getTitle().trim());
        draft.setFindings(form.getFindings().trim());
        draft.setSourceNotes(form.getSourceNotes() == null ? null : form.getSourceNotes().trim());
    }

    private InvestigationForm form(InvestigationDraft draft) {
        InvestigationForm form = new InvestigationForm();
        form.setCaseId(draft.getCaseRecord().getId());
        form.setTitle(draft.getTitle());
        form.setFindings(draft.getFindings());
        form.setSourceNotes(draft.getSourceNotes());
        return form;
    }
}
