package rs.teslaris.core.exporter.service.impl;

import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.exporter.model.common.ExportEvent;
import rs.teslaris.core.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.core.exporter.model.common.ExportPerson;
import rs.teslaris.core.exporter.model.converter.ExportEventConverter;
import rs.teslaris.core.exporter.model.converter.ExportOrganisationUnitConverter;
import rs.teslaris.core.exporter.model.converter.ExportPersonConverter;
import rs.teslaris.core.exporter.service.interfaces.CommonExportService;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CommonExportServiceImpl implements CommonExportService {

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final PersonRepository personRepository;

    private final ConferenceRepository conferenceRepository;


    @Override
    @Scheduled(cron = "${export-to-common.schedule.ou}")
    public void exportOrganisationUnitsToCommonModel() {
        exportEntities(
            organisationUnitRepository::findAllModifiedInLast24Hours,
            ExportOrganisationUnitConverter::toCommonExportModel,
            ExportOrganisationUnit.class,
            OrganisationUnit::getId
        );
    }

    @Override
    @Scheduled(cron = "${export-to-common.schedule.person}")
    public void exportPersonsToCommonModel() {
        exportEntities(
            personRepository::findAllModifiedInLast24Hours,
            ExportPersonConverter::toCommonExportModel,
            ExportPerson.class,
            Person::getId
        );
    }

    @Override
    @Scheduled(cron = "${export-to-common.schedule.event}")
    public void exportConferencesToCommonModel() {
        exportEntities(
            conferenceRepository::findAllModifiedInLast24Hours,
            ExportEventConverter::toCommonExportModel,
            ExportEvent.class,
            Conference::getId
        );
    }

    private <T, E> void exportEntities(
        Function<Pageable, Page<T>> repositoryFunction,
        Function<T, E> converter,
        Class<E> exportClass,
        Function<T, Integer> idGetter
    ) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<T> chunk =
                repositoryFunction.apply(PageRequest.of(pageNumber, chunkSize)).getContent();
            chunk.forEach(entity -> {
                var query = new Query();
                query.addCriteria(Criteria.where("database_id").is(idGetter.apply(entity)));
                query.limit(1);

                var exportEntry = converter.apply(entity);

                mongoTemplate.remove(query, exportClass);
                mongoTemplate.save(exportEntry);
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }
}
