package com.casezero.casezero;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_research_reports")
public class CaseResearchReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;
    @Column(name = "source_name", length = 200)
    private String sourceName;
    @Column(name = "source_url", nullable = false, length = 1000, unique = true)
    private String sourceUrl;
    @Column(name = "published_at", length = 80)
    private String publishedAt;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 30)
    private String status = "PENDING";
    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public CaseResearchReport() {}

    public CaseResearchReport(ResearchArticle article) {
        this.title = article.title().length() > 500 ? article.title().substring(0, 500) : article.title();
        this.sourceName = article.source();
        this.sourceUrl = article.url();
        this.publishedAt = article.publishedAt();
        this.description = article.description();
        this.status = "PENDING";
        this.collectedAt = LocalDateTime.now();
    }

    @PrePersist
    public void setCollectedAt() {
        if (collectedAt == null) collectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public String getPublishedAt() { return publishedAt; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setStatus(String status) { this.status = status; }
}