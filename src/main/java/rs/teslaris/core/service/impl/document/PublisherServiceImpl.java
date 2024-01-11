package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.indexrepository.PublisherIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class PublisherServiceImpl extends JPAServiceImpl<Publisher> implements PublisherService {

    private final PublisherRepository publisherRepository;

    private final PublisherIndexRepository publisherIndexRepository;

    private final MultilingualContentService multilingualContentService;

    private final EmailUtil emailUtil;

    private final SearchService<PublisherIndex> searchService;


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
    public Page<PublisherIndex> searchPublishers(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable,
            PublisherIndex.class, "publisher");
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

        reindexPublisher(publisher);

        return savedPublisher;
    }

    @Override
    public void updatePublisher(PublisherDTO publisherDTO, Integer publisherId) {
        var publisherToUpdate = findPublisherById(publisherId);

        setCommonFields(publisherToUpdate, publisherDTO);

        this.save(publisherToUpdate);

        reindexPublisher(publisherToUpdate);
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

        var index = publisherIndexRepository.findByDatabaseId(publisherId);
        index.ifPresent(publisherIndexRepository::delete);
    }

    private void setCommonFields(Publisher publisher, PublisherDTO publisherDTO) {
        publisher.setName(
            multilingualContentService.getMultilingualContent(publisherDTO.getName()));
        publisher.setPlace(
            multilingualContentService.getMultilingualContent(publisherDTO.getPlace()));
        publisher.setState(
            multilingualContentService.getMultilingualContent(publisherDTO.getState()));
    }

    private void reindexPublisher(Publisher publisher) {
        PublisherIndex index = new PublisherIndex();
        var indexOptional = publisherIndexRepository.findByDatabaseId(publisher.getId());
        if (indexOptional.isPresent()) {
            index = indexOptional.get();
        } else {
            index.setDatabaseId(publisher.getId());
        }

        indexCommonFields(publisher, index);
        publisherIndexRepository.save(index);
    }

    private void indexCommonFields(Publisher publisher, PublisherIndex publisherIndex) {
        indexMultilingualContent(publisherIndex, publisher, Publisher::getName,
            PublisherIndex::setNameSr, PublisherIndex::setNameOther);
        indexMultilingualContent(publisherIndex, publisher, Publisher::getPlace,
            PublisherIndex::setPlaceSr, PublisherIndex::setPlaceOther);
        indexMultilingualContent(publisherIndex, publisher, Publisher::getState,
            PublisherIndex::setStateSr, PublisherIndex::setStateOther);
    }

    private void indexMultilingualContent(PublisherIndex index, Publisher publisher,
                                          Function<Publisher, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<PublisherIndex, String> srSetter,
                                          BiConsumer<PublisherIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(publisher);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();
        contentList.forEach(content -> {
            if (content.getLanguage().getLanguageTag().equals(LanguageAbbreviations.SERBIAN)) {
                srContent.append(content.getContent()).append(" | ");
            } else {
                otherContent.append(content.getContent()).append(" | ");
            }
        });

        StringUtil.removeTrailingPipeDelimiter(srContent, otherContent);
        srSetter.accept(index,
            srContent.length() > 0 ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            otherContent.length() > 0 ? otherContent.toString() : srContent.toString());
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.wildcard(
                    m -> m.field("name_sr").value(token).caseInsensitive(true)));
                b.should(sb -> sb.wildcard(
                    m -> m.field("name_other").value(token).caseInsensitive(true)));
                b.should(sb -> sb.match(
                    m -> m.field("place_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("place_other").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("state_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("state_other").query(token)));
            });
            return b;
        })))._toQuery();
    }

    @Override
    protected JpaRepository<Publisher, Integer> getEntityRepository() {
        return publisherRepository;
    }
}
