package rs.teslaris.core.service.impl.identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.IdentifierConverter;
import rs.teslaris.core.dto.identifier.IdentifierDTO;
import rs.teslaris.core.dto.identifier.IdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierCodeInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierReferenceConstraintViolationException;
import rs.teslaris.core.util.search.CollectionOperations;

@Service
@RequiredArgsConstructor
@Traceable
public class IdentifierServiceImpl extends JPAServiceImpl<Identifier> implements IdentifierService {

    private final IdentifierRepository identifierRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    protected JpaRepository<Identifier, Integer> getEntityRepository() {
        return identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IdentifierResponseDTO> readAllIdentifiers(Pageable pageable, String language) {
        return identifierRepository.readAll(language, pageable).map(IdentifierConverter::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentifierResponseDTO> getIdentifiersApplicableToEntity(
        List<ApplicableEntityType> applicableEntityTypes) {
        if (!applicableEntityTypes.isEmpty() &&
            !applicableEntityTypes.contains(ApplicableEntityType.ALL)) {
            applicableEntityTypes.add(ApplicableEntityType.ALL);
        }

        return identifierRepository.getIdentifiersApplicableToEntity(applicableEntityTypes).stream()
            .map(IdentifierConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IdentifierResponseDTO readIdentifierById(
        Integer identifierId) {
        return IdentifierConverter.toDTO(findOne(identifierId));
    }

    @Override
    public AccessLevel readIdentifierAccessLevel(Integer identifierId) {
        return findOne(identifierId).getAccessLevel();
    }

    @Override
    public Identifier getIdentifierByCode(String code) {
        return identifierRepository.findByCode(code);
    }

    @Override
    @Transactional
    public Identifier createIdentifier(IdentifierDTO identifier) {
        var newIdentifier = new Identifier();

        setCommonFields(newIdentifier, identifier);

        return save(newIdentifier);
    }

    @Override
    @Transactional
    public void updateIdentifier(Integer identifierId, IdentifierDTO identifier) {
        var identifierToUpdate = findOne(identifierId);

        setCommonFields(identifierToUpdate, identifier);

        save(identifierToUpdate);
    }

    private void setCommonFields(Identifier identifier, IdentifierDTO identifierDTO) {
        if (identifierRepository.identifierCodeInUse(identifierDTO.code(), identifier.getId())) {
            throw new IdentifierCodeInUseException(
                "Identifier code " + identifierDTO.code() + " is allready in use.");
        }

        identifier.setCode(identifierDTO.code());

        if (CollectionOperations.containsValues(identifierDTO.applicableTypes())) {
            identifier.setApplicableTypes(new HashSet<>(identifierDTO.applicableTypes()));
        } else {
            identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.ALL)));
        }

        identifier.setTitle(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                identifierDTO.title()));
        identifier.setDescription(
            multilingualContentService.getMultilingualContentAndSetDefaultsIfNonExistent(
                identifierDTO.description()));
        identifier.setAccessLevel(identifierDTO.identifierAccessLevel());

        identifier.setRegularExpression(
            Objects.requireNonNullElse(identifierDTO.regularExpression(), ""));
        identifier.setUriPrefix(Objects.requireNonNullElse(identifierDTO.uriPrefix(), ""));
    }

    @Override
    @Transactional
    public void deleteIdentifier(Integer identifierId) {
        if (identifierRepository.isInUse(identifierId)) {
            throw new IdentifierReferenceConstraintViolationException("identifierInUseMessage");
        }

        delete(identifierId);
    }
}
