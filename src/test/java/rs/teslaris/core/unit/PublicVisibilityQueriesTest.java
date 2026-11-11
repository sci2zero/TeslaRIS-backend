package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.util.visibility.PublicVisibility;
import rs.teslaris.core.util.visibility.PublicVisibilityQueries;

public class PublicVisibilityQueriesTest {

    private static final Query QUERY = MatchAllQuery.of(m -> m)._toQuery();

    private static void bindRequest() {
        RequestContextHolder.setRequestAttributes(
            new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private static void logIn() {
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken("user", "password", "ROLE_USER"));
    }

    @AfterEach
    void clearContexts() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFilterOnPubliclyVisibleField() {
        var query = PublicVisibilityQueries.publiclyVisible();

        assertTrue(query.isTerm());
        assertEquals(PublicVisibility.INDEX_FIELD, query.term().field());
        assertTrue(query.term().value().booleanValue());
    }

    @Test
    void shouldLeaveQueryUnchangedForBackgroundWork() {
        assertSame(QUERY, PublicVisibilityQueries.restrictForAnonymous(QUERY));
    }

    @Test
    void shouldLeaveQueryUnchangedForLoggedInUser() {
        // Given
        bindRequest();
        logIn();

        // When / Then
        assertSame(QUERY, PublicVisibilityQueries.restrictForAnonymous(QUERY));
    }

    @Test
    void shouldRestrictQueryForAnonymousRequest() {
        // Given
        bindRequest();

        // When
        var restricted = PublicVisibilityQueries.restrictForAnonymous(QUERY);

        // Then
        assertTrue(restricted.isBool());
        assertSame(QUERY, restricted.bool().must().getFirst());
        assertEquals(PublicVisibility.INDEX_FIELD,
            restricted.bool().filter().getFirst().term().field());
    }

    @Test
    void shouldFilterStatisticsOnApprovalForLoggedInUser() {
        // Given
        logIn();

        // When
        var query = PublicVisibilityQueries.documentVisibilityForCurrentUser();

        // Then
        assertEquals("is_approved", query.term().field());
    }

    @Test
    void shouldFilterStatisticsOnPublicVisibilityForAnonymousUser() {
        // When
        var query = PublicVisibilityQueries.documentVisibilityForCurrentUser();

        // Then
        assertEquals(PublicVisibility.INDEX_FIELD, query.term().field());
    }
}
