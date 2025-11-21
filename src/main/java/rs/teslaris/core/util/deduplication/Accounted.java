package rs.teslaris.core.util.deduplication;

import java.util.Set;

public interface Accounted {

    Set<String> getAccountingIds();

    void setAccountingIds(Set<String> accountingIds);
}
