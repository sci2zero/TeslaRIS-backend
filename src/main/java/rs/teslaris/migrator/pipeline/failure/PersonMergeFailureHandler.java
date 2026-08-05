package rs.teslaris.migrator.pipeline.failure;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.FailureResolution;

/**
 * Port of the identifier-resolver map in {@code OAIPMHLoaderImpl.loadBatch}.
 * <p>
 * When a person already exists under one of their identifiers, the existing person is adopted as the
 * migration target instead of failing the item.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersonMergeFailureHandler implements FailureHandler<ImportPersonDTO> {

    private static final Map<String, Function<ImportPersonDTO, String>> IDENTIFIER_RESOLVERS =
        Map.of(
            "scopusAuthorIdExistsError", ImportPersonDTO::getScopusAuthorId,
            "orcidIdExistsError", ImportPersonDTO::getOrcid,
            "apvntExistsError", ImportPersonDTO::getApvnt
        );

    private final PersonService personService;


    @Override
    public FailureResolution onCreateFailed(
        rs.teslaris.migrator.pipeline.MigrationItem<ImportPersonDTO> item, Exception exception,
        int attempt) {

        var message = Objects.toString(exception.getMessage(), "");

        return Optional.ofNullable(IDENTIFIER_RESOLVERS.get(message))
            .map(resolver -> resolver.apply(item.dto()))
            .filter(identifier -> !identifier.isBlank())
            .flatMap(personService::findPersonByIdentifier)
            .map(person -> {
                log.info("Merged person '{}' into existing person {}.", item.sourceKey(),
                    person.getId());
                return FailureResolution.resolved(person.getId());
            })
            .orElseGet(FailureResolution::skip);
    }
}
