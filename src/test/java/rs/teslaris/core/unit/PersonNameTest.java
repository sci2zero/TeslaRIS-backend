package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import rs.teslaris.core.model.person.PersonName;

public class PersonNameTest {

    private static PersonName name(String firstname, String otherName, String lastname) {
        var personName = new PersonName();
        personName.setFirstname(firstname);
        personName.setOtherName(otherName);
        personName.setLastname(lastname);
        return personName;
    }

    @Test
    public void shouldFormatFullName() {
        assertEquals("Ivan Radomir Mrsulja",
            name("Ivan", "Radomir", "Mrsulja").toString());
        assertEquals("Ivan (Radomir) Mrsulja",
            name("Ivan", "Radomir", "Mrsulja").toText());
    }

    @Test
    public void shouldFormatNameWithoutOtherName() {
        assertEquals("Ivan Mrsulja", name("Ivan", null, "Mrsulja").toString());
        assertEquals("Ivan Mrsulja", name("Ivan", "", "Mrsulja").toText());
    }

    @Test
    public void shouldSkipMissingParts() {
        assertEquals("Vasco Manuel Thudichum de Serpa Vasconcelos",
            name(null, null, "Vasco Manuel Thudichum de Serpa Vasconcelos").toString());
        assertEquals("Vasco Manuel Thudichum de Serpa Vasconcelos",
            name("  ", null, "Vasco Manuel Thudichum de Serpa Vasconcelos").toText());
        assertEquals("Ivan", name("Ivan", null, null).toString());
        assertEquals("Ivan (Radomir)", name("Ivan", "Radomir", null).toText());
        assertEquals("", name(null, null, null).toString());
    }
}
