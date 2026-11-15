package com.casezero.casezero;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_comments")
public class CaseComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseRecord caseRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 1500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public CaseComment() {
    }

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public CaseRecord getCaseRecord() { return caseRecord; }
    public UserAccount getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCaseRecord(CaseRecord caseRecord) { this.caseRecord = caseRecord; }
    public void setAuthor(UserAccount author) { this.author = author; }
    public void setContent(String content) { this.content = content; }
}
