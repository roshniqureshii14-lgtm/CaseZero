
package com.casezero.casezero;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_records")
public class CaseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 120)
    private String location;

    @Column(nullable = false, length = 1200)
    private String summary;

    @Column(nullable = false, length = 30)
    private String status = "Open";

    @Column(length = 150)
    private String sourceName;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(nullable = false)
    private boolean published = true;

    private LocalDateTime createdAt;

    public CaseRecord() {
    }

    public CaseRecord(String title, String category,
                      String location, String summary,
                      String status, String sourceName,
                      String sourceUrl) {
        this.title = title;
        this.category = category;
        this.location = location;
        this.summary = summary;
        this.status = status;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
    }

    @PrePersist
    public void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public boolean isPublished() { return published; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setLocation(String location) { this.location = location; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setStatus(String status) { this.status = status; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setPublished(boolean published) { this.published = published; }
}
