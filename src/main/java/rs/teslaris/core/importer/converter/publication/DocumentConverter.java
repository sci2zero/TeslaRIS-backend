package rs.teslaris.core.importer.converter.publication;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.importer.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.importer.publication.Contributor;
import rs.teslaris.core.importer.publication.Publication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Component
@RequiredArgsConstructor
@Slf4j
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

        setContributionInformation(record, dto);
    }

    private void setContributionInformation(Publication record, DocumentDTO dto) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();

        addContributors(record.getAuthors(), DocumentContributionType.AUTHOR, contributions);
        addContributors(record.getEditors(), DocumentContributionType.EDITOR, contributions);

        dto.setContributions(contributions);
    }

    private <T extends Contributor> void addContributors(List<T> contributors,
                                                         DocumentContributionType contributionType,
                                                         List<PersonDocumentContributionDTO> contributions) {
        if (Objects.isNull(contributors)) {
            return;
        }

        for (int i = 0; i < contributors.size(); i++) {
            var contributor = contributors.get(i);
            var contribution = createContribution(contributor, contributionType, i);
            if (Objects.nonNull(contribution)) {
                contribution.setIsMainContributor(i == 0);
                contribution.setIsCorrespondingContributor(false);
                contributions.add(contribution);
            }
        }
    }

    @Nullable
    private <T extends Contributor> PersonDocumentContributionDTO createContribution(T contributor,
                                                                                     DocumentContributionType contributionType,
                                                                                     int orderNumber) {
        var contribution = new PersonDocumentContributionDTO();
        contribution.setContributionType(contributionType);
        contribution.setContributionDescription(new ArrayList<>());
        contribution.setDisplayAffiliationStatement(new ArrayList<>());

        var person = personService.findPersonByOldId(
            OAIPMHParseUtility.parseBISISID(contributor.getPerson().getId()));
        if (Objects.isNull(person)) {
            log.warn("No saved person with id: " + contributor.getPerson().getId());
            return null;
        }
        contribution.setPersonId(person.getId());

        if (Objects.nonNull(contributor.getDisplayName())) {
            contribution.setPersonName(
                new PersonNameDTO(contributor.getDisplayName(), "", "", null, null));
        }

        contribution.setOrderNumber(orderNumber);

        return contribution;
    }
}
