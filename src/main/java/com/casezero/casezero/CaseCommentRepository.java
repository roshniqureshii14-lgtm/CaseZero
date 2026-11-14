package com.casezero.casezero;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseCommentRepository extends JpaRepository<CaseComment, Long> {
    List<CaseComment> findByCaseRecord_IdOrderByCreatedAtAsc(Long caseId);
}
