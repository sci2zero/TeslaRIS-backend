package rs.teslaris.core.util.visibility;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import rs.teslaris.core.util.session.SessionUtil;

public final class PublicVisibilityQueries {

    private PublicVisibilityQueries() {
    }

    public static Query publiclyVisible() {
        return TermQuery.of(t -> t.field(PublicVisibility.INDEX_FIELD).value(true))._toQuery();
    }

    public static Query approved() {
        return TermQuery.of(t -> t.field("is_approved").value(true))._toQuery();
    }

    public static Query restrictForAnonymous(Query query) {
        if (!PublicVisibility.isAnonymousRequest()) {
            return query;
        }

        return BoolQuery.of(b -> b.must(query).filter(publiclyVisible()))._toQuery();
    }

    public static Query documentVisibilityForCurrentUser() {
        return SessionUtil.isUserLoggedIn() ? approved() : publiclyVisible();
    }
}
