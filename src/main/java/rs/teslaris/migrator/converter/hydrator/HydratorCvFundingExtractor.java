package rs.teslaris.migrator.converter.hydrator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectInvestigationRole;
import rs.teslaris.core.model.project.ProjectStatus;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Fundings of one curriculum (CW-001-MAP-000001…000017), each with the project it belongs to and
 * the owner's part in it. The source has no funding name, so the funding stays unnamed.
 */
@Component
@RequiredArgsConstructor
public class HydratorCvFundingExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, CvFundingMigrationDTO> {

    private static final Map<String, FundingType> FUNDING_TYPES = Map.of(
        "B", FundingType.GRANT,
        "C", FundingType.CONTRACT,
        "O", FundingType.OTHER
    );

    private static final Map<String, ProjectStatus> STATUSES = Map.of(
        "C", ProjectStatus.CONCLUDED,
        "E", ProjectStatus.ONGOING,
        "D", ProjectStatus.CANCELLED
    );

    // Principal investigator is a contribution type, not an investigation role
    private static final String PRINCIPAL_INVESTIGATOR = "IR";

    private static final Map<String, PersonProjectInvestigationRole> INVESTIGATION_ROLES =
        Map.ofEntries(
            Map.entry("I", PersonProjectInvestigationRole.RESEARCHER),
            Map.entry("BI", PersonProjectInvestigationRole.RESEARCH_FELLOW),
            Map.entry("D", PersonProjectInvestigationRole.PHD_STUDENT_FELLOW),
            Map.entry("OT", PersonProjectInvestigationRole.OTHER),
            Map.entry("M", PersonProjectInvestigationRole.MASTER_STUDENT_FELLOW),
            Map.entry("BPD", PersonProjectInvestigationRole.POSTDOC_FELLOW),
            Map.entry("BII",
                PersonProjectInvestigationRole.INTEGRATION_INTO_RESEARCH_GRANT_FELLOW),
            Map.entry("BIC", PersonProjectInvestigationRole.SCIENTIFIC_INITIATION_FELLOW),
            Map.entry("O", PersonProjectInvestigationRole.SUPERVISOR),
            Map.entry("DT", PersonProjectInvestigationRole.TECHNICAL_DEVELOPMENT),
            Map.entry("PD", PersonProjectInvestigationRole.POSTDOC),
            Map.entry("BCC", PersonProjectInvestigationRole.INVITED_SCIENTIST_FELLOW),
            Map.entry("BTI", PersonProjectInvestigationRole.RESEARCH_TECHNICIAN_FELLOW),
            Map.entry("BGC",
                PersonProjectInvestigationRole.SCIENCE_AND_TECHNOLOGY_MANAGEMENT_FELLOW)
        );

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public List<CvFundingMigrationDTO> extract(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().fundings()) ||
            Objects.isNull(record.curriculum().fundings().funding())) {
            return List.of();
        }

        var language = record.curriculum().language();
        var result = new ArrayList<CvFundingMigrationDTO>();

        record.curriculum().fundings().funding().forEach(funding -> {
            if (Objects.isNull(funding) || !StringUtil.valueExists(funding.id())) {
                dropped(record.id(), HydratorMappingRule.cv(6), "funding without id");
                return;
            }

            var key = CvFundingMigrationDTO.keyOf(record.id(), funding.id());

            if (!StringUtil.valueExists(funding.projectTitle())) {
                dropped(key, HydratorMappingRule.cv(6), "funding without project title");
                return;
            }

            var references = references(funding);
            var reference = references.stream()
                .filter(HydratorFundingReference::isStrong)
                .findFirst()
                .orElse(null);

            String rejection = null;
            var project = project(key, funding, reference, references, language);
            var owner = owner(key, funding);
            var fundingDTO = funding(key, funding, references, language);
            try {
                project.setDateFrom(conversionUtil.localDate(funding.startDate()));
                project.setDateTo(conversionUtil.localDate(funding.endDate()));
                owner.setDateFrom(conversionUtil.localDate(funding.startDateParticipation()));
                owner.setDateTo(conversionUtil.localDate(funding.endDateParticipation()));
                fundingDTO.setDateAwarded(yearAwarded(funding.yearAwarded()));
                fundingDTO.setAmount(amount(key, funding.totalAmount()));
            } catch (InvalidSourceValueException e) {
                rejection = e.getMessage();
            }
            project.setStatus(status(key, funding.status(), project));

            result.add(new CvFundingMigrationDTO(record.id(), funding.id(), reference, project,
                consortium(funding.institutions()), investigators(key, funding.investigators()),
                owner, fundingDTO, funder(key, funding.fundingInstitutions()), rejection));
        });

        return result;
    }

    // Blank references are skipped; the rest keep their source order
    private List<HydratorFundingReference> references(HydratorCVModel.Funding funding) {
        if (Objects.isNull(funding.fundingIdentifiers()) ||
            Objects.isNull(funding.fundingIdentifiers().fundingIdentifier())) {
            return List.of();
        }

        return funding.fundingIdentifiers().fundingIdentifier().stream()
            .filter(identifier -> Objects.nonNull(identifier) && StringUtil.valueExists(identifier.reference()))
            .map(identifier -> HydratorFundingReference.of(identifier.reference()))
            .toList();
    }

    // CW-001-MAP-000006…000009/000014/000015; dates and status are set by the caller
    private ProjectDTO project(String key, HydratorCVModel.Funding funding,
                               HydratorFundingReference reference,
                               List<HydratorFundingReference> references, String language) {
        var dto = new ProjectDTO();
        dto.getOldIds().add(key);
        dto.setImportSource(HydratorSource.IMPORT_SOURCE);
        dto.setName(conversionUtil.multilingualContent(funding.projectTitle().trim(), language));
        dto.setDescription(
            conversionUtil.multilingualContent(funding.projectDescription(), language));
        dto.setKeywords(conversionUtil.multilingualContent(keywords(funding), language));
        dto.setResearchAreasId(new HashSet<>(researchAreas(key, funding)));

        // Strong references let the next curriculum find this project, see HydratorProjectResolver
        if (Objects.nonNull(reference) && reference.kind() == HydratorFundingReference.Kind.FCT) {
            dto.setNationalId(reference.lookupValue());
        }
        dto.setInternalIdentifiers(originals(references.stream()
            .filter(HydratorFundingReference::isStrong).toList()));

        return dto;
    }

    // CW-001-MAP-000001…000005
    private FundingDTO funding(String key, HydratorCVModel.Funding funding,
                               List<HydratorFundingReference> references, String language) {
        var dto = new FundingDTO();
        dto.getOldIds().add(key);
        dto.setImportSource(HydratorSource.IMPORT_SOURCE);
        dto.setName(new ArrayList<>());
        dto.setFundingTypes(fundingTypes(key, funding.fundingCategory()));
        dto.setInternalIdentifiers(originals(references));
        dto.setCompetitive(flag(key, "competitive", funding.competitive()));
        dto.setRenewable(flag(key, "funding-renewable", funding.fundingRenewable()));

        // CW-001-MAP-000003 as written: program-name names a programme, not the funder
        // (displayProgram would fit), see cv-funding-patch-review.md
        if (StringUtil.valueExists(funding.programName())) {
            dto.setDisplayFunder(
                conversionUtil.multilingualContent(funding.programName().trim(), language));
        }

        return dto;
    }

    // CW-001-MAP-000010/000011/000017: dates are set by the caller
    private PersonProjectContributionDTO owner(String key, HydratorCVModel.Funding funding) {
        var dto = new PersonProjectContributionDTO();
        dto.setContributionType(PersonProjectContributionType.TEAM_MEMBER);
        dto.setContributionDescription(new ArrayList<>());
        dto.setDisplayAffiliationStatement(new ArrayList<>());

        var role = funding.investigationRole();
        if (Objects.isNull(role) || !StringUtil.valueExists(role.code())) {
            return dto;
        }

        var code = role.code().trim().toUpperCase();
        if (PRINCIPAL_INVESTIGATOR.equals(code)) {
            dto.setContributionType(PersonProjectContributionType.PRINCIPLE_INVESTIGATOR);
            return dto;
        }

        var investigationRole = INVESTIGATION_ROLES.get(code);
        if (Objects.isNull(investigationRole)) {
            dropped(key, HydratorMappingRule.cv(17),
                "unknown investigation role '" + role.code() + "'");
        }
        dto.setInvestigationRole(investigationRole);

        return dto;
    }

    // CW-001-MAP-000016: source order; the owner and duplicates are filtered at creation time
    private List<ProjectMigrationDTO.TeamEntry> investigators(
        String key, HydratorCVModel.FundingInvestigators investigators) {
        var entries = new ArrayList<ProjectMigrationDTO.TeamEntry>();

        if (Objects.isNull(investigators) || Objects.isNull(investigators.investigator())) {
            return entries;
        }

        investigators.investigator().stream()
            .filter(Objects::nonNull)
            .forEach(investigator -> {
                var name = !StringUtil.valueExists(investigator.name()) ? null : investigator.name().trim();
                var cienciaId = !StringUtil.valueExists(investigator.cienciaId()) ? null :
                    investigator.cienciaId().trim();

                if (Objects.isNull(name) && Objects.isNull(cienciaId)) {
                    dropped(key, HydratorMappingRule.cv(16),
                        "investigator without name and identifier");
                    return;
                }

                entries.add(new ProjectMigrationDTO.TeamEntry(
                    PersonProjectContributionType.TEAM_MEMBER, name, cienciaId, null,
                    entries.size() + 1));
            });

        return entries;
    }

    // CW-001-MAP-000013: the source gives no role, so every institution is a consortium member
    private List<ProjectMigrationDTO.ConsortiumEntry> consortium(
        HydratorCVModel.Institutions institutions) {
        var entries = new ArrayList<ProjectMigrationDTO.ConsortiumEntry>();

        if (Objects.isNull(institutions) || Objects.isNull(institutions.institution())) {
            return entries;
        }

        institutions.institution().stream()
            .filter(Objects::nonNull)
            .forEach(institution -> entries.add(new ProjectMigrationDTO.ConsortiumEntry(
                OrganisationUnitProjectContributionType.CONSORTIUM_MEMBER, institution,
                entries.size() + 1, null)));

        return entries;
    }

    // CW-001-MAP-000002: a funding has one funder; further institutions are not carried over
    private HydratorCVModel.Institution funder(String key,
                                               HydratorCVModel.Institutions institutions) {
        if (Objects.isNull(institutions) || Objects.isNull(institutions.institution())) {
            return null;
        }

        var funders = institutions.institution().stream().filter(Objects::nonNull).toList();
        if (funders.size() > 1) {
            dropped(key, HydratorMappingRule.cv(2),
                (funders.size() - 1) + " further funding institution(s) not carried over");
        }

        return funders.isEmpty() ? null : funders.getFirst();
    }

    private Set<FundingType> fundingTypes(String key, HydratorCVModel.CodeValue category) {
        var types = new HashSet<FundingType>();

        if (Objects.isNull(category) || !StringUtil.valueExists(category.code())) {
            return types;
        }

        var type = FUNDING_TYPES.get(category.code().trim().toUpperCase());
        if (Objects.isNull(type)) {
            dropped(key, HydratorMappingRule.cv(1),
                "funding category '" + category.code() + "' has no funding type");
        } else {
            types.add(type);
        }

        return types;
    }

    // CW-001-MAP-000012; without a status it is derived from the dates, as for SciPROJ
    private ProjectStatus status(String key, HydratorCVModel.FundingStatus status,
                                 ProjectDTO project) {
        if (Objects.nonNull(status) && StringUtil.valueExists(status.code())) {
            var mapped = STATUSES.get(status.code().trim().toUpperCase());
            if (Objects.nonNull(mapped)) {
                return mapped;
            }

            dropped(key, HydratorMappingRule.cv(12),
                "unknown status '" + status.code() + "', derived from dates");
        }

        return HydratorProjectConverter.status(project.getDateFrom(), project.getDateTo());
    }

    // The source keeps only the year; a non-numeric year is rejected, not guessed
    private LocalDate yearAwarded(String yearAwarded) {
        if (!StringUtil.valueExists(yearAwarded)) {
            return null;
        }

        var year = conversionUtil.parseInteger(yearAwarded);
        if (Objects.isNull(year) || year < 1 || year > 9999) {
            throw new InvalidSourceValueException("invalid year awarded '" + yearAwarded + "'");
        }

        return LocalDate.of(year, 1, 1);
    }

    // No crosswalk rule; currency is resolved by code at creation time
    private MonetaryAmountDTO amount(String key, HydratorCVModel.TotalAmount totalAmount) {
        if (Objects.isNull(totalAmount)) {
            return null;
        }

        if (!StringUtil.valueExists(totalAmount.amount())) {
            if (StringUtil.valueExists(totalAmount.currency())) {
                dropped(key, HydratorMappingRule.cvWithoutRule(),
                    "currency '" + totalAmount.currency() + "' without amount");
            }
            return null;
        }

        var dto = new MonetaryAmountDTO();
        try {
            dto.setAmount(Double.parseDouble(totalAmount.amount().trim()));
        } catch (NumberFormatException e) {
            throw new InvalidSourceValueException(
                "invalid amount '" + totalAmount.amount() + "'");
        }
        dto.setCurrencyCode(!StringUtil.valueExists(totalAmount.currency()) ? null :
            totalAmount.currency().trim().toUpperCase());

        return dto;
    }

    // No crosswalk rule; the source writes "true"/"false"
    private Boolean flag(String key, String field, String value) {
        if (!StringUtil.valueExists(value)) {
            return null;
        }

        var trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }

        dropped(key, HydratorMappingRule.cvWithoutRule(),
            "invalid " + field + " value '" + value + "'");
        return null;
    }

    // One keyword per line, as for the person's keywords
    private String keywords(HydratorCVModel.Funding funding) {
        if (Objects.isNull(funding.keywords()) || Objects.isNull(funding.keywords().keyword())) {
            return null;
        }

        var keywords = new LinkedHashSet<String>();
        funding.keywords().keyword().stream()
            .filter(keyword -> StringUtil.valueExists(keyword))
            .map(String::trim)
            .forEach(keywords::add);

        return keywords.isEmpty() ? null : String.join("\n", keywords);
    }

    private Set<Integer> researchAreas(String key, HydratorCVModel.Funding funding) {
        if (Objects.isNull(funding.researchClassifications()) ||
            Objects.isNull(funding.researchClassifications().researchClassification())) {
            return Set.of();
        }

        return conversionUtil.researchAreaIds(
            funding.researchClassifications().researchClassification(),
            reason -> dropped(key, HydratorMappingRule.cv(14), reason));
    }

    private Set<String> originals(List<HydratorFundingReference> references) {
        var originals = new LinkedHashSet<String>();
        references.forEach(reference -> originals.add(reference.original()));
        return originals;
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.CV_FUNDING.name(), key,
            rule, reason);
    }
}
