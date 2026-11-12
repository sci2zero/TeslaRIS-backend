package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.funding.FundingType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fundingExports")
public class ExportFunding extends BaseExportEntity {

    @Field("name")
    private List<ExportMultilingualContent> name = new ArrayList<>();

    @Field("name_abbreviation")
    private List<ExportMultilingualContent> nameAbbreviation = new ArrayList<>();

    @Field("description")
    private List<ExportMultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<ExportMultilingualContent> keywords = new ArrayList<>();

    @Field("doi")
    private String doi;

    @Field("grant_agreement_id")
    private String grantAgreementId;

    @Field("identifiers")
    private List<ExportEntityIdentifier> identifiers = new ArrayList<>();

    @Field("uris")
    private List<String> uris = new ArrayList<>();

    @Field("project")
    private ExportProject project;

    @Field("funder")
    private ExportOrganisationUnit funder;

    @Field("display_funder")
    private List<ExportMultilingualContent> displayFunder = new ArrayList<>();

    @Field("display_call")
    private List<ExportMultilingualContent> displayCall = new ArrayList<>();

    @Field("display_program")
    private List<ExportMultilingualContent> displayProgram = new ArrayList<>();

    @Field("funding_call_name")
    private List<ExportMultilingualContent> fundingCallName = new ArrayList<>();

    @Field("funding_program_name")
    private List<ExportMultilingualContent> fundingProgramName = new ArrayList<>();

    @Field("funding_types")
    private Set<FundingType> fundingTypes = new HashSet<>();

    @Field("amount")
    private ExportMonetaryAmount amount;

    @Field("competitive")
    private Boolean competitive;

    @Field("renewable")
    private Boolean renewable;

    @Field("date_submitted")
    private LocalDate dateSubmitted;

    @Field("date_awarded")
    private LocalDate dateAwarded;

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;

    @Field("oa_mandated")
    private Boolean oaMandated;

    @Field("oa_mandate_url")
    private String oaMandateUrl;

    @Field("internal_investment")
    private Boolean internalInvestment;
}
