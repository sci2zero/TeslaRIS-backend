package rs.teslaris.core.service.impl.document;

import java.util.HashSet;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
@Transactional
public class PublisherServiceImpl extends JPAServiceImpl<Publisher> implements PublisherService {

    private final PublisherRepository publisherRepository;

    private final MultilingualContentService multilingualContentService;

    private final EmailUtil emailUtil;


    @Override
    public Page<PublisherDTO> readAllPublishers(Pageable pageable) {
        return this.findAll(pageable).map(p -> new PublisherDTO(p.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(p.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(p.getPlace()),
            MultilingualContentConverter.getMultilingualContentDTO(p.getState())));
    }

    @Override
    public Publisher findPublisherById(Integer publisherId) {
        return this.findOne(publisherId);
    }

    @Override
    public Publisher createPublisher(PublisherDTO publisherDTO) {
        var publisher = new Publisher();

        setCommonFields(publisher, publisherDTO);

        return this.save(publisher);
    }

    @Override
    public Publisher createPublisher(PublisherBasicAdditionDTO publisherDTO) {
        var publisher = new Publisher();

        publisher.setPlace(new HashSet<>());

        publisher.setName(
            multilingualContentService.getMultilingualContent(publisherDTO.getName()));
        publisher.setState(
            multilingualContentService.getMultilingualContent(publisherDTO.getState()));

        var savedPublisher = this.save(publisher);

        emailUtil.notifyInstitutionalEditor(savedPublisher.getId(), "publisher");

        return savedPublisher;
    }

    @Override
    public void updatePublisher(PublisherDTO publisherDTO, Integer publisherId) {
        var publisherToUpdate = findPublisherById(publisherId);

        setCommonFields(publisherToUpdate, publisherDTO);

        this.save(publisherToUpdate);
    }

    @Override
    public void deletePublisher(Integer publisherId) {

        if (publisherRepository.hasPublishedDataset(publisherId) ||
            publisherRepository.hasPublishedPatent(publisherId) ||
            publisherRepository.hasPublishedProceedings(publisherId) ||
            publisherRepository.hasPublishedSoftware(publisherId) ||
            publisherRepository.hasPublishedThesis(publisherId)) {
            throw new PublisherReferenceConstraintViolationException(
                "Publisher with id " + publisherId + " is already in use.");
        }

        this.delete(publisherId);
    }

    private void setCommonFields(Publisher publisher, PublisherDTO publisherDTO) {
        publisher.setName(
            multilingualContentService.getMultilingualContent(publisherDTO.getName()));
        publisher.setPlace(
            multilingualContentService.getMultilingualContent(publisherDTO.getPlace()));
        publisher.setState(
            multilingualContentService.getMultilingualContent(publisherDTO.getState()));
    }

    @Override
    protected JpaRepository<Publisher, Integer> getEntityRepository() {
        return publisherRepository;
    }
}
