package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.PublisherDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedPublishersDTO {

    private PublisherDTO leftPublisher;

    private PublisherDTO rightPublisher;
}
