package rs.teslaris.core.util.deduplication;

import java.util.Set;

public interface Mergeable {

    Set<Integer> getMergedIds();

    void setMergedIds(Set<Integer> mergedIds);

    Set<Integer> getOldIds();
}
