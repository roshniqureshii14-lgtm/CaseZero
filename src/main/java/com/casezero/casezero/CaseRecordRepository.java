package com.casezero.casezero;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRecordRepository
        extends JpaRepository<CaseRecord, Long> {

    List<CaseRecord> findAllByPublishedTrueOrderByCreatedAtDesc();

    boolean existsByTitleIgnoreCase(String title);

    boolean existsBySourceUrl(String sourceUrl);
}
