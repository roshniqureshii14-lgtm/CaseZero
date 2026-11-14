package com.casezero.casezero;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InvestigationDraftRepository extends JpaRepository<InvestigationDraft,Long>{
 List<InvestigationDraft> findAllByAuthor_EmailIgnoreCaseOrderByUpdatedAtDesc(String email);
 Optional<InvestigationDraft> findByIdAndAuthor_EmailIgnoreCase(Long id,String email);
}