package rs.teslaris.reporting.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

public class NetworkStructure {

    @Getter
    private final Set<Integer> allAuthorIds = new HashSet<>();

    private final Map<Integer, Integer> authorDepths = new HashMap<>();

    private final Map<Integer, Integer> authorDegrees = new HashMap<>();

    @Getter
    private final List<Connection> connections = new ArrayList<>();

    @Getter
    private final Integer rootAuthorId;


    public NetworkStructure(Integer rootAuthorId) {
        this.rootAuthorId = rootAuthorId;
    }

    public void addAuthor(Integer authorId, int depth) {
        if (allAuthorIds.add(authorId)) {
            authorDepths.putIfAbsent(authorId, depth);
            authorDegrees.putIfAbsent(authorId, 0); // Initialize degree
        }
    }

    public void addConnection(Integer sourceAuthorId, Integer targetAuthorId,
                              Long publicationCount) {
        if (connectionExists(sourceAuthorId, targetAuthorId) ||
            connectionExists(rootAuthorId, targetAuthorId)) {
            return;
        }

        connections.add(new Connection(sourceAuthorId, targetAuthorId, publicationCount));

        authorDegrees.merge(sourceAuthorId, 1, Integer::sum);
        authorDegrees.merge(targetAuthorId, 1, Integer::sum);
    }

    private boolean connectionExists(Integer sourceAuthorId, Integer targetAuthorId) {
        return connections.stream().anyMatch(conn ->
            (conn.getSourceAuthorId().equals(sourceAuthorId) &&
                conn.getTargetAuthorId().equals(targetAuthorId)) ||
                (conn.getSourceAuthorId().equals(targetAuthorId) &&
                    conn.getTargetAuthorId().equals(sourceAuthorId))
        );
    }

    public int getDepth(Integer authorId) {
        return authorDepths.getOrDefault(authorId, -1);
    }

    public int getDegree(Integer authorId) {
        return authorDegrees.getOrDefault(authorId, 0);
    }
}
