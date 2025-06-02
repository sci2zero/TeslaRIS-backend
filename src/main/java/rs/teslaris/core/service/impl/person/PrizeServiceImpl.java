package rs.teslaris.core.service.impl.person;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.person.PrizeRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class PrizeServiceImpl extends JPAServiceImpl<Prize> implements PrizeService {

    private final PrizeRepository prizeRepository;

    private final PersonService personService;

    private final MultilingualContentService multilingualContentService;

    private final DocumentFileService documentFileService;


    @Override
    protected JpaRepository<Prize, Integer> getEntityRepository() {
        return prizeRepository;
    }

    @Override
    public PrizeResponseDTO addPrize(Integer personId, PrizeDTO dto) {
        var newPrize = new Prize();
        var person = personService.findOne(personId);

        setCommonFields(newPrize, dto);
        newPrize.setPerson(person);
        var savedPrize = prizeRepository.save(newPrize);

        person.addPrize(savedPrize);

        return new PrizeResponseDTO(
            MultilingualContentConverter.getMultilingualContentDTO(savedPrize.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(savedPrize.getDescription()),
            savedPrize.getDate(), savedPrize.getId(), new ArrayList<>());
    }

    @Override
    public PrizeResponseDTO updatePrize(Integer prizeId, PrizeDTO dto) {
        var prizeToUpdate = findOne(prizeId);

        setCommonFields(prizeToUpdate, dto);
        prizeRepository.save(prizeToUpdate);

        return new PrizeResponseDTO(
            MultilingualContentConverter.getMultilingualContentDTO(prizeToUpdate.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(prizeToUpdate.getDescription()),
            prizeToUpdate.getDate(), prizeToUpdate.getId(), prizeToUpdate.getProofs().stream().map(
            DocumentFileConverter::toDTO).collect(Collectors.toList()));
    }

    @Override
    public void deletePrize(Integer prizeId, Integer personId) {
        var person = personService.findOne(personId);

        person.setPrizes(
            person.getPrizes().stream().filter(prize -> !Objects.equals(prize.getId(), prizeId))
                .collect(
                    Collectors.toSet()));

        delete(prizeId);
    }

    @Override
    public DocumentFileResponseDTO addProof(Integer prizeId, DocumentFileDTO proof) {
        var prize = findOne(prizeId);
        var documentFile =
            documentFileService.saveNewPersonalDocument(proof, false, prize.getPerson());
        prize.getProofs().add(documentFile);
        save(prize);

        return DocumentFileConverter.toDTO(documentFile);
    }

    @Override
    public DocumentFileResponseDTO updateProof(DocumentFileDTO updatedProof) {
        return documentFileService.editDocumentFile(updatedProof, false);
    }

    @Override
    public void deleteProof(Integer proofId, Integer prizeId) {
        var prize = findOne(prizeId);
        var documentFile = documentFileService.findDocumentFileById(proofId);

        prize.setProofs(prize.getProofs().stream()
            .filter(proof -> !Objects.equals(proof.getId(), proofId)).collect(
                Collectors.toSet()));
        save(prize);

        documentFileService.deleteDocumentFile(documentFile.getServerFilename());
    }

    private void setCommonFields(Prize prize, PrizeDTO dto) {
        prize.setTitle(multilingualContentService.getMultilingualContent(dto.getTitle()));
        prize.setDescription(
            multilingualContentService.getMultilingualContent(dto.getDescription()));
        prize.setDate(dto.getDate());
    }
}
