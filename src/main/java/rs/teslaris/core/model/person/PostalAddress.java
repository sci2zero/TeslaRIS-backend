package rs.teslaris.core.model.person;

import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.util.Set;

public class PostalAddress {
    Country country;
    Set<MultiLingualContent> streetAndNumber;
    Set<MultiLingualContent> city;
}
