package rs.teslaris.core.service.impl.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherInUseException;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    public Publisher findPublisherById(Integer publisherId) {
        return publisherRepository.findById(publisherId)
            .orElseThrow(() -> new NotFoundException("Publisher with given ID does not exist."));
    }

    @Override
    public Publisher createPublisher(PublisherDTO publisherDTO) {
        var publisher = new Publisher();

        setCommonFields(publisher, publisherDTO);

        return publisherRepository.save(publisher);
    }

    @Override
    public void updatePublisher(PublisherDTO publisherDTO, Integer publisherId) {
        var publisherToUpdate = findPublisherById(publisherId);

        setCommonFields(publisherToUpdate, publisherDTO);

        publisherRepository.save(publisherToUpdate);
    }

    @Override
    public void deletePublisher(Integer publisherId) {
        var publisherToDelete = findPublisherById(publisherId);

        if (publisherRepository.publishedDataset(publisherId) ||
            publisherRepository.publishedPatent(publisherId) ||
            publisherRepository.publishedProceedings(publisherId) ||
            publisherRepository.publishedSoftware(publisherId) ||
            publisherRepository.publishedThesis(publisherId)) {
            throw new PublisherInUseException(
                "Publisher with id " + publisherId + " is already in use.");
        }

        publisherRepository.delete(publisherToDelete);
    }

    private void setCommonFields(Publisher publisher, PublisherDTO publisherDTO) {
        publisher.setName(
            multilingualContentService.getMultilingualContent(publisherDTO.getName()));
        publisher.setPlace(
            multilingualContentService.getMultilingualContent(publisherDTO.getPlace()));
        publisher.setState(
            multilingualContentService.getMultilingualContent(publisherDTO.getState()));
    }
}
