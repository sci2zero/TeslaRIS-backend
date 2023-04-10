package rs.teslaris.core.model.project;

import rs.teslaris.core.model.commontypes.MultiLingualContent;

import java.time.LocalDate;
import java.util.Set;

public class Funding {
    String fundingNumber,
    DocumentFile agreement,
    Set<MultiLingualContent> fundingCall,
    Set<MultiLingualContent> fundingProgram,
    Set<MultiLingualContent> fundingAgency,
    MonetaryAmount grant,
    LocalDate from,
    LocalDate to,
}
