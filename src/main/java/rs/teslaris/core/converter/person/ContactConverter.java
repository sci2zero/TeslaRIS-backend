package rs.teslaris.core.converter.person;

import java.util.Objects;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.model.person.Contact;

public class ContactConverter {

    public static ContactDTO toDTO(Contact contact) {
        var dto = new ContactDTO();

        if (Objects.nonNull(contact)) {
            dto.setContactEmail(contact.getContactEmail());
            dto.setPhoneNumber(contact.getPhoneNumber());
        }

        filterSensitiveData(dto);

        return dto;
    }

    public static Contact fromDTO(ContactDTO contactDTO) {
        var contact = new Contact();
        contact.setContactEmail(contactDTO.getContactEmail());
        contact.setPhoneNumber(contactDTO.getPhoneNumber());

        return contact;
    }

    private static void filterSensitiveData(ContactDTO contactResponse) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (Objects.isNull(authentication) || !authentication.isAuthenticated() ||
            (authentication.getPrincipal() instanceof String &&
                authentication.getPrincipal().equals("anonymousUser"))) {
            contactResponse.setContactEmail("");
            contactResponse.setPhoneNumber("");
        }
    }
}
