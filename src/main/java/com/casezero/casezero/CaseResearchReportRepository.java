package com.casezero.casezero;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseResearchReportRepository extends JpaRepository<CaseResearchReport, Long> {
    boolean existsBySourceUrl(String sourceUrl);
    List<CaseResearchReport> findAllByStatusOrderByCollectedAtDesc(String status);
}
