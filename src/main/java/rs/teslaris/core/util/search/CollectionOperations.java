package rs.teslaris.core.util.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
}
