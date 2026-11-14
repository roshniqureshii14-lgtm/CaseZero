package com.casezero.casezero;
import jakarta.validation.constraints.*;
public class InvestigationForm {
 @NotNull(message="Select a case.") private Long caseId;
 @NotBlank(message="Enter an investigation title.") @Size(max=150) private String title;
 @NotBlank(message="Add your findings or investigation notes.") @Size(max=6000) private String findings;
 @Size(max=3000) private String sourceNotes;
 public Long getCaseId(){return caseId;} public String getTitle(){return title;} public String getFindings(){return findings;} public String getSourceNotes(){return sourceNotes;}
 public void setCaseId(Long v){caseId=v;} public void setTitle(String v){title=v;} public void setFindings(String v){findings=v;} public void setSourceNotes(String v){sourceNotes=v;}
}