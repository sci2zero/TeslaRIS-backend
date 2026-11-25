package rs.teslaris.revisioner.util.dataquality;

import java.util.Comparator;

/**
 * Orders profile versions by numeric segment, thus 1.10.0 follows 1.9.0. A plain string order puts
 * it before, which makes the newest profile version the wrong one.
 */
public class ProfileVersionComparator implements Comparator<String> {

    public static final ProfileVersionComparator INSTANCE = new ProfileVersionComparator();

    private static final String SEGMENT_SEPARATOR = "\\.";


    @Override
    public int compare(String left, String right) {
        var leftSegments = left.split(SEGMENT_SEPARATOR);
        var rightSegments = right.split(SEGMENT_SEPARATOR);

        for (var i = 0; i < Math.max(leftSegments.length, rightSegments.length); i++) {
            var leftSegment = i < leftSegments.length ? leftSegments[i] : "";
            var rightSegment = i < rightSegments.length ? rightSegments[i] : "";

            var comparison = compareSegments(leftSegment, rightSegment);

            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

    private int compareSegments(String leftSegment, String rightSegment) {
        if (isNumeric(leftSegment) && isNumeric(rightSegment)) {
            return Long.compare(Long.parseLong(leftSegment), Long.parseLong(rightSegment));
        }

        return leftSegment.compareTo(rightSegment);
    }

    private boolean isNumeric(String segment) {
        if (segment.isEmpty() || segment.length() > 18) {
            return false;
        }

        return segment.chars().allMatch(Character::isDigit);
    }
}
