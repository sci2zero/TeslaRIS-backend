package rs.teslaris.core.importer.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Component
@RequiredArgsConstructor
public abstract class DocumentConverter {

    protected final MultilingualContentConverter multilingualContentConverter;

    private final PersonService personService;


    protected void setCommonFields(Publication record, DocumentDTO dto) {
        dto.setTitle(multilingualContentConverter.toDTO(record.getTitle()));
        dto.setSubTitle(multilingualContentConverter.toDTO(record.getSubtitle()));
        if (Objects.nonNull(record.getPublicationDate())) {
            dto.setDocumentDate(record.getPublicationDate().toString());
        }

        if (Objects.nonNull(record.getKeywords())) {
            dto.setKeywords(
                multilingualContentConverter.toDTO(Strings.join(record.getKeywords(), ", ")));
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        if (Objects.nonNull(record.getUrl())) {
            dto.setUris(new HashSet<>(record.getUrl()));
        } else {
            dto.setUris(new HashSet<>());
        }

        dto.setDoi(record.getDoi());
        dto.setDescription(multilingualContentConverter.toDTO(record.get_abstract()));

        setAuthorInformation(record, dto);
    }

    private void setAuthorInformation(Publication record, DocumentDTO dto) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        var authors = record.getAuthors();
        for (int i = 0; i < authors.size(); i++) {
            var author = authors.get(i);

            var contribution = new PersonDocumentContributionDTO();
            contribution.setContributionType(DocumentContributionType.AUTHOR);

            contribution.setContributionDescription(new ArrayList<>());
            contribution.setDisplayAffiliationStatement(new ArrayList<>());

            var person = personService.findPersonByOldId(
                OAIPMHParseUtility.parseBISISID(author.getPerson().getId()));
            if (Objects.isNull(person)) {
                System.out.println("No saved person with id: " + author.getPerson().getId());
                continue;
            }
            contribution.setPersonId(person.getId());

            if (Objects.nonNull(author.getDisplayName())) {
                contribution.setPersonName(
                    new PersonNameDTO(author.getDisplayName(), "", "", null, null));
            }

            if (i == 0) {
                contribution.setIsMainContributor(true);
            }

            contributions.add(contribution);
        }
        dto.setContributions(contributions);
    }
}
