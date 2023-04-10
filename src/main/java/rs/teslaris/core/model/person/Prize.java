package rs.teslaris.core.model.person;

import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.time.LocalDate;
import java.util.Set;

public class Prize extends BaseEntity {
    Set<MultiLingualContent> title;
    Set<MultiLingualContent> description;
    Set<DocumentFile> proofs;
    LocalDate date;
}
