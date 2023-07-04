package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.InformativeAttachment;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface InformativeAttachmentRepository
    extends JPASoftDeleteRepository<InformativeAttachment> {
}
