package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.revisioner.model.EntityRevision;

@Component
@RequiredArgsConstructor
public class DataQualityCalculatorPtCris {

    private final RevisionHydratorRegistry revisionHydratorRegistry;

    private final Map<Class<?>, BiConsumer<Object, EntityRevision>> assessors = Map.of(
        ThesisResponseDTO.class, (dto, rev) -> assessEntity((ThesisResponseDTO) dto, rev),
        DatasetDTO.class, (dto, rev) -> assessEntity((DatasetDTO) dto, rev),
        PersonalInfoDTO.class, (dto, rev) -> assessEntity((PersonalInfoDTO) dto, rev)
    );


    public void assessDataQuality(EntityRevision entityRevision, String json,
                                  ObjectMapper objectMapper)
        throws JsonProcessingException {
        Class<?> dtoClass = revisionHydratorRegistry.getDtoClass(entityRevision.getEntityType());

        Object dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);
        assessEntity(dto, entityRevision);
    }

    private void assessEntity(Object dto, EntityRevision entityRevision) {
        BiConsumer<Object, EntityRevision> assessor = assessors.get(dto.getClass());

        if (Objects.nonNull(assessor)) {
            assessor.accept(dto, entityRevision);
        }
    }

    private void assessEntity(DocumentDTO dto, EntityRevision entityRevision) {
        entityRevision.setQualityDataScore(0.0);

        if (!CollectionOperations.containsValues(dto.getTitle())) {
            System.out.println("AAAA");
        }

        if (!StringUtil.valueExists(dto.getDoi())) {
            System.out.println("NoDoiPresent");
        }

        if (dto instanceof ThesisResponseDTO) {
            System.out.println("THESIS");
        }
    }

    private void assessEntity(PersonalInfoDTO dto, EntityRevision entityRevision) {
        if (!StringUtil.valueExists(dto.getOrcid())) {
            System.out.println("OrcidIsRecommended");
        }
    }
}
