package rs.teslaris.thesislibrary.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.util.LocalizationUtil;
import rs.teslaris.assessment.util.ReportTemplateEngine;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.thesislibrary.dto.NotAddedToPromotionThesesRequestDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportCountsDTO;
import rs.teslaris.thesislibrary.dto.ThesisReportRequestDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryReportingService;

@Service
@Transactional
@RequiredArgsConstructor
@Traceable
public class ThesisLibraryReportingServiceImpl implements ThesisLibraryReportingService {

    public static final Object lock = new Object();

    private final ThesisRepository thesisRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final OrganisationUnitService organisationUnitService;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final LoadingCache<ThesisReportRequestDTO, List<ThesisReportCountsDTO>>
        thesisReportCache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(20, TimeUnit.SECONDS)  // Cache expiry period
            .build(new CacheLoader<>() {
                @NotNull
                @Override
                public List<ThesisReportCountsDTO> load(@NotNull ThesisReportRequestDTO request) {
                    return fetchThesisCounts(request);
                }
            });


    @Override
    public List<ThesisReportCountsDTO> createThesisCountsReport(ThesisReportRequestDTO request) {
        return thesisReportCache.getUnchecked(request);
    }

    private List<ThesisReportCountsDTO> fetchThesisCounts(ThesisReportRequestDTO request) {
        var facultyIds = new HashSet<Integer>();

        request.topLevelInstitutionIds().forEach(institutionId -> {
            facultyIds.add(institutionId);

            organisationUnitIndexRepository.findOrganisationUnitIndexesBySuperOUId(
                    institutionId, Pageable.unpaged()).stream()
                .forEach(index -> {
                    if (Objects.isNull(index.getAllowedThesisTypes()) ||
                        index.getAllowedThesisTypes().isEmpty()) {

                        // try to fetch one level below and end it there
                        facultyIds.addAll(
                            organisationUnitIndexRepository.findOrganisationUnitIndexesBySuperOUId(
                                    index.getDatabaseId(), Pageable.unpaged()).stream()
                                .filter(
                                    (subIdx -> Objects.nonNull(subIdx.getAllowedThesisTypes()) &&
                                        !subIdx.getAllowedThesisTypes().isEmpty()))
                                .map(OrganisationUnitIndex::getDatabaseId).toList());

                        return;
                    }

                    facultyIds.add(index.getDatabaseId());
                });
        });

        return facultyIds.stream()
            .map(institutionId -> {
                var institutionIds =
                    organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

                var defendedCount = thesisRepository.countDefendedThesesInPeriod(
                    request.fromDate(), request.toDate(), request.thesisTypes(), institutionIds);
                var notDefendedCount = thesisRepository.countNotDefendedThesesInPeriod(
                    request.fromDate(), request.toDate(), request.thesisTypes(), institutionIds);
//                var acceptedCount = thesisRepository.countAcceptedThesesInPeriod(
//                    request.fromDate(), request.toDate(), request.thesisTypes(), institutionIds);
//                var publicReviewCount = thesisRepository.countThesesWithPublicReviewInPeriod(
//                    request.fromDate(), request.toDate(), request.thesisTypes(), institutionIds);
                var openAccessCount =
                    thesisRepository.countPubliclyAvailableDefendedThesesThesesInPeriod(
                        request.fromDate(), request.toDate(), request.thesisTypes(),
                        institutionIds);
                var closedAccessCount =
                    thesisRepository.countClosedAccessDefendedThesesThesesInPeriod(
                        request.fromDate(), request.toDate(), request.thesisTypes(),
                        institutionIds);

                if (defendedCount == 0 && notDefendedCount == 0 && closedAccessCount == 0 &&
                    openAccessCount == 0) {
                    return null;
                }

                var institutionName = MultilingualContentConverter.getMultilingualContentDTO(
                    organisationUnitService.findOne(institutionId).getName());

                return new ThesisReportCountsDTO(institutionId, institutionName, defendedCount,
                    notDefendedCount, openAccessCount, closedAccessCount);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public Page<DocumentPublicationIndex> fetchDefendedThesesInPeriod(
        ThesisReportRequestDTO request, Pageable pageable) {
        var institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());

        return documentPublicationIndexRepository.fetchDefendedThesesInPeriod(request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().stream().map(ThesisType::name).toList(), pageable);
    }

    @Override
    public Page<DocumentPublicationIndex> fetchNotDefendedThesesInPeriod(
        ThesisReportRequestDTO request, Pageable pageable) {
        var institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());

        return documentPublicationIndexRepository.fetchNotDefendedThesesInPeriod(request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().stream().map(ThesisType::name).toList(), pageable);
    }

    @Override
    public Page<DocumentPublicationIndex> fetchDefendedThesesInPeriodNotSentToPromotion(
        NotAddedToPromotionThesesRequestDTO request, Integer libraryInstitutionId,
        Pageable pageable) {
        List<Integer> institutionIds;
        if (Objects.nonNull(libraryInstitutionId)) {
            institutionIds =
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                    libraryInstitutionId);
        } else {
            institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());
        }

        return documentPublicationIndexRepository.fetchDefendedThesesNotSentToPromotionInPeriod(
            request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().isEmpty() ?
                List.of(ThesisType.PHD.name(), ThesisType.PHD_ART_PROJECT.name()) :
                request.thesisTypes().stream().map(ThesisType::name).toList(),
            pageable
        );
    }

    @Override
    public Page<DocumentPublicationIndex> fetchAcceptedThesesInPeriod(
        ThesisReportRequestDTO request, Pageable pageable) {
        var institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());

        return documentPublicationIndexRepository.fetchAcceptedThesesInPeriod(request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().stream().map(ThesisType::name).toList(),
            pageable
        );
    }

    @Override
    public Page<DocumentPublicationIndex> fetchPublicReviewThesesInPeriod(
        ThesisReportRequestDTO request, Pageable pageable) {
        var institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());

        return documentPublicationIndexRepository.fetchThesesWithPublicReviewInPeriod(
            request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().stream().map(ThesisType::name).toList(),
            pageable
        );
    }

    @Override
    public Page<DocumentPublicationIndex> fetchPubliclyAvailableThesesInPeriod(
        ThesisReportRequestDTO request, Pageable pageable) {
        var institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());

        return documentPublicationIndexRepository.fetchDefendedThesesInPeriodWithExplicitAccess(
            request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().stream().map(ThesisType::name).toList(),
            true, pageable
        );
    }

    @Override
    public Page<DocumentPublicationIndex> fetchClosedAccessThesesInPeriod(
        ThesisReportRequestDTO request, Pageable pageable) {
        var institutionIds = getAllSubInstitutionsForTopLevelOnes(request.topLevelInstitutionIds());

        return documentPublicationIndexRepository.fetchDefendedThesesInPeriodWithExplicitAccess(
            request.fromDate(),
            request.toDate(), institutionIds,
            request.thesisTypes().stream().map(ThesisType::name).toList(),
            false, pageable
        );
    }

    @Override
    public Pair<InputStreamResource, Integer> generatePhdLibraryReportDocument(
        ThesisReportRequestDTO request,
        String locale) {
        if (!request.thesisTypes().contains(ThesisType.PHD) &&
            !request.thesisTypes().contains(ThesisType.PHD_ART_PROJECT)) {
            throw new ThesisException(
                "This report is only available for PHD dissertations and art projects.");
        }

        try {
            var reportCounts = createThesisCountsReport(request);
            var reportData = generateReportData(request, reportCounts, locale);

            synchronized (lock) {
                var document = ReportTemplateEngine.loadDocumentTemplate("phdLibraryReport.docx");

                ReportTemplateEngine.insertFields(document, reportData.a);
                ReportTemplateEngine.dynamicallyGenerateTableRows(document, reportData.b, 0);

                return ReportTemplateEngine.getReportAsResource(document);
            }
        } catch (IOException e) {
            throw new LoadingException(
                "Unable to load report template file."); // Should never happen
        }
    }

    private Pair<Map<String, String>, List<List<String>>> generateReportData(
        ThesisReportRequestDTO request,
        List<ThesisReportCountsDTO> reportCounts, String locale) {
        Map<String, String> replacements =
            constructReportHeaders(locale, String.valueOf(request.fromDate().getYear()),
                String.valueOf(request.toDate().getYear()));

        List<List<String>> rowData = new ArrayList<>();

        var counter = new AtomicInteger(1);
        var totalDefendedCount = new AtomicInteger(0);
        var totalNotDefendedCount = new AtomicInteger(0);
        var totalPubliclyAvailableCount = new AtomicInteger(0);
        var totalClosedAccessCount = new AtomicInteger(0);

        reportCounts.forEach(countReport -> {
            totalDefendedCount.addAndGet(countReport.defendedCount());
            totalNotDefendedCount.addAndGet(countReport.notDefendedCount());
//            totalPublicReviewCount.addAndGet(countReport.putOnPublicReviewCount());
//            totalAcceptedCount.addAndGet(countReport.topicsAcceptedCount());
            totalPubliclyAvailableCount.addAndGet(countReport.publiclyAvailableCount());
            totalClosedAccessCount.addAndGet(countReport.closedAccessCount());

            rowData.add(List.of(String.valueOf(counter.getAndIncrement()),
                getContentFromMCField(countReport.institutionName(), locale),
                String.valueOf(countReport.defendedCount()),
                String.valueOf(countReport.notDefendedCount()),
                String.valueOf(countReport.publiclyAvailableCount()),
                String.valueOf(countReport.closedAccessCount())));
        });

        rowData.add(List.of(String.valueOf(counter.get()),
            LocalizationUtil.getMessage("reporting.phdReport.total", new Object[] {}, locale),
            String.valueOf(totalDefendedCount.get()),
            String.valueOf(totalNotDefendedCount.get()),
            String.valueOf(totalPubliclyAvailableCount.get()),
            String.valueOf(totalClosedAccessCount.get())
        ));

        return new Pair<>(replacements, rowData);
    }

    private Map<String, String> constructReportHeaders(String locale, String fromYear,
                                                       String toYear) {
        return Map.of(
            "{header}",
            LocalizationUtil.getMessage("reporting.phdReport.header",
                new Object[] {fromYear, toYear},
                locale),
            "{col1}",
            LocalizationUtil.getMessage("reporting.table.rowNumber", new Object[] {}, locale),
            "{col2}",
            LocalizationUtil.getMessage("reporting.phdReport.faculty", new Object[] {}, locale),
            "{col3}",
            LocalizationUtil.getMessage("reporting.phdReport.defended", new Object[] {}, locale),
            "{col4}",
            LocalizationUtil.getMessage("reporting.phdReport.notDefended", new Object[] {},
                locale),
            "{col5}",
            LocalizationUtil.getMessage("reporting.phdReport.public", new Object[] {}, locale),
            "{col6}",
            LocalizationUtil.getMessage("reporting.phdReport.closed", new Object[] {}, locale)
        );
    }

    private List<Integer> getAllSubInstitutionsForTopLevelOnes(
        List<Integer> topLevelInstitutionIds) {
        var institutionSet = new HashSet<Integer>();
        topLevelInstitutionIds.forEach(institutionId -> {
            institutionSet.addAll(
                organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId));
        });

        return institutionSet.stream().toList();
    }

    private String getContentFromMCField(List<MultilingualContentDTO> contentList,
                                         String languageCode) {
        var localisedContent = contentList.stream()
            .filter(mc -> mc.getLanguageTag().equals(languageCode)).findFirst();
        if (localisedContent.isPresent()) {
            return languageCode.toUpperCase().startsWith("SR") ?
                SerbianTransliteration.toCyrillic(localisedContent.get().getContent()) :
                localisedContent.get().getContent();
        }

        return contentList.stream()
            .findFirst()
            .map(MultilingualContentDTO::getContent)
            .orElseThrow(() -> new NotFoundException("Missing container title"));
    }
}
