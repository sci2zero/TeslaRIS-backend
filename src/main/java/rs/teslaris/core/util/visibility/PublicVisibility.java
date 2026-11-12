package rs.teslaris.core.util.visibility;

import java.util.Objects;
import org.springframework.web.context.request.RequestContextHolder;
import rs.teslaris.core.indexmodel.PubliclyVisibleIndex;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.util.session.SessionUtil;

public final class PublicVisibility {

    public static final String INDEX_FIELD = "is_publicly_visible";

    private PublicVisibility() {
    }

    public static boolean isCandidate(Boolean publicationCandidate) {
        return !Boolean.FALSE.equals(publicationCandidate);
    }

    public static boolean isPubliclyVisible(Document document) {
        if (!ApproveStatus.APPROVED.equals(document.getApproveStatus())) {
            return false;
        }

        if (document instanceof Thesis thesis &&
            Boolean.TRUE.equals(thesis.getIsOnPublicReview())) {
            return true;
        }

        return isCandidate(document.getPublicationCandidate());
    }

    public static boolean isPubliclyVisible(ApproveStatus approveStatus,
                                            Boolean publicationCandidate) {
        return ApproveStatus.APPROVED.equals(approveStatus) && isCandidate(publicationCandidate);
    }

    public static boolean isPubliclyVisible(Boolean publicationCandidate) {
        return isCandidate(publicationCandidate);
    }

    /**
     * Background work has no bound request, so it can always read hidden records.
     */
    public static boolean isAnonymousRequest() {
        return Objects.nonNull(RequestContextHolder.getRequestAttributes()) &&
            !SessionUtil.isUserLoggedIn();
    }

    public static boolean isHiddenFromCurrentUser(Boolean publicationCandidate) {
        return isAnonymousRequest() && !isCandidate(publicationCandidate);
    }

    public static boolean isVisibleToCurrentUser(Object index) {
        return SessionUtil.isUserLoggedIn() || !(index instanceof PubliclyVisibleIndex visible) ||
            Boolean.TRUE.equals(visible.getIsPubliclyVisible());
    }
}
