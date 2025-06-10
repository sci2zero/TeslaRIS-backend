package rs.teslaris.importer.model.converter.harvest;

import java.util.Optional;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.importer.model.common.DocumentImport;

public class CSVConverter {

    /**
     * Converts a row from a CSV containing publication metadata into a standard internal format.
     *
     * <p>This method assumes the input CSV has columns in a fixed order, where each column maps to
     * specific metadata about a publication. Below is a description of each column index and the type
     * of data it holds, with anonymized examples:</p>
     *
     * <ul>
     *   <li><b>[0]</b> Publication Type - "Article", "Conference paper"</li>
     *   <li><b>[1]</b> Authors - Author name variants that they used in publication </li>
     *   <li><b>[2]</b> Author Full Names with Identifier - "Smith, John (12345678900); Doe, Jane (09876543210)"</li>
     *   <li><b>[3]</b> Author Full Names with Affiliations - "Smith J., University of Something, City; Jane D., University, City"</li>
     *   <li><b>[4]</b> Title - "Integration of a system with an OAI-PMH compatible repository"</li>
     *   <li><b>[5]</b> Year - "2012"</li>
     *   <li><b>[6]</b> Volume - "56"</li>
     *   <li><b>[7]</b> Issue - "2"</li>
     *   <li><b>[8]</b> Start Page - "104"</li>
     *   <li><b>[9]</b> End Page - "112"</li>
     *   <li><b>[10]</b> Pages - if you want to supply page range, free string format</li>
     *   <li><b>[11]</b> DOI</li>
     *   <li><b>[12]</b> Link / URL</li>
     *   <li><b>[13]</b> Abstract</li>
     *   <li><b>[14]</b> Author supplied keywords, separated by "; "</li>
     *   <li><b>[15]</b> Conference name</li>
     *   <li><b>[16]</b> Journal Name</li>
     *   <li><b>[17]</b> ISSN</li>
     *   <li><b>[18]</b> ISBN</li>
     *   <li><b>[19]</b> ConfID</li>
     * </ul>
     */
    public static Optional<DocumentImport> toCommonImportModel(String[] record) {
        var document = new DocumentImport();

        if (record[0].equals("Article")) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
        } else if (record[0].equals("Conference paper")) {
            document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        } else {
            return Optional.empty();
        }


        return Optional.of(document);
    }
}
