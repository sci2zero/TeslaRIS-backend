package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.util.visibility.PublicVisibility;

public class PublicVisibilityTest {

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

    @ParameterizedTest
    @CsvSource({
        "APPROVED, true, true",
        "APPROVED, false, false",
        "APPROVED, , true",
        "REQUESTED, true, false",
        "DECLINED, true, false"
    })
    void shouldCombineApprovalAndCandidacyForDocument(ApproveStatus approveStatus,
                                                      Boolean candidate, boolean expected) {
        // Given
        var document = new JournalPublication();
        document.setApproveStatus(approveStatus);
        document.setPublicationCandidate(candidate);

        // When
        var visible = PublicVisibility.isPubliclyVisible(document);

        // Then
        assertEquals(expected, visible);
    }

    @Test
    void shouldKeepThesisOnPublicReviewVisibleWhenNotCandidate() {
        // Given
        var thesis = new Thesis();
        thesis.setApproveStatus(ApproveStatus.APPROVED);
        thesis.setPublicationCandidate(false);
        thesis.setIsOnPublicReview(true);

        // When / Then
        assertTrue(PublicVisibility.isPubliclyVisible(thesis));
    }

    @Test
    void shouldHideThesisOffPublicReviewWhenNotCandidate() {
        // Given
        var thesis = new Thesis();
        thesis.setApproveStatus(ApproveStatus.APPROVED);
        thesis.setPublicationCandidate(false);
        thesis.setIsOnPublicReview(false);

        // When / Then
        assertFalse(PublicVisibility.isPubliclyVisible(thesis));
    }

    @Test
    void shouldNotExemptUnapprovedThesisOnPublicReview() {
        // Given
        var thesis = new Thesis();
        thesis.setApproveStatus(ApproveStatus.REQUESTED);
        thesis.setIsOnPublicReview(true);

        // When / Then
        assertFalse(PublicVisibility.isPubliclyVisible(thesis));
    }

    @ParameterizedTest
    @CsvSource({
        "APPROVED, true, true",
        "APPROVED, false, false",
        "REQUESTED, true, false"
    })
    void shouldCombineApprovalAndCandidacyForApprovableEntity(ApproveStatus approveStatus,
                                                              Boolean candidate,
                                                              boolean expected) {
        assertEquals(expected, PublicVisibility.isPubliclyVisible(approveStatus, candidate));
    }

    @Test
    void shouldTreatMissingCandidacyAsCandidate() {
        assertTrue(PublicVisibility.isPubliclyVisible((Boolean) null));
        assertTrue(PublicVisibility.isPubliclyVisible(true));
        assertFalse(PublicVisibility.isPubliclyVisible(false));
    }

    @Test
    void shouldNotTreatBackgroundWorkAsAnonymousRequest() {
        // When / Then
        assertFalse(PublicVisibility.isAnonymousRequest());
        assertFalse(PublicVisibility.isHiddenFromCurrentUser(false));
    }

    @Test
    void shouldTreatRequestWithoutLoginAsAnonymous() {
        // Given
        bindRequest();

        // When / Then
        assertTrue(PublicVisibility.isAnonymousRequest());
        assertTrue(PublicVisibility.isHiddenFromCurrentUser(false));
        assertFalse(PublicVisibility.isHiddenFromCurrentUser(true));
        assertFalse(PublicVisibility.isHiddenFromCurrentUser(null));
    }

    @Test
    void shouldNotHideFromLoggedInUser() {
        // Given
        bindRequest();
        logIn();

        // When / Then
        assertFalse(PublicVisibility.isAnonymousRequest());
        assertFalse(PublicVisibility.isHiddenFromCurrentUser(false));
    }

    @Test
    void shouldHideInvisibleIndexFromAnonymousUserOnly() {
        // Given
        var hidden = new PersonIndex();
        hidden.setIsPubliclyVisible(false);

        // When / Then
        assertFalse(PublicVisibility.isVisibleToCurrentUser(hidden));
        assertTrue(PublicVisibility.isVisibleToCurrentUser(new PersonIndex()));
        assertTrue(PublicVisibility.isVisibleToCurrentUser("not an index"));

        logIn();
        assertTrue(PublicVisibility.isVisibleToCurrentUser(hidden));
    }
}
