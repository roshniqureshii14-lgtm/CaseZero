package com.casezero.casezero;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestigationAttachmentRepository
        extends JpaRepository<InvestigationAttachment, Long> {

    List<InvestigationAttachment> findAllByDraft_IdOrderByUploadedAtDesc(Long draftId);

    List<InvestigationAttachment> findAllByDraft_Author_EmailIgnoreCaseOrderByUploadedAtDesc(String email);

    Optional<InvestigationAttachment> findByIdAndDraft_Author_EmailIgnoreCase(Long id, String email);
}
