package rs.teslaris.core.model.person;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Contact {

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Contact contact = (Contact) o;
        return contactEmail.equals(contact.contactEmail) && phoneNumber.equals(contact.phoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactEmail, phoneNumber);
    }
}
