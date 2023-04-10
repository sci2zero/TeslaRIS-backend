package rs.teslaris.core.model.person;

import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.util.Set;

public class Employment extends Involvement{
    Position position;
    Set<MultiLingualContent> title;
    Set<MultiLingualContent> role;
}
