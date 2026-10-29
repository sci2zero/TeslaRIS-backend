package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.model.funding.FundingType;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Fundings of one SciPROJ project (CW-002-MAP-000001…000014 and CW-002-MAP-000036…000039).
 */
@Component
@RequiredArgsConstructor
public class HydratorFundingExtractor
    implements RecordExtractor<HydratorProjectModel.ProjectDocument, FundingMigrationDTO> {

    // OpenAIRE funding types seen in the source; any other type is dropped
    private static final Map<String, FundingType> FUNDING_TYPES = Map.of(
        "FundingProgramme", FundingType.FUNDING_PROGRAMME,
        "Contract", FundingType.CONTRACT,
        "Gift", FundingType.GIFT,
        "Call", FundingType.CALL);

    private final HydratorConversionUtil conversionUtil;

    private final HydratorProjectConverter projectConverter;

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
        var consortiumAmounts = HydratorProjectConverter.fundingCount(project) == 1 ?
            projectConverter.consortiumEntries(project.consortium()).stream()
                .filter(entry -> Objects.nonNull(entry.amount()))
                .toList() :
            List.<ProjectMigrationDTO.ConsortiumEntry>of();

        project.funded().forEach(funded -> {
            var funding = Objects.isNull(funded) || Objects.isNull(funded.fundedAs()) ? null :
                funded.fundedAs().funding();

            if (Objects.isNull(funding) || isBlank(funding.id())) {
                dropped(project.projectId(), HydratorMappingRule.sciproj(1), "funding without id");
                return;
            }

            var key = FundingMigrationDTO.keyOf(project.projectId(), funding.id());
            var dto = new FundingDTO();
            dto.getOldIds().add(funding.id().trim());
            dto.setImportSource(HydratorSource.PROJECTS_IMPORT_SOURCE);

            if (Objects.nonNull(funding.name())) {
                dto.setName(conversionUtil.multilingualContent(funding.name().value(),
                    funding.name().language()));
            }

            if (Objects.nonNull(funding.type()) && !isBlank(funding.type().value())) {
                var type = typeName(funding.type().value());
                if (FUNDING_TYPES.containsKey(type)) {
                    dto.getFundingTypes().add(FUNDING_TYPES.get(type));
                } else {
                    dropped(key, HydratorMappingRule.sciproj(2),
                        "funding type '" + type + "' has no vocabulary");
                }
            }

            setIdentifiers(dto, funding.identifiers());
            if (!isBlank(funding.grantId())) {
                dto.setGrantAgreementId(funding.grantId().trim());
            }

            setOaMandate(dto, project.oaMandate());

            String rejection = null;
            try {
                if (Objects.nonNull(funding.duration())) {
                    dto.setDateFrom(conversionUtil.isoDate(funding.duration().startDate()));
                    dto.setDateTo(conversionUtil.isoDate(funding.duration().endDate()));
                }
                dto.setAmount(amount(funding.amount()));
            } catch (InvalidSourceValueException e) {
                rejection = e.getMessage();
            }

            result.add(new FundingMigrationDTO(project.projectId(), funding.id(),
                funder(funded.fundedBy()), dto, consortiumAmounts, rejection));
        });

        return result;
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

    // CW-002-MAP-000036/000037 swap the two fields; mapped by meaning
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
            amount.currency().trim().toUpperCase());

        return dto;
    }

    private HydratorCVModel.Institution funder(HydratorProjectModel.FundedBy fundedBy) {
        return Objects.isNull(fundedBy) ? null : conversionUtil.institution(fundedBy.orgUnit());
    }

    private String typeName(String type) {
        return conversionUtil.typeName(type);
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT_FUNDING.name(),
            key, rule, reason);
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
