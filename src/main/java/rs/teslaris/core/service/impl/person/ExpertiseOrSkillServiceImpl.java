package rs.teslaris.core.service.impl.person;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.person.ExpertiseOrSkillConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillResponseDTO;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.repository.person.ExpertiseOrSkillRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class ExpertiseOrSkillServiceImpl extends JPAServiceImpl<ExpertiseOrSkill>
    implements ExpertiseOrSkillService {

    private final ExpertiseOrSkillRepository expertiseOrSkillRepository;

    private final PersonService personService;

    private final MultilingualContentService multilingualContentService;

    private final DocumentFileService documentFileService;

    private final ResearchAreaService researchAreaService;


    @Override
    protected JpaRepository<ExpertiseOrSkill, Integer> getEntityRepository() {
        return expertiseOrSkillRepository;
    }

    @Override
    public ExpertiseOrSkillResponseDTO addExpertiseOrSkill(Integer personId,
                                                           ExpertiseOrSkillDTO dto) {
        var person = personService.findOne(personId);
        var newExpertiseOrSkill = new ExpertiseOrSkill();

        setCommonFields(newExpertiseOrSkill, dto);
        newExpertiseOrSkill.setPerson(person);
        var savedExpertiseOrSkill = expertiseOrSkillRepository.save(newExpertiseOrSkill);

        person.getExpertisesAndSkills().add(newExpertiseOrSkill);
        personService.save(person);

        return ExpertiseOrSkillConverter.toDTO(savedExpertiseOrSkill);
    }

    @Override
    public ExpertiseOrSkillResponseDTO updateExpertiseOrSkill(Integer expertiseOrSkillId,
                                                              ExpertiseOrSkillDTO dto) {
        var expertiseOrSkill = findOne(expertiseOrSkillId);
        setCommonFields(expertiseOrSkill, dto);

        var savedExpertiseOrSkill = expertiseOrSkillRepository.save(expertiseOrSkill);

        return ExpertiseOrSkillConverter.toDTO(savedExpertiseOrSkill);
    }

    @Override
    public void deleteExpertiseOrSkill(Integer expertiseOrSkillId, Integer personId) {
        var person = personService.findOne(personId);
        person.setExpertisesAndSkills(person.getExpertisesAndSkills().stream().filter(
                expertiseOrSkill -> !Objects.equals(expertiseOrSkill.getId(), expertiseOrSkillId))
            .collect(Collectors.toSet()));
        delete(expertiseOrSkillId);
    }

    @Override
    public DocumentFileResponseDTO addProof(Integer expertiseOrSkillId,
                                            DocumentFileDTO proof) {
        var expertiseOrSkill = findOne(expertiseOrSkillId);
        var documentFile =
            documentFileService.saveNewPersonalDocument(proof, false, expertiseOrSkill.getPerson());
        expertiseOrSkill.getProofs().add(documentFile);
        save(expertiseOrSkill);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    public void deleteProof(Integer proofId, Integer expertiseOrSkillId) {
        var expertiseOrSkill = findOne(expertiseOrSkillId);
        var documentFile = documentFileService.findDocumentFileById(proofId);

        expertiseOrSkill.setProofs(expertiseOrSkill.getProofs().stream()
            .filter(proof -> !Objects.equals(proof.getId(), proofId)).collect(
                Collectors.toSet()));
        save(expertiseOrSkill);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    private void setCommonFields(ExpertiseOrSkill expertiseOrSkill, ExpertiseOrSkillDTO dto) {
        expertiseOrSkill.setName(multilingualContentService.getMultilingualContent(dto.getName()));
        expertiseOrSkill.setDescription(
            multilingualContentService.getMultilingualContent(dto.getDescription()));
        expertiseOrSkill.setKeywords(
            multilingualContentService.getMultilingualContent(dto.getKeywords()));
        expertiseOrSkill.setFavorite(Objects.requireNonNullElse(dto.getFavorite(), false));

        var researchAreas =
            researchAreaService.getResearchAreasByIds(dto.getResearchAreasId().stream().toList());
        expertiseOrSkill.setResearchAreas(new HashSet<>(researchAreas));
    }
}
