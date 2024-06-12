package rs.teslaris.core.converter.person;

import java.util.Objects;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.person.Contact;

public class ContactConverter {

    public static ContactDTO toDTO(Contact contact) {
        var dto = new ContactDTO();

        if (Objects.nonNull(contact)) {
            dto.setContactEmail(contact.getContactEmail());
            dto.setPhoneNumber(contact.getPhoneNumber());
        }

        return dto;
    }

    public static Contact fromDTO(ContactDTO contactDTO) {
        var contact = new Contact();
        contact.setContactEmail(contactDTO.getContactEmail());
        contact.setPhoneNumber(contactDTO.getPhoneNumber());

        return contact;
    }
}
