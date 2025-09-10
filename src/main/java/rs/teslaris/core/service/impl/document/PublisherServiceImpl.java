package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.PublisherConverter;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.indexrepository.PublisherIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;
import rs.teslaris.core.util.search.StringUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class PublisherServiceImpl extends JPAServiceImpl<Publisher> implements PublisherService {

    private final PublisherRepository publisherRepository;

    private final PublisherIndexRepository publisherIndexRepository;

    private final MultilingualContentService multilingualContentService;

    private final SearchService<PublisherIndex> searchService;

    private final CountryService countryService;

    private final IndexBulkUpdateService indexBulkUpdateService;


    @Override
    public Page<PublisherDTO> readAllPublishers(Pageable pageable) {
        return this.findAll(pageable).map(p -> new PublisherDTO(p.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(p.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(p.getPlace()),
            p.getCountry() != null ? p.getCountry().getId() : null));
    }

    @Override
    public PublisherDTO readPublisherById(Integer publisherId) {
        Publisher publisher;
        try {
            publisher = findOne(publisherId);
        } catch (NotFoundException e) {
            publisherIndexRepository.findByDatabaseId(publisherId)
                .ifPresent(publisherIndexRepository::delete);
            throw e;
        }

        return PublisherConverter.toDTO(publisher);
    }

    @Override
    public Page<PublisherIndex> searchPublishers(List<String> tokens, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens), pageable,
            PublisherIndex.class, "publisher");
    }

    @Override
    public Publisher createPublisher(PublisherDTO publisherDTO, Boolean index) {
        var publisher = new Publisher();

        setCommonFields(publisher, publisherDTO);
        var savedPublisher = this.save(publisher);

        if (index) {
            indexPublisher(savedPublisher, new PublisherIndex());
        }

        return savedPublisher;
    }

    @Override
    public Publisher createPublisher(PublisherBasicAdditionDTO publisherDTO) {
        var publisher = new Publisher();

        publisher.setName(
            multilingualContentService.getMultilingualContent(publisherDTO.getName()));

        if (Objects.nonNull(publisherDTO.getCountryId())) {
            publisher.setCountry(countryService.findOne(publisherDTO.getCountryId()));
        }

        var savedPublisher = this.save(publisher);

        indexPublisher(publisher, new PublisherIndex());

        return savedPublisher;
    }

    @Override
    public void editPublisher(Integer publisherId, PublisherDTO publisherDTO) {
        var publisherToUpdate = findOne(publisherId);

        publisherToUpdate.setCountry(null);
        setCommonFields(publisherToUpdate, publisherDTO);

        this.save(publisherToUpdate);

        var index = publisherIndexRepository.findByDatabaseId(publisherToUpdate.getId())
            .orElse(new PublisherIndex());
        indexPublisher(publisherToUpdate, index);
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

    @Override
    public void forceDeletePublisher(Integer publisherId) {
        publisherRepository.unbindDataset(publisherId);
        publisherRepository.unbindPatent(publisherId);
        publisherRepository.unbindProceedings(publisherId);
        publisherRepository.unbindSoftware(publisherId);
        publisherRepository.unbindThesis(publisherId);

        delete(publisherId);

        var index = publisherIndexRepository.findByDatabaseId(publisherId);
        index.ifPresent(publisherIndexRepository::delete);

        indexBulkUpdateService.removeIdFromRecord("document_publication", "publisher_id",
            publisherId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexPublishers() {
        publisherIndexRepository.deleteAll();
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;

        while (hasNextPage) {

            List<Publisher> chunk = findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach((publisher) -> indexPublisher(publisher, new PublisherIndex()));

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
        return null;
    }

    @Override
    public void indexPublisher(Publisher publisher) {
        indexPublisher(publisher, publisherIndexRepository.findByDatabaseId(publisher.getId())
            .orElse(new PublisherIndex()));
    }

    @Override
    public Publisher findRaw(Integer publisherId) {
        return publisherRepository.findRaw(publisherId)
            .orElseThrow(() -> new NotFoundException("Publisher with given ID does not exist."));
    }

    private void setCommonFields(Publisher publisher, PublisherDTO publisherDTO) {
        publisher.setName(
            multilingualContentService.getMultilingualContent(publisherDTO.getName()));
        publisher.setPlace(
            multilingualContentService.getMultilingualContent(publisherDTO.getPlace()));

        if (Objects.nonNull(publisherDTO.getCountryId())) {
            publisher.setCountry(countryService.findOne(publisherDTO.getCountryId()));
        }
    }

    private void indexPublisher(Publisher publisher, PublisherIndex index) {
        index.setDatabaseId(publisher.getId());

        indexCommonFields(publisher, index);
        publisherIndexRepository.save(index);
    }

    private void indexCommonFields(Publisher publisher, PublisherIndex publisherIndex) {
        indexMultilingualContent(publisherIndex, publisher, Publisher::getName,
            PublisherIndex::setNameSr, PublisherIndex::setNameOther);
        indexMultilingualContent(publisherIndex, publisher, Publisher::getPlace,
            PublisherIndex::setPlaceSr, PublisherIndex::setPlaceOther);

        publisherIndex.setStateSr("");
        publisherIndex.setStateOther("");
        if (Objects.nonNull(publisher.getCountry())) {
            indexMultilingualContent(publisherIndex, publisher,
                t -> publisher.getCountry().getName(),
                PublisherIndex::setStateSr, PublisherIndex::setStateOther);
        }

        publisherIndex.setNameSrSortable(publisherIndex.getNameSr());
        publisherIndex.setNameOtherSortable(publisherIndex.getNameOther());
        publisherIndex.setStateSrSortable(publisherIndex.getStateSr());
        publisherIndex.setStateOtherSortable(publisherIndex.getStateOther());
        publisherIndex.setPlaceSrSortable(publisherIndex.getPlaceSr());
        publisherIndex.setPlaceOtherSortable(publisherIndex.getPlaceOther());
    }

    private void indexMultilingualContent(PublisherIndex index, Publisher publisher,
                                          Function<Publisher, Set<MultiLingualContent>> contentExtractor,
                                          BiConsumer<PublisherIndex, String> srSetter,
                                          BiConsumer<PublisherIndex, String> otherSetter) {
        Set<MultiLingualContent> contentList = contentExtractor.apply(publisher);

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();
        multilingualContentService.buildLanguageStrings(srContent, otherContent, contentList, true);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        srSetter.accept(index,
            !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        otherSetter.accept(index,
            !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                if (token.startsWith("\\\"") && token.endsWith("\\\"")) {
                    b.must(mp ->
                        mp.bool(m -> {
                            {
                                m.should(sb -> sb.matchPhrase(
                                    mq -> mq.field("name_sr")
                                        .query(token.replace("\\\"", ""))));
                                m.should(sb -> sb.matchPhrase(
                                    mq -> mq.field("name_other")
                                        .query(token.replace("\\\"", ""))));
                            }
                            return m;
                        }));
                } else if (token.endsWith("\\*") || token.endsWith(".")) {
                    var wildcard = token.replace("\\*", "").replace(".", "");
                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("name_sr").value(
                                StringUtil.performSimpleLatinPreprocessing(wildcard) + "*")))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("name_other").value(wildcard + "*")))
                    ));
                } else {
                    var wildcard = token + "*";
                    b.should(mp -> mp.bool(m -> m
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("name_sr")
                                .value(StringUtil.performSimpleLatinPreprocessing(token) + "*")))
                        .should(sb -> sb.wildcard(
                            mq -> mq.field("name_other").value(wildcard)))
                        .should(sb -> sb.match(
                            mq -> mq.field("name_sr").query(wildcard)))
                        .should(sb -> sb.match(
                            mq -> mq.field("name_other").query(wildcard)))
                    ));
                }

                b.should(sb -> sb.match(
                    m -> m.field("place_sr").query(token).boost(0.7f)));
                b.should(sb -> sb.match(
                    m -> m.field("place_other").query(token).boost(0.7f)));
                b.should(sb -> sb.match(
                    m -> m.field("state_sr").query(token).boost(0.5f)));
                b.should(sb -> sb.match(
                    m -> m.field("state_other").query(token).boost(0.5f)));
            });
            return b;
        })))._toQuery();
    }

    @Override
    protected JpaRepository<Publisher, Integer> getEntityRepository() {
        return publisherRepository;
    }
}
