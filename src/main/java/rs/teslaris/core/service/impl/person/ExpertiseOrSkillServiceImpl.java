package rs.teslaris.core.service.impl.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillDTO;
import rs.teslaris.core.dto.person.ExpertiseOrSkillResponseDTO;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.repository.person.ExpertiseOrSkillRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertiseOrSkillServiceImpl extends JPAServiceImpl<ExpertiseOrSkill>
    implements ExpertiseOrSkillService {

    private final ExpertiseOrSkillRepository expertiseOrSkillRepository;

    private final PersonService personService;

    private final MultilingualContentService multilingualContentService;

    private final DocumentFileService documentFileService;


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
        var savedExpertiseOrSkill = expertiseOrSkillRepository.save(newExpertiseOrSkill);

        person.getExpertisesAndSkills().add(newExpertiseOrSkill);
        personService.save(person);

        return new ExpertiseOrSkillResponseDTO(
            savedExpertiseOrSkill.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(savedExpertiseOrSkill.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(
                savedExpertiseOrSkill.getDescription()), new ArrayList<>());
    }

    @Override
    public ExpertiseOrSkillResponseDTO updateExpertiseOrSkill(Integer expertiseOrSkillId,
                                                              ExpertiseOrSkillDTO dto) {
        var expertiseOrSkill = findOne(expertiseOrSkillId);
        setCommonFields(expertiseOrSkill, dto);

        expertiseOrSkillRepository.save(expertiseOrSkill);

        return new ExpertiseOrSkillResponseDTO(
            expertiseOrSkill.getId(),
            MultilingualContentConverter.getMultilingualContentDTO(expertiseOrSkill.getName()),
            MultilingualContentConverter.getMultilingualContentDTO(
                expertiseOrSkill.getDescription()), expertiseOrSkill.getProofs().stream().map(
            DocumentFileConverter::toDTO).collect(Collectors.toList()));
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
        var documentFile = documentFileService.saveNewDocument(proof, false);
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

    @Override
    public void switchSkills(List<Integer> skillIds, Integer sourcePersonId,
                             Integer targetPersonId) {
        var sourcePerson = personService.findOne(sourcePersonId);
        var targetPerson = personService.findOne(targetPersonId);

        skillIds.forEach(skillId -> {
            var skillToUpdate = findOne(skillId);

            sourcePerson.getExpertisesAndSkills().remove(skillToUpdate);

            targetPerson.getExpertisesAndSkills().add(skillToUpdate);
        });

        personService.save(sourcePerson);
        personService.save(targetPerson);
    }

    private void setCommonFields(ExpertiseOrSkill expertiseOrSkill, ExpertiseOrSkillDTO dto) {
        expertiseOrSkill.setName(multilingualContentService.getMultilingualContent(dto.getName()));
        expertiseOrSkill.setDescription(
            multilingualContentService.getMultilingualContent(dto.getDescription()));
    }
}
