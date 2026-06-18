package rs.teslaris.revisioner.hydrator;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;

@Component
public class ProceedingsPublicationRevisionHydrator
    extends RevisionHydrator<ProceedingsPublicationDTO> {

    private final ProceedingsService proceedingsService;


    @Autowired
    public ProceedingsPublicationRevisionHydrator(
        CountryService countryService, ProceedingsService proceedingsService) {
        super(countryService);
        this.proceedingsService = proceedingsService;
    }

    @Override
    public String entityType() {
        return DocumentPublicationType.PROCEEDINGS_PUBLICATION.name();
    }

    @Override
    @Transactional(readOnly = true)
    public void hydrate(ProceedingsPublicationDTO dto) {
        hydrateCommonFields(dto);

        dto.setProceedingsName(MultilingualContentConverter.getMultilingualContentDTO(proceedingsService.findProceedingsById(dto.getProceedingsId()).getTitle()));
    }
}
