package com.casezero.casezero;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/investigations")
public class InvestigationController {
 private final InvestigationDraftRepository drafts; private final UserAccountRepository users; private final CaseRecordRepository cases;
 public InvestigationController(InvestigationDraftRepository d,UserAccountRepository u,CaseRecordRepository c){drafts=d;users=u;cases=c;}

 @GetMapping public String list(Principal p,Model m){
  m.addAttribute("drafts",drafts.findAllByAuthor_EmailIgnoreCaseOrderByUpdatedAtDesc(p.getName())); return "investigations";
 }
 @GetMapping("/new") public String createPage(@RequestParam Long caseId,Model m){
  CaseRecord c=publicCase(caseId); InvestigationForm f=new InvestigationForm(); f.setCaseId(c.getId());
  m.addAttribute("form",f); m.addAttribute("caseRecord",c); m.addAttribute("pageTitle","Start an Investigation"); return "investigation-editor";
 }
 @PostMapping public String create(@Valid @ModelAttribute("form") InvestigationForm f,BindingResult b,Principal p,Model m){
  CaseRecord c=(f.getCaseId()==null)?null:cases.findById(f.getCaseId()).filter(CaseRecord::isPublished).orElse(null);
  if(c==null)b.rejectValue("caseId","case.invalid","The selected public case could not be found.");
  if(b.hasErrors()){m.addAttribute("caseRecord",c);m.addAttribute("pageTitle","Start an Investigation");return "investigation-editor";}
  InvestigationDraft d=new InvestigationDraft();d.setCaseRecord(c);d.setAuthor(currentUser(p));copy(f,d);d.setStatus("DRAFT");drafts.save(d);return "redirect:/investigations";
 }
 @GetMapping("/{id}") public String view(@PathVariable Long id,Principal p,Model m){
  m.addAttribute("draft",owned(id,p)); return "investigation-view";
 }
 @GetMapping("/{id}/edit") public String editPage(@PathVariable Long id,Principal p,Model m){
  InvestigationDraft d=owned(id,p);if(!"DRAFT".equals(d.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  m.addAttribute("form",form(d));m.addAttribute("caseRecord",d.getCaseRecord());m.addAttribute("draftId",d.getId());m.addAttribute("pageTitle","Edit Investigation Draft");return "investigation-editor";
 }
 @PostMapping("/{id}/edit") public String update(@PathVariable Long id,@Valid @ModelAttribute("form") InvestigationForm f,BindingResult b,Principal p,Model m){
  InvestigationDraft d=owned(id,p);if(!"DRAFT".equals(d.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(b.hasErrors()){m.addAttribute("caseRecord",d.getCaseRecord());m.addAttribute("draftId",d.getId());m.addAttribute("pageTitle","Edit Investigation Draft");return "investigation-editor";}
  copy(f,d);drafts.save(d);return "redirect:/investigations";
 }
 @PostMapping("/{id}/submit") public String submit(@PathVariable Long id,Principal p,RedirectAttributes ra){
  InvestigationDraft d=owned(id,p);if(!"DRAFT".equals(d.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT);
  d.setStatus("PENDING_REVIEW");drafts.save(d);ra.addFlashAttribute("successMessage","Submitted for review. This investigation remains private until approved.");return "redirect:/investigations";
 }
 private UserAccount currentUser(Principal p){return users.findByEmailIgnoreCase(p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 private CaseRecord publicCase(Long id){return cases.findById(id).filter(CaseRecord::isPublished).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private InvestigationDraft owned(Long id,Principal p){return drafts.findByIdAndAuthor_EmailIgnoreCase(id,p.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private void copy(InvestigationForm f,InvestigationDraft d){d.setTitle(f.getTitle().trim());d.setFindings(f.getFindings().trim());d.setSourceNotes(f.getSourceNotes()==null?null:f.getSourceNotes().trim());}
 private InvestigationForm form(InvestigationDraft d){InvestigationForm f=new InvestigationForm();f.setCaseId(d.getCaseRecord().getId());f.setTitle(d.getTitle());f.setFindings(d.getFindings());f.setSourceNotes(d.getSourceNotes());return f;}
}