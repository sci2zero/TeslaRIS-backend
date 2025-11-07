package rs.teslaris.exporter.model.converter.skgif;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import rs.teslaris.core.model.skgif.common.SKGIFAccessRights;
import rs.teslaris.core.model.skgif.researchproduct.SKGIFContribution;
import rs.teslaris.core.model.skgif.venue.Venue;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportPublicationType;

public class VenueConverter extends BaseConverter {

    public static List<Venue> toSKGIF(ExportDocument document) {
        var venue = new Venue();

        venue.setLocalIdentifier(IdentifierUtil.identifierPrefix + document.getDatabaseId());

        venue.setEntityType("venue");
        venue.setType(getEntityType(document.getType()));

        populateIdentifiers(venue.getIdentifiers(), document);

        venue.setTitle(extractTitleFromMC(document.getTitle()));

        if (Objects.nonNull(document.getAcronym()) && !document.getAcronym().isEmpty()) {
            venue.setAcronym(extractTitleFromMC(document.getAcronym()));
        }

        venue.setSeries(getSeriesName(document));
        venue.setCreationDate(document.getDocumentDate());
        venue.setAccessRights(new SKGIFAccessRights(document.getOpenAccess() ? "open" : "closed",
            document.getOpenAccess() ? "Open Access" : "Closed Access"));

        venue.setContributions(new ArrayList<>());
        document.getPublishers().forEach(publisher -> {
            var contribution = new SKGIFContribution();
            contribution.setBy(extractTitleFromMC(publisher.getName()));
            contribution.setRole("publisher");

            venue.getContributions().add(contribution);
        });

        document.getEditors().forEach(editor -> {
            var contribution = new SKGIFContribution();
            contribution.setBy(editor.getDisplayName());
            contribution.setRole("editor");

            venue.getContributions().add(contribution);
        });

        document.getBoardMembers().forEach(boardMember -> {
            var contribution = new SKGIFContribution();
            contribution.setBy(boardMember.getDisplayName());
            contribution.setRole("scientific board member");

            venue.getContributions().add(contribution);
        });

        return List.of(venue);
    }

    private static String getEntityType(ExportPublicationType type) {
        return switch (type) {
            case PROCEEDINGS -> "conference";
            case MONOGRAPH -> "book";
            case JOURNAL -> "journal";
            default -> throw new IllegalArgumentException("No venue type for: " + type.name());
        };
    }

    private static String getSeriesName(ExportDocument document) {
        return switch (document.getType()) {
            case PROCEEDINGS -> Objects.nonNull(document.getEvent()) ?
                extractTitleFromMC(document.getEvent().getName()) : "";
            case MONOGRAPH -> (Objects.nonNull(document.getPublishers()) &&
                !document.getPublishers().isEmpty()) ?
                extractTitleFromMC(document.getPublishers().getFirst().getName()) : "";
            default -> "";
        };
    }
}
