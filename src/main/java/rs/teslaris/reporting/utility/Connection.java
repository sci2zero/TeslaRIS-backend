package rs.teslaris.reporting.utility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Connection {

    private final Integer sourceAuthorId;

    private final Integer targetAuthorId;

    private final Long publicationCount;
}
