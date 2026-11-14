package com.casezero.casezero;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigation_attachments")
public class InvestigationAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private InvestigationDraft draft;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 700)
    private String storagePath;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public InvestigationAttachment() {}

    @PrePersist
    public void beforeInsert() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public InvestigationDraft getDraft() { return draft; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStoragePath() { return storagePath; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public void setDraft(InvestigationDraft draft) { this.draft = draft; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
