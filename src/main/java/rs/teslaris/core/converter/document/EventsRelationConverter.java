package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.model.document.EventsRelation;

public class EventsRelationConverter {

    public static EventsRelationDTO toDTO(EventsRelation eventsRelation) {
        var relationDTO = new EventsRelationDTO();

        relationDTO.setId(eventsRelation.getId());
        relationDTO.setSourceId(eventsRelation.getSource().getId());
        relationDTO.setTargetId(eventsRelation.getTarget().getId());
        relationDTO.setEventsRelationType(eventsRelation.getEventsRelationType());

        relationDTO.setSourceEventName(MultilingualContentConverter.getMultilingualContentDTO(
            eventsRelation.getSource().getName()));
        relationDTO.setTargetEventName(MultilingualContentConverter.getMultilingualContentDTO(
            eventsRelation.getTarget().getName()));

        return relationDTO;
    }
}
