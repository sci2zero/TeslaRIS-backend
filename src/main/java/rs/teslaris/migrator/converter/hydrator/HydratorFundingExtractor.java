package rs.teslaris.migrator.converter.hydrator;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.funding.FundingDTO;

/**
 * Fundings of one SciPROJ project (CW-002 MAP-001…014, project MAP-036…039).
 */
@Component
@RequiredArgsConstructor
public class HydratorFundingExtractor
    implements RecordExtractor<HydratorProjectModel.ProjectDocument, FundingMigrationDTO> {

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public List<FundingMigrationDTO> extract(HydratorProjectModel.ProjectDocument record) {
        var project = Objects.isNull(record) || Objects.isNull(record.record()) ||
            Objects.isNull(record.record().metadata()) ? null :
            record.record().metadata().project();

        if (Objects.isNull(project) || isBlank(project.projectId()) ||
            Objects.isNull(project.funded())) {
            return List.of();
        }

        var result = new ArrayList<FundingMigrationDTO>();

        project.funded().forEach(funded -> {
            var funding = Objects.isNull(funded) || Objects.isNull(funded.fundedAs()) ? null :
                funded.fundedAs().funding();

            if (Objects.isNull(funding) || isBlank(funding.id())) {
                dropped(project.projectId(), "MAP-001", "funding without id");
                return;
            }

            var key = keyOf(project.projectId(), funding.id());
            var dto = new FundingDTO();

            if (Objects.nonNull(funding.name())) {
                dto.setName(conversionUtil.multilingualContent(funding.name().value(),
                    funding.name().language()));
            }

            if (Objects.nonNull(funding.type()) && !isBlank(funding.type().value())) {
                dropped(key, "MAP-002",
                    "funding type '" + typeName(funding.type().value()) + "' has no vocabulary");
            }

            setIdentifiers(dto, funding.identifiers());
            if (!isBlank(funding.grantId())) {
                dto.setGrantAgreementId(funding.grantId().trim());
            }

            setOaMandate(dto, project.oaMandate());

            String rejection = null;
            try {
                if (Objects.nonNull(funding.duration())) {
                    dto.setDateFrom(date(funding.duration().startDate()));
                    dto.setDateTo(date(funding.duration().endDate()));
                }
                dto.setAmount(amount(funding.amount()));
            } catch (InvalidSourceValueException e) {
                rejection = e.getMessage();
            }

            result.add(new FundingMigrationDTO(project.projectId(), funding.id(),
                funder(funded.fundedBy()), dto, rejection));
        });

        return result;
    }

    public String keyOf(HydratorProjectModel.ProjectDocument record, FundingMigrationDTO dto) {
        return keyOf(dto.projectSourceKey(), dto.sourceId());
    }

    private String keyOf(String projectId, String fundingId) {
        return projectId + "#funding#" + fundingId;
    }

    // DOI is the funding's strong id; project references and QREN codes are internal ids
    private void setIdentifiers(FundingDTO dto, List<HydratorProjectModel.TypedValue> identifiers) {
        if (Objects.isNull(identifiers)) {
            return;
        }

        var internal = new LinkedHashSet<String>();
        identifiers.stream()
            .filter(identifier -> Objects.nonNull(identifier) && !isBlank(identifier.value()))
            .forEach(identifier -> {
                if ("DOI".equalsIgnoreCase(typeName(identifier.type())) &&
                    Objects.isNull(dto.getDoi())) {
                    dto.setDoi(identifier.value().trim());
                } else {
                    internal.add(identifier.value().trim());
                }
            });
        dto.setInternalIdentifiers(internal);
    }

    // MAP-036/037 swap the two fields; mapped by meaning
    private void setOaMandate(FundingDTO dto, HydratorProjectModel.OAMandate oaMandate) {
        if (Objects.isNull(oaMandate)) {
            return;
        }

        if (!isBlank(oaMandate.mandated())) {
            dto.setOaMandated(Boolean.parseBoolean(oaMandate.mandated().trim()));
        }
        if (!isBlank(oaMandate.uri())) {
            dto.setOaMandateUrl(oaMandate.uri().trim());
        }
    }

    // Currency is resolved by code at creation time
    private MonetaryAmountDTO amount(HydratorProjectModel.Amount amount) {
        if (Objects.isNull(amount) || isBlank(amount.value())) {
            return null;
        }

        var dto = new MonetaryAmountDTO();
        try {
            dto.setAmount(Double.parseDouble(amount.value().trim()));
        } catch (NumberFormatException e) {
            throw new InvalidSourceValueException("invalid amount '" + amount.value() + "'");
        }
        dto.setCurrencyCode(isBlank(amount.currency()) ? null :
            amount.currency().trim().toUpperCase(Locale.ROOT));

        return dto;
    }

    private HydratorCVModel.Institution funder(HydratorProjectModel.FundedBy fundedBy) {
        var orgUnit = Objects.isNull(fundedBy) ? null : fundedBy.orgUnit();
        if (Objects.isNull(orgUnit)) {
            return null;
        }

        var identifiers = new ArrayList<HydratorCVModel.InstitutionIdentifier>();
        if (Objects.nonNull(orgUnit.identifiers())) {
            orgUnit.identifiers().stream()
                .filter(Objects::nonNull)
                .forEach(identifier -> identifiers.add(new HydratorCVModel.InstitutionIdentifier(
                    identifier.value(), typeName(identifier.type()))));
        }
        if (!isBlank(orgUnit.rorId())) {
            identifiers.add(new HydratorCVModel.InstitutionIdentifier(orgUnit.rorId(), "ROR"));
        }

        var name = Objects.isNull(orgUnit.name()) ? null : orgUnit.name().value();
        return new HydratorCVModel.Institution(name, null, null,
            new HydratorCVModel.OtherIdentifiers(identifiers.size(), identifiers));
    }

    private LocalDate date(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidSourceValueException("invalid date '" + value + "'");
        }
    }

    private String typeName(String type) {
        if (isBlank(type)) {
            return "";
        }

        var hash = type.lastIndexOf('#');
        return hash >= 0 ? type.substring(hash + 1) : type.trim();
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT_FUNDING.name(),
            key, rule, reason);
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
