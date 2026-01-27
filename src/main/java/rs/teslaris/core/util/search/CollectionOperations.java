package rs.teslaris.core.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionOperations {

    public static <T> boolean containsValues(Collection<T> collection) {
        return Objects.nonNull(collection) && !collection.isEmpty();
    }

    public static <T> List<T> getIntersection(List<T> list1, List<T> list2) {
        if (!containsValues(list1) && !containsValues(list2)) {
            return Collections.emptyList();
        }

        if (!containsValues(list1)) {
            return list2;
        }

        if (!containsValues(list2)) {
            return list1;
        }

        var set = new HashSet<>(list1);
        set.retainAll(list2);

        return new ArrayList<>(set);
    }

    public static boolean hasCaseInsensitiveMatch(Set<String> set1, Set<String> set2) {
        if (Objects.isNull(set1) || Objects.isNull(set2)) {
            return false;
        }

        var lowerSet1 = set1.stream()
            .filter(Objects::nonNull)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        var lowerSet2 = set2.stream()
            .filter(Objects::nonNull)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return !Collections.disjoint(lowerSet1, lowerSet2);
    }
}
