package rs.teslaris.assessment.service.impl.indicator.worker;

import com.google.common.util.concurrent.AtomicDouble;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.assessment.repository.indicator.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.service.impl.indicator.DocumentMetricResult;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.functional.FunctionalUtil;

@Service
@RequiredArgsConstructor
public class ExternalIndicatorWorker {

    private final DocumentRepository documentRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final PersonService personService;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    private final PersonIndexRepository personIndexRepository;

    private final int PROCESS_BATCH_SIZE = 100;


    @Transactional
    public void setOpenAccessPolicyInfo(String doi, DocumentMetricResult result) {
        if (result.value() == 0) {
            return;
        }

        documentRepository.setIsOpenAccess(doi);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void performInstitutionDeduction(List<EntityIndicatorSource> entityIndicatorSources,
                                            OrganisationUnitIndex institution,
                                            Indicator totalCitationsIndicator,
                                            Indicator totalOutputIndicator,
                                            Set<OrganisationUnitIndicator> indicatorsToSave
    ) {
        entityIndicatorSources.forEach(entityIndicatorSource -> {
            var totalCitationCount = new AtomicDouble(0);
            var totalPublicationsCount = new AtomicDouble(0);

            FunctionalUtil.forEachChunked(
                PageRequest.of(0, PROCESS_BATCH_SIZE,
                    Sort.by(Sort.Direction.ASC, "databaseId")),
                page -> personService.findPeopleForOrganisationUnit(
                    institution.getDatabaseId(), List.of("*"),
                    page, false),
                people -> people.forEach(person -> {
                    if (Objects.nonNull(totalCitationsIndicator)) {
                        personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                            totalCitationsIndicator.getCode(), entityIndicatorSource,
                            null, person.getDatabaseId()
                        ).ifPresent(
                            ind -> totalCitationCount.addAndGet(ind.getNumericValue()));
                    }

                    if (Objects.nonNull(totalOutputIndicator)) {
                        personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                            totalOutputIndicator.getCode(), entityIndicatorSource, null,
                            person.getDatabaseId()
                        ).ifPresent(
                            ind -> totalPublicationsCount.addAndGet(ind.getNumericValue()));
                    }
                })
            );

            var organisationUnit =
                organisationUnitService.findOne(institution.getDatabaseId());

            if (Objects.nonNull(totalCitationsIndicator) && totalCitationCount.get() > 0) {
                var newTotalCitationsIndicator =
                    organisationUnitIndicatorRepository.findIndicatorForCodeAndSourceAndOrganisationUnitId(
                            totalCitationsIndicator.getCode(), entityIndicatorSource,
                            institution.getDatabaseId())
                        .orElse(new OrganisationUnitIndicator());

                newTotalCitationsIndicator.setOrganisationUnit(organisationUnit);
                newTotalCitationsIndicator.setNumericValue(
                    totalCitationCount.doubleValue());
                newTotalCitationsIndicator.setSource(entityIndicatorSource);
                newTotalCitationsIndicator.setIndicator(totalCitationsIndicator);
                newTotalCitationsIndicator.setToDate(LocalDate.now());

                indicatorsToSave.add(newTotalCitationsIndicator);
            }

            if (Objects.nonNull(totalOutputIndicator) && totalPublicationsCount.get() > 0) {
                var newTotalOutputIndicator =
                    organisationUnitIndicatorRepository.findIndicatorForCodeAndSourceAndOrganisationUnitId(
                            totalOutputIndicator.getCode(), entityIndicatorSource,
                            institution.getDatabaseId())
                        .orElse(new OrganisationUnitIndicator());

                newTotalOutputIndicator.setOrganisationUnit(organisationUnit);
                newTotalOutputIndicator.setNumericValue(
                    totalPublicationsCount.doubleValue());
                newTotalOutputIndicator.setSource(entityIndicatorSource);
                newTotalOutputIndicator.setIndicator(totalOutputIndicator);
                newTotalOutputIndicator.setToDate(LocalDate.now());

                indicatorsToSave.add(newTotalOutputIndicator);
            }
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void persistPersonCitationIndicators(Person person, Map<String, Integer> counts,
                                                Integer totalOutputCount,
                                                List<Integer> citations,
                                                Indicator totalIndicator,
                                                Indicator yearlyIndicator,
                                                Indicator totalOutputIndicator,
                                                Indicator hIndexIndicator,
                                                EntityIndicatorSource source) {
        var index = personIndexRepository.findByDatabaseId(person.getId());
        if (index.isEmpty()) {
            return;
        }
        var shouldUpdateIndex = source.equals(EntityIndicatorSource.OPEN_ALEX);

        var personIndicators = new HashSet<PersonIndicator>();

        counts.forEach((key, value) -> {
            if (value == 0) {
                return;
            }

            PersonIndicator newCitationCountIndicator;

            if (key.equals("TOTAL")) {
                if (Objects.isNull(totalIndicator)) {
                    return;
                }

                newCitationCountIndicator =
                    personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                            totalIndicator.getCode(), source, null, person.getId())
                        .orElse(new PersonIndicator());
                newCitationCountIndicator.setIndicator(totalIndicator);
                newCitationCountIndicator.setToDate(LocalDate.now());

                if (shouldUpdateIndex) {
                    index.get().setTotalCitations((long) value);
                }
            } else {
                if (Objects.isNull(yearlyIndicator)) {
                    return;
                }

                int year = Integer.parseInt(key);
                var fromDate = LocalDate.of(year, 1, 1);

                newCitationCountIndicator =
                    personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                            yearlyIndicator.getCode(), source, year, person.getId())
                        .orElse(new PersonIndicator());

                newCitationCountIndicator.setIndicator(yearlyIndicator);
                newCitationCountIndicator.setFromDate(fromDate);
                newCitationCountIndicator.setToDate(
                    year == LocalDate.now().getYear() ? LocalDate.now() : LocalDate.of(year, 12, 31)
                );

                if (shouldUpdateIndex) {
                    index.get().getCitationsByYear().put(year, value);
                }
            }

            newCitationCountIndicator.setPerson(person);
            newCitationCountIndicator.setNumericValue((double) value);
            newCitationCountIndicator.setSource(source);

            personIndicators.add(newCitationCountIndicator);
        });

        var hIndex = calculateHIndex(citations);
        if (Objects.nonNull(hIndexIndicator) && hIndex > 0) {
            var newHIndexIndicator =
                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                        hIndexIndicator.getCode(), source, null, person.getId())
                    .orElse(new PersonIndicator());

            newHIndexIndicator.setPerson(person);
            newHIndexIndicator.setNumericValue((double) hIndex);
            newHIndexIndicator.setSource(source);
            newHIndexIndicator.setIndicator(hIndexIndicator);
            newHIndexIndicator.setToDate(LocalDate.now());
            personIndicators.add(newHIndexIndicator);

            if (shouldUpdateIndex) {
                index.get().setHIndex(hIndex);
            }
        }

        if (shouldUpdateIndex) {
            personIndexRepository.save(index.get());
        }

        if (Objects.nonNull(totalOutputIndicator)) {
            var newTotalOutputIndicator =
                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                        totalOutputIndicator.getCode(), source, null, person.getId())
                    .orElse(new PersonIndicator());

            newTotalOutputIndicator.setPerson(person);
            newTotalOutputIndicator.setNumericValue((double) totalOutputCount);
            newTotalOutputIndicator.setSource(source);
            newTotalOutputIndicator.setIndicator(totalOutputIndicator);
            newTotalOutputIndicator.setToDate(LocalDate.now());
            personIndicators.add(newTotalOutputIndicator);
        }

        personIndicatorRepository.saveAll(personIndicators);
    }

    private int calculateHIndex(List<Integer> citations) {
        return (int) citations.stream()
            .sorted(Comparator.reverseOrder())
            .mapToInt(Integer::intValue)
            .takeWhile(new IntPredicate() {
                int index = 0;

                @Override
                public boolean test(int value) {
                    index++;
                    return value >= index;
                }
            })
            .count();
    }
}
