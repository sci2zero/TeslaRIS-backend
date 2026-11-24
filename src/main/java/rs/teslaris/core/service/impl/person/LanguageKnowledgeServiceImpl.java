package rs.teslaris.core.service.impl.person;

import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.person.LanguageKnowledgeConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeRequestDTO;
import rs.teslaris.core.dto.person.LanguageKnowledgeResponseDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.LanguageKnowledge;
import rs.teslaris.core.repository.person.LanguageKnowledgeRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.LanguageKnowledgeService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.exceptionhandling.exception.ExpertiseOrSkillAlreadyExistsException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class LanguageKnowledgeServiceImpl extends JPAServiceImpl<LanguageKnowledge>
    implements LanguageKnowledgeService {

    private final LanguageKnowledgeRepository languageKnowledgeRepository;

    private final PersonService personService;

    private final LanguageService languageService;

    private final DocumentFileService documentFileService;


    @Override
    protected JpaRepository<LanguageKnowledge, Integer> getEntityRepository() {
        return languageKnowledgeRepository;
    }

    @Override
    public LanguageKnowledgeResponseDTO addLanguageKnowledge(Integer personId,
                                                             LanguageKnowledgeRequestDTO dto) {
        var person = personService.findOne(personId);
        var languageKnowledge = new LanguageKnowledge();

        assertLanguageIsNotADuplicate(personId, dto, null);

        setCommonFields(languageKnowledge, dto);
        languageKnowledge.setPerson(person);

        return LanguageKnowledgeConverter.toDTO(languageKnowledgeRepository.save(
            languageKnowledge));
    }

    @Override
    public LanguageKnowledgeResponseDTO updateLanguageKnowledge(Integer languageKnowledgeId,
                                                                LanguageKnowledgeRequestDTO dto) {
        var languageKnowledge = findOne(languageKnowledgeId);

        assertLanguageIsNotADuplicate(languageKnowledge.getPerson().getId(), dto,
            languageKnowledgeId);

        setCommonFields(languageKnowledge, dto);

        return LanguageKnowledgeConverter.toDTO(languageKnowledgeRepository.save(
            languageKnowledge));
    }

    /**
     * The collection of a person removes an orphan, so the delete marks the row and keeps the
     * collection as it is.
     */
    @Override
    public void deleteLanguageKnowledge(Integer languageKnowledgeId, Integer personId) {
        var languageKnowledge = findOne(languageKnowledgeId);

        if (!Objects.equals(languageKnowledge.getPerson().getId(), personId)) {
            throw new NotFoundException("Language knowledge is not for this person.");
        }

        delete(languageKnowledgeId);
    }

    @Override
    public DocumentFileResponseDTO addProof(Integer languageKnowledgeId, DocumentFileDTO proof) {
        var languageKnowledge = findOne(languageKnowledgeId);
        var documentFile = documentFileService.saveNewPersonalDocument(proof, false,
            languageKnowledge.getPerson());
        languageKnowledge.getProofs().add(documentFile);
        save(languageKnowledge);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    public void deleteProof(Integer proofId, Integer languageKnowledgeId) {
        var languageKnowledge = findOne(languageKnowledgeId);
        var documentFile = documentFileService.findDocumentFileById(proofId);

        languageKnowledge.setProofs(languageKnowledge.getProofs().stream()
            .filter(proof -> !Objects.equals(proof.getId(), proofId))
            .collect(Collectors.toSet()));
        save(languageKnowledge);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    private void assertLanguageIsNotADuplicate(Integer personId, LanguageKnowledgeRequestDTO dto,
                                               Integer languageKnowledgeId) {
        if (languageKnowledgeRepository.existsForPersonWithLanguage(personId, dto.getLanguageId(),
            languageKnowledgeId)) {
            throw new ExpertiseOrSkillAlreadyExistsException(
                "languageKnowledgeAlreadyExistsMessage");
        }

        if (Boolean.TRUE.equals(dto.getMotherTongue()) &&
            languageKnowledgeRepository.existsMotherTongueForPerson(personId,
                languageKnowledgeId)) {
            throw new ExpertiseOrSkillAlreadyExistsException("motherTongueAlreadyExistsMessage");
        }
    }

    private void setCommonFields(LanguageKnowledge languageKnowledge,
                                 LanguageKnowledgeRequestDTO dto) {
        var language = languageService.findLanguageById(dto.getLanguageId());

        languageKnowledge.setLanguage(language);
        languageKnowledge.setMotherTongue(
            Objects.nonNull(dto.getMotherTongue()) ? dto.getMotherTongue() : false);
        languageKnowledge.setReading(dto.getReading());
        languageKnowledge.setWriting(dto.getWriting());
        languageKnowledge.setSpeaking(dto.getSpeaking());
        languageKnowledge.setListening(dto.getListening());
        languageKnowledge.setAcademicReview(dto.getAcademicReview());
        languageKnowledge.setAcademicWriting(dto.getAcademicWriting());
        languageKnowledge.setOverall(dto.getOverall());

        // Expertises are listed by name, so the language names stand in for one
        languageKnowledge.getName().clear();
        language.getName().stream()
            .map(MultiLingualContent::new)
            .forEach(languageKnowledge.getName()::add);
    }
}
