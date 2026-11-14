package com.casezero.casezero;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="investigation_drafts")
public class InvestigationDraft {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="case_id",nullable=false) private CaseRecord caseRecord;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="author_id",nullable=false) private UserAccount author;
 @Column(nullable=false,length=150) private String title;
 @Column(nullable=false,length=6000) private String findings;
 @Column(length=3000) private String sourceNotes;
 @Column(nullable=false,length=30) private String status="DRAFT";
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
 public InvestigationDraft() {}
 @PrePersist public void beforeInsert(){ LocalDateTime n=LocalDateTime.now(); if(createdAt==null)createdAt=n; if(updatedAt==null)updatedAt=n; }
 @PreUpdate public void beforeUpdate(){updatedAt=LocalDateTime.now();}
 public Long getId(){return id;} public CaseRecord getCaseRecord(){return caseRecord;} public UserAccount getAuthor(){return author;}
 public String getTitle(){return title;} public String getFindings(){return findings;} public String getSourceNotes(){return sourceNotes;}
 public String getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
 public void setCaseRecord(CaseRecord v){caseRecord=v;} public void setAuthor(UserAccount v){author=v;}
 public void setTitle(String v){title=v;} public void setFindings(String v){findings=v;} public void setSourceNotes(String v){sourceNotes=v;} public void setStatus(String v){status=v;}
}