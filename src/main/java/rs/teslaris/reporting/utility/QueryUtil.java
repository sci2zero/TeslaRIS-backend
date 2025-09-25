package rs.teslaris.reporting.utility;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;

@Component
public class QueryUtil {

    private static OrganisationUnitOutputConfigurationService
        organisationUnitOutputConfigurationService;

    private static OrganisationUnitService organisationUnitService;


    @Autowired
    public QueryUtil(
        OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
        OrganisationUnitService organisationUnitService) {
        QueryUtil.organisationUnitOutputConfigurationService =
            organisationUnitOutputConfigurationService;
        QueryUtil.organisationUnitService = organisationUnitService;
    }

    public static Query organisationUnitMatchQuery(List<Integer> organisationUnitIds,
                                                   List<String> searchFields) {
        return Query.of(q -> q.bool(b -> {
            for (String field : searchFields) {
                for (Integer id : organisationUnitIds) {
                    b.should(s -> s.term(t -> t.field(field).value(id)));
                }
            }
            return b.minimumShouldMatch("1");
        }));
    }

    public static List<String> getOrganisationUnitOutputSearchFields(Integer organisationUnitId) {
        var fields = new ArrayList<String>();

        var outputConfiguration =
            organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
                organisationUnitId);

        if (!outputConfiguration.showOutputs()) {
            return Collections.emptyList();
        }

        if (outputConfiguration.showBySpecifiedAffiliation()) {
            fields.add("organisation_unit_ids_specified");
        }

        if (outputConfiguration.showByPublicationYearEmployments()) {
            fields.add("organisation_unit_ids_year_of_publication");
        }

        if (outputConfiguration.showByCurrentEmployments()) {
            fields.add("organisation_unit_ids_active");
        }

        return fields;
    }

    public static List<Integer> getAllMergedOrganisationUnitIds(Integer organisationUnitId) {
        var result = new ArrayList<>(List.of(organisationUnitId));
        result.addAll(organisationUnitService.findOne(organisationUnitId).getMergedIds());

        return result;
    }
}
