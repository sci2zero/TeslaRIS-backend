package rs.teslaris.reporting.utility;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.functional.Pair;

@Component
public class QueryUtil {

    public static final int NUMBER_OF_WORLD_COUNTRIES = 195;
    private static OrganisationUnitOutputConfigurationService
        organisationUnitOutputConfigurationService;
    private static OrganisationUnitService organisationUnitService;
    private static UserService userService;
    // 195 countries exist at the moment, we can change this if need be


    @Autowired
    public QueryUtil(
        OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService,
        OrganisationUnitService organisationUnitService, UserService userService) {
        QueryUtil.organisationUnitOutputConfigurationService =
            organisationUnitOutputConfigurationService;
        QueryUtil.organisationUnitService = organisationUnitService;
        QueryUtil.userService = userService;
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

    public static Pair<Integer, Integer> constructYearRange(Integer startYear, Integer endYear) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var currentYear = LocalDate.now().getYear();
            return new Pair<>(currentYear, currentYear - 10);
        }

        return new Pair<>(startYear, endYear);
    }

    public static Set<Pair<Integer, Set<MultiLingualContent>>> fetchCommissionsForOrganisationUnit(
        Integer organisationUnitId) {
        var commissions = new HashSet<Pair<Integer, Set<MultiLingualContent>>>();
        userService.findCommissionForOrganisationUnitId(organisationUnitId).forEach(commission -> {
            commissions.add(new Pair<>(commission.getId(), commission.getDescription()));
        });

        return commissions;
    }

    public static void applyAllowedThesisTypesFilter(
        BoolQuery.Builder boolBuilder,
        List<ThesisType> allowedThesisTypes
    ) {
        if (Objects.nonNull(allowedThesisTypes) && !allowedThesisTypes.isEmpty()) {
            boolBuilder.must(m -> m.bool(innerBool -> innerBool
                .should(sh -> sh.terms(t -> t
                    .field("publication_type")
                    .terms(v -> v.value(
                        allowedThesisTypes.stream()
                            .map(ThesisType::name)
                            .map(FieldValue::of)
                            .toList()
                    ))
                ))
                .minimumShouldMatch("1")
            ));
        }
    }
}
