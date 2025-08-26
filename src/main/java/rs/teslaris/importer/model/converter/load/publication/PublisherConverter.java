package rs.teslaris.importer.model.converter.load.publication;

import java.util.Arrays;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PublishableDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.oaipmh.publication.Publisher;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;

@Component
@RequiredArgsConstructor
public class PublisherConverter {

    private final PublisherService publisherService;

    private final CountryService countryService;

    private final MultilingualContentConverter multilingualContentConverter;


    public void setPublisherInformation(Publisher publisher, PublishableDTO dto) {
        for (var mcName : publisher.getName()) {
            var name = mcName.getValue();
            var potentialMatches = publisherService.searchPublishers(
                Arrays.stream(name.split(" ")).filter(n -> !n.isBlank()).toList(),
                PageRequest.of(0, 1));
            if (potentialMatches.hasContent()) {
                var match = potentialMatches.getContent().getFirst();
                if (match.getNameSr().equals(name) || match.getNameOther().equals(name)) {
                    dto.setPublisherId(match.getDatabaseId());
                    break;
                }
            }
        }

        if (Objects.isNull(dto.getPublisherId())) {
            var publisherDTO = new PublisherDTO();
            publisherDTO.setName(multilingualContentConverter.toDTO(publisher.getName()));
            publisherDTO.setPlace(multilingualContentConverter.toDTO(publisher.getName()));

            for (var stateName : publisher.getState()) {
                var match = countryService.findCountryByName(stateName.getValue());
                if (match.isPresent()) {
                    publisherDTO.setCountryId(match.get().getId());
                    break;
                }
            }

            dto.setPublisherId(publisherService.createPublisher(publisherDTO, true).getId());
        }
    }
}
