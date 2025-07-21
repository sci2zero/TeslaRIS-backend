package rs.teslaris.thesislibrary.service.impl;

import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.thesislibrary.converter.PublicReviewPageContentConverter;
import rs.teslaris.thesislibrary.dto.PublicReviewPageContentDTO;
import rs.teslaris.thesislibrary.model.PublicReviewPageContent;
import rs.teslaris.thesislibrary.repository.PublicReviewPageContentRepository;
import rs.teslaris.thesislibrary.service.interfaces.PublicReviewPageContentService;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicReviewPageContentServiceImpl extends JPAServiceImpl<PublicReviewPageContent>
    implements PublicReviewPageContentService {

    private final PublicReviewPageContentRepository publicReviewPageContentRepository;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<PublicReviewPageContent, Integer> getEntityRepository() {
        return publicReviewPageContentRepository;
    }

    @Override
    public List<PublicReviewPageContentDTO> readPageContentConfigurationForInstitution(
        Integer institutionId) {
        return publicReviewPageContentRepository.getConfigurationForInstitution(institutionId)
            .stream().map(PublicReviewPageContentConverter::toDTO).toList();
    }

    @Override
    public List<PublicReviewPageContentDTO> readPageContentConfigurationForInstitutionAndType(
        Integer institutionId, List<ThesisType> thesisTypes) {
        return publicReviewPageContentRepository.getConfigurationForInstitutionAndThesisTypes(
                institutionId, thesisTypes)
            .stream().map(PublicReviewPageContentConverter::toDTO).toList();
    }

    @Override
    public void savePageConfiguration(List<PublicReviewPageContentDTO> contentConfiguration,
                                      Integer institutionId) {
        var institution = organisationUnitService.findOne(institutionId);
        publicReviewPageContentRepository.deleteAllContentForInstitution(institutionId);

        contentConfiguration.forEach(content -> {
            var newContent = new PublicReviewPageContent();
            newContent.setInstitution(institution);
            newContent.setContentType(content.contentType());
            newContent.setPageType(content.pageType());
            newContent.setThesisTypes(new HashSet<>(content.thesisTypes()));
            newContent.setContent(
                multilingualContentService.getMultilingualContent(content.content()));

            publicReviewPageContentRepository.save(newContent);
        });
    }
}
