package rs.teslaris.core.applicationevent;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PersonContributionsChangeEvent {

    private Set<Integer> personIds;
}
