package rs.teslaris.core.converter.person;

import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.person.Contact;

public class ContactConverter {

    public static ContactDTO toDTO(Contact contact) {
        return new ContactDTO(contact.getContactEmail(), contact.getPhoneNumber());
    }
}
