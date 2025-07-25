package rs.teslaris.core.service.impl.person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.institution.InstitutionDefaultSubmissionContentDTO;
import rs.teslaris.core.model.institution.InstitutionDefaultSubmissionContent;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.institution.InstitutionDefaultSubmissionContentRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.InstitutionDefaultSubmissionContentService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
@RequiredArgsConstructor
@Transactional
public class InstitutionDefaultSubmissionContentServiceImpl
    extends JPAServiceImpl<InstitutionDefaultSubmissionContent>
    implements InstitutionDefaultSubmissionContentService {

    private final InstitutionDefaultSubmissionContentRepository
        institutionDefaultSubmissionContentRepository;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<InstitutionDefaultSubmissionContent, Integer> getEntityRepository() {
        return institutionDefaultSubmissionContentRepository;
    }

    @Override
    public InstitutionDefaultSubmissionContentDTO readInstitutionDefaultContentForUser(
        Integer userId) {
        var user = userService.findOne(userId);
        List<Integer> institutionIds;

        var roleName = user.getAuthority().getName();

        if (roleName.equals(UserRole.INSTITUTIONAL_EDITOR.name()) ||
            roleName.equals(UserRole.INSTITUTIONAL_LIBRARIAN.name())) {
            institutionIds = List.of(user.getOrganisationUnit().getId());
        } else if (roleName.equals(UserRole.RESEARCHER.name())) {
            institutionIds = user.getPerson().getInvolvements().stream()
                .filter(i -> (i.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                    i.getInvolvementType().equals(InvolvementType.HIRED_BY)) &&
                    i.getOrganisationUnit() != null)
                .map(i -> i.getOrganisationUnit().getId())
                .toList();
        } else {
            return new InstitutionDefaultSubmissionContentDTO(Collections.emptyList(),
                Collections.emptyList());
        }

        return findFirstAvailableInstitutionDefaultContent(institutionIds);
    }

    @Override
    public InstitutionDefaultSubmissionContentDTO readInstitutionDefaultContent(
        Integer institutionId) {
        return findFirstAvailableInstitutionDefaultContent(List.of(institutionId));
    }

    private InstitutionDefaultSubmissionContentDTO findFirstAvailableInstitutionDefaultContent(
        List<Integer> baseInstitutionIds) {
        var institutionIdsFullHierarchy = new ArrayList<Integer>();
        for (var id : baseInstitutionIds) {
            institutionIdsFullHierarchy.add(id);
            institutionIdsFullHierarchy.addAll(
                organisationUnitService.getSuperOUsHierarchyRecursive(id));
        }

        for (var id : institutionIdsFullHierarchy) {
            var contentOpt =
                institutionDefaultSubmissionContentRepository.getDefaultContentForInstitution(id);
            if (contentOpt.isPresent()) {
                var content = contentOpt.get();
                return new InstitutionDefaultSubmissionContentDTO(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        content.getTypeOfTitle()),
                    MultilingualContentConverter.getMultilingualContentDTO(content.getPlaceOfKeep())
                );
            }
        }

        return new InstitutionDefaultSubmissionContentDTO(Collections.emptyList(),
            Collections.emptyList());
    }

    @Override
    public void saveConfiguration(Integer institutionId,
                                  InstitutionDefaultSubmissionContentDTO content) {
        var institution = organisationUnitService.findOne(institutionId);

        institutionDefaultSubmissionContentRepository.getDefaultContentForInstitution(institutionId)
            .ifPresentOrElse(savedContent -> {
                savedContent.setPlaceOfKeep(
                    multilingualContentService.getMultilingualContent(content.placeOfKeep()));
                savedContent.setTypeOfTitle(
                    multilingualContentService.getMultilingualContent(content.typeOfTitle()));
                save(savedContent);
            }, () -> {
                var newContent = new InstitutionDefaultSubmissionContent();
                newContent.setInstitution(institution);
                newContent.setPlaceOfKeep(
                    multilingualContentService.getMultilingualContent(content.placeOfKeep()));
                newContent.setTypeOfTitle(
                    multilingualContentService.getMultilingualContent(content.typeOfTitle()));
                save(newContent);
            });
    }
}
