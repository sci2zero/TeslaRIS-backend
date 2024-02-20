package rs.teslaris.core.importer.publication;

import rs.teslaris.core.importer.person.Person;

public interface Contributor {
    Person getPerson();

    String getDisplayName();
}