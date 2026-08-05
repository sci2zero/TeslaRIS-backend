package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;

/**
 * One curriculum yields exactly one person, so this is a 1:1 converter wrapped with
 * {@link RecordExtractor#of}.
 */
@Component
@RequiredArgsConstructor
public class HydratorPersonConverter
    implements RecordExtractor.RecordConverter<HydratorCVModel.Curriculum, ImportPersonDTO> {

    private static final String ORCID_CODE = "ORCID";

    private static final String SCOPUS_CODE = "SCOPUS";

    private final HydratorConversionUtil conversionUtil;


    @Override
    public ImportPersonDTO toDTO(HydratorCVModel.Curriculum record) {
        var personInfo = personInfoOf(record);

        if (Objects.isNull(personInfo)) {
            return null;
        }

        var personName = personName(personInfo, record.fullName());

        if (Objects.isNull(personName)) {
            return null;
        }

        var dto = new ImportPersonDTO();
        dto.setPersonName(personName);
        dto.setOrcid(identifier(record, ORCID_CODE));
        dto.setScopusAuthorId(identifier(record, SCOPUS_CODE));

        var language = Objects.isNull(record.curriculum()) ? null : record.curriculum().language();
        var resume = Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().identifyingInfo()) ||
            Objects.isNull(record.curriculum().identifyingInfo().resume())
            ? null : record.curriculum().identifyingInfo().resume().text();

        dto.setBiography(conversionUtil.multilingualContent(resume, language));

        return dto;
    }

    private PersonNameDTO personName(HydratorCVModel.PersonInfo personInfo,
                                     String fullNameFallback) {
        var name = new PersonNameDTO();
        var firstName = personInfo.names();
        var lastName = personInfo.surnames();

        if (isBlank(firstName) || isBlank(lastName)) {
            var source = isBlank(personInfo.fullName()) ? fullNameFallback : personInfo.fullName();

            if (isBlank(source)) {
                return null;
            }

            var parts = source.trim().split("\\s+");
            firstName = parts[0];
            lastName = parts.length > 1 ? parts[parts.length - 1] : parts[0];
        }

        name.setFirstname(firstName.trim());
        name.setLastname(lastName.trim());

        return name;
    }

    private String identifier(HydratorCVModel.Curriculum record, String code) {
        return identifiersOf(record).stream()
            .filter(identifier -> Objects.nonNull(identifier.identifierType()))
            .filter(identifier -> code.equalsIgnoreCase(identifier.identifierType().code()) ||
                code.equalsIgnoreCase(identifier.identifierType().value()))
            .map(HydratorCVModel.AuthorIdentifier::identifier)
            .filter(value -> !isBlank(value))
            .findFirst()
            .orElse(null);
    }

    private List<HydratorCVModel.AuthorIdentifier> identifiersOf(
        HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().identifyingInfo()) ||
            Objects.isNull(record.curriculum().identifyingInfo().authorIdentifiers()) ||
            Objects.isNull(
                record.curriculum().identifyingInfo().authorIdentifiers().authorIdentifier())) {
            return List.of();
        }

        return record.curriculum().identifyingInfo().authorIdentifiers().authorIdentifier();
    }

    private HydratorCVModel.PersonInfo personInfoOf(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().identifyingInfo())) {
            return null;
        }

        return record.curriculum().identifyingInfo().personInfo();
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
