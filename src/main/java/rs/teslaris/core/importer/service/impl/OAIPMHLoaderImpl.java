package rs.teslaris.core.importer.service.impl;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.model.converter.event.EventConverter;
import rs.teslaris.core.importer.model.converter.institution.OrganisationUnitConverter;
import rs.teslaris.core.importer.model.converter.person.PersonConverter;
import rs.teslaris.core.importer.model.converter.publication.JournalConverter;
import rs.teslaris.core.importer.model.converter.publication.JournalPublicationConverter;
import rs.teslaris.core.importer.model.converter.publication.PatentConverter;
import rs.teslaris.core.importer.model.converter.publication.ProceedingsConverter;
import rs.teslaris.core.importer.model.converter.publication.ProceedingsPublicationConverter;
import rs.teslaris.core.importer.model.converter.publication.ProductConverter;
import rs.teslaris.core.importer.model.event.Event;
import rs.teslaris.core.importer.model.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.patent.Patent;
import rs.teslaris.core.importer.model.person.Person;
import rs.teslaris.core.importer.model.product.Product;
import rs.teslaris.core.importer.model.publication.Publication;
import rs.teslaris.core.importer.service.interfaces.OAIPMHLoader;
import rs.teslaris.core.importer.utility.CreatorMethod;
import rs.teslaris.core.importer.utility.OAIPMHDataSet;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.importer.utility.ProgressReport;
import rs.teslaris.core.importer.utility.RecordConverter;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@RequiredArgsConstructor
public class OAIPMHLoaderImpl implements OAIPMHLoader {

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitConverter organisationUnitConverter;

    private final PersonConverter personConverter;

    private final EventConverter eventConverter;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final InvolvementService involvementService;

    private final ConferenceService conferenceService;

    private final JournalPublicationService journalPublicationService;

    private final JournalPublicationConverter journalPublicationConverter;

    private final ProceedingsPublicationService proceedingsPublicationService;

    private final ProceedingsPublicationConverter proceedingsPublicationConverter;

    private final ProceedingsConverter proceedingsConverter;

    private final JournalConverter journalConverter;

    private final JournalService journalService;

    private final ProceedingsService proceedingsService;

    private final PatentConverter patentConverter;

    private final PatentService patentService;

    private final SoftwareService softwareService;

    private final ProductConverter productConverter;


    public <R> R loadRecordsWizard(OAIPMHDataSet requestDataSet) {
        Query query = new Query();

        var progressReport = getProgressReport(requestDataSet);
        if (progressReport != null) {
            query.addCriteria(Criteria.where("_id").gt(progressReport.getLastLoadedId()));
        } else {
            query.addCriteria(Criteria.where("_id").gt(""));
        }
        query.limit(1);

        switch (requestDataSet) {
            case PERSONS:
                var person = mongoTemplate.findOne(query, Person.class);
                if (Objects.nonNull(person)) {
                    updateProgressReport(requestDataSet, person.getId());
                    return (R) personConverter.toDTO(person);
                }
                break;
            case EVENTS:
                var event = mongoTemplate.findOne(query, Event.class);
                if (Objects.nonNull(event)) {
                    updateProgressReport(requestDataSet, event.getId());
                    return (R) eventConverter.toDTO(event);
                }
                break;
        }

        return null;
    }

    @Nullable
    private ProgressReport getProgressReport(OAIPMHDataSet requestDataSet) {
        Query query = new Query();
        query.addCriteria(
            Criteria.where("dataset").is(requestDataSet.name()));
        return mongoTemplate.findOne(query, ProgressReport.class);
    }

    private void updateProgressReport(OAIPMHDataSet requestDataSet, String lastLoadedId) {
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(
            Criteria.where("dataset").is(requestDataSet));
        mongoTemplate.remove(deleteQuery, ProgressReport.class);

        mongoTemplate.save(new ProgressReport(lastLoadedId, requestDataSet));
    }

    @Override
    public void loadRecordsAuto(OAIPMHDataSet requestDataSet, boolean performIndex) {
        int batchSize = 10;
        int page = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var pageable = PageRequest.of(page, batchSize);
            var query = new Query().with(pageable);

            switch (requestDataSet) {
                case ORGANISATION_UNITS:
                    hasNextPage = loadBatch(OrgUnit.class, organisationUnitConverter,
                        organisationUnitService::createOrganisationUnit, query, performIndex,
                        batchSize);
                    break;
                case PERSONS:
                    hasNextPage = loadBatch(Person.class, personConverter,
                        personService::createPersonWithBasicInfo, query, performIndex, batchSize);
                    break;
                case EVENTS:
                    hasNextPage = loadBatch(Event.class, eventConverter,
                        conferenceService::createConference, query, performIndex, batchSize);
                    break;
                case PUBLICATIONS:
                    var criteria = new Criteria().orOperator(
                        Criteria.where("type").regex("c_f744$"),
                        Criteria.where("type").regex("c_0640$")
                    );
                    var batch = mongoTemplate.find(query.addCriteria(criteria), Publication.class);
                    batch.forEach(record -> {
                        if (record.getType()
                            .endsWith("c_f744")) { // COAR type: conference proceedings
                            var creationDTO = proceedingsConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                proceedingsService.createProceedings(creationDTO, performIndex);
                            }
                        } else if (record.getType().endsWith("c_0640")) { // COAR type: journal
                            var creationDTO = journalConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                journalService.createJournal(creationDTO, performIndex);
                            }
                        }
                        // TODO: what is conference output (c_c94f) ???
                    });
                    hasNextPage = batch.size() == batchSize;
                    break;
                case PATENTS:
                    hasNextPage = loadBatch(Patent.class, patentConverter,
                        patentService::createPatent, query, performIndex, batchSize);
                    break;
                case PRODUCTS:
                    hasNextPage = loadBatch(Product.class, productConverter,
                        softwareService::createSoftware, query, performIndex, batchSize);
                    break;
            }
            page++;
        }

        handleDataRelations(requestDataSet, performIndex);
    }

    private <T, D, R> boolean loadBatch(Class<T> entityClass, RecordConverter<T, D> converter,
                                        CreatorMethod<D, R> creatorMethod, Query query,
                                        boolean performIndex, int batchSize) {
        List<T> batch = mongoTemplate.find(query, entityClass);
        batch.forEach(record -> {
            D creationDTO = converter.toDTO(record);
            creatorMethod.apply(creationDTO, performIndex);
        });
        return batch.size() == batchSize;
    }

    private void handleDataRelations(OAIPMHDataSet requestDataSet, boolean performIndex) {
        int batchSize = 10;
        int page = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var pageable = PageRequest.of(page, batchSize);
            var query = new Query().with(pageable);

            switch (requestDataSet) {
                case ORGANISATION_UNITS:
                    List<OrgUnit> orgUnitBatch = mongoTemplate.find(query, OrgUnit.class);
                    orgUnitBatch.forEach((orgUnit) -> {
                        var creationDTO = organisationUnitConverter.toRelationDTO(orgUnit);
                        creationDTO.ifPresent(
                            organisationUnitService::createOrganisationUnitsRelation);
                    });
                    page++;
                    hasNextPage = orgUnitBatch.size() == batchSize;
                    break;
                case PERSONS:
                    List<Person> personBatch = mongoTemplate.find(query, Person.class);
                    personBatch.forEach((person) -> {
                        var savedPerson = personService.findPersonByOldId(
                            OAIPMHParseUtility.parseBISISID(person.getId()));
                        if (Objects.isNull(person.getAffiliation()) &&
                            Objects.nonNull(savedPerson)) {
                            return;
                        }
                        person.getAffiliation().getOrgUnits().forEach(((affiliation) -> {
                            var creationDTO =
                                personConverter.toPersonEmployment(affiliation);
                            creationDTO.ifPresent(employmentDTO -> involvementService.addEmployment(
                                savedPerson.getId(), employmentDTO));
                        }));
                    });
                    page++;
                    hasNextPage = personBatch.size() == batchSize;
                    break;
                case PUBLICATIONS:
                    var criteria = new Criteria().orOperator(
                        Criteria.where("type").regex("c_2df8fbb1$"),
                        Criteria.where("type").regex("c_5794$")
                    );
                    var publicationBatch =
                        mongoTemplate.find(query.addCriteria(criteria), Publication.class);
                    publicationBatch.forEach(record -> {
                        if (record.getType()
                            .endsWith("c_2df8fbb1")) { // COAR type: research article
                            var creationDTO = journalPublicationConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                journalPublicationService.createJournalPublication(creationDTO,
                                    performIndex);
                            }
                        } else if (record.getType()
                            .endsWith("c_5794")) { // COAR type: conference paper
                            var creationDTO = proceedingsPublicationConverter.toDTO(record);
                            if (Objects.nonNull(creationDTO)) {
                                proceedingsPublicationService.createProceedingsPublication(
                                    creationDTO,
                                    performIndex);
                            }
                        }
                    });
                    page++;
                    hasNextPage = publicationBatch.size() == batchSize;
                    break;
                default:
                    hasNextPage = false;
                    break;
            }
        }
    }
}
