package rs.teslaris.revisioner.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.revisioner.hydrator.RevisionHydrator;

@Component
public class RevisionHydratorRegistry {

    private final Map<String, RevisionHydrator<?>> hydrators;

    private final Map<String, Class<?>> dtoClasses;

    {
        dtoClasses =
            Map.ofEntries(Map.entry(DocumentPublicationType.DATASET.name(), DatasetDTO.class),
                Map.entry(DocumentPublicationType.INTANGIBLE_PRODUCT.name(),
                    IntangibleProductDTO.class),
                Map.entry(DocumentPublicationType.THESIS.name(), ThesisResponseDTO.class),
                Map.entry(DocumentPublicationType.PROCEEDINGS.name(), ProceedingsResponseDTO.class),
                Map.entry(DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(),
                    ProceedingsPublicationDTO.class),
                Map.entry(DocumentPublicationType.PATENT.name(), PatentDTO.class),
                Map.entry(DocumentPublicationType.MATERIAL_PRODUCT.name(),
                    MaterialProductDTO.class),
                Map.entry(DocumentPublicationType.GENETIC_MATERIAL.name(),
                    GeneticMaterialDTO.class),
                Map.entry(DocumentPublicationType.MONOGRAPH.name(), MonographDTO.class),
                Map.entry(DocumentPublicationType.MONOGRAPH_PUBLICATION.name(),
                    MonographPublicationDTO.class),
                Map.entry(DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    JournalPublicationResponseDTO.class),
                Map.entry(DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT.name(),
                    PerformanceRelatedOutputDTO.class));
    }


    public RevisionHydratorRegistry(List<RevisionHydrator<?>> hydratorList) {
        this.hydrators =
            hydratorList.stream()
                .collect(Collectors.toMap(RevisionHydrator::entityType, Function.identity()));
    }

    public Optional<RevisionHydrator<?>> get(String entityType) {
        return Optional.ofNullable(hydrators.get(entityType));
    }

    public Class<?> getDtoClass(String entityType) {
        return dtoClasses.get(entityType);
    }
}
