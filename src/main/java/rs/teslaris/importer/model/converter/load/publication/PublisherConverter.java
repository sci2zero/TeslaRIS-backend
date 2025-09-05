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
import rs.teslaris.core.util.language.SerbianTransliteration;
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
            if (Objects.isNull(name) || name.isBlank()) {
                return;
            }

            name = name.trim();

            var potentialMatches = publisherService.searchPublishers(
                Arrays.stream(name.split(" ")).filter(n -> !n.isBlank()).toList(),
                PageRequest.of(0, 1));
            if (potentialMatches.hasContent()) {
                var match = potentialMatches.getContent().getFirst();
                var cyrillicName = SerbianTransliteration.toCyrillic(name);
                if (match.getNameSr().equalsIgnoreCase(name) ||
                    match.getNameOther().equalsIgnoreCase(name) ||
                    match.getNameSr().equalsIgnoreCase(cyrillicName) ||
                    match.getNameOther().equalsIgnoreCase(cyrillicName)) {
                    dto.setPublisherId(match.getDatabaseId());
                    return;
                }
            }
        }

        if (Objects.isNull(dto.getPublisherId())) {
            var publisherDTO = new PublisherDTO();
            publisherDTO.setName(multilingualContentConverter.toDTO(publisher.getName()));
            publisherDTO.setPlace(multilingualContentConverter.toDTO(publisher.getPlace()));

            for (var stateName : publisher.getState()) {
                var match = countryService.findCountryByName(stateName.getValue());
                if (match.isPresent()) {
                    publisherDTO.setCountryId(match.get().getId());
                    break;
                }
            }

            if (Objects.nonNull(publisherDTO.getName()) && !publisherDTO.getName().isEmpty() &&
                publisherDTO.getName().stream().noneMatch(
                    name -> Objects.isNull(name.getContent()) || name.getContent().isBlank())) {
                dto.setPublisherId(publisherService.createPublisher(publisherDTO, true).getId());
            }
        }
    }
}
