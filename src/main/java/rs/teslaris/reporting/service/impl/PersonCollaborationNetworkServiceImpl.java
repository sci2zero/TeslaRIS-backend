package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CollaborationLink;
import rs.teslaris.reporting.dto.CollaborationNetworkDTO;
import rs.teslaris.reporting.dto.PersonNode;
import rs.teslaris.reporting.service.interfaces.PersonCollaborationNetworkService;
import rs.teslaris.reporting.utility.NetworkStructure;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonCollaborationNetworkServiceImpl implements PersonCollaborationNetworkService {

    private final ElasticsearchClient elasticsearchClient;

    private final PersonIndexRepository personIndexRepository;


    @Override
    public CollaborationNetworkDTO findCollaborationNetwork(Integer authorId, int depth) {
        try {
            if (depth < 1 || depth > 3) {
                throw new IllegalArgumentException("Depth must be between 1 and 3");
            }

            var networkStructure = buildNetworkStructure(authorId, depth);

            Map<Integer, PersonIndex> personMap =
                fetchPersonDetails(networkStructure.getAllAuthorIds());

            var nodes = buildNodes(networkStructure, personMap);
            var links = buildLinksWithPublicationCounts(networkStructure);

            return new CollaborationNetworkDTO(nodes, links);
        } catch (Exception e) {
            log.error(
                "Failed to build collaboration network for author {} with depth {}. Reason: {}",
                authorId, depth, e.getMessage());

            return new CollaborationNetworkDTO(List.of(), List.of());
        }
    }

    private NetworkStructure buildNetworkStructure(Integer authorId, int depth) {
        var structure = new NetworkStructure(authorId);

        structure.addAuthor(authorId, 0);
        buildNetworkLevel(structure, authorId, 1, depth);

        return structure;
    }

    private void buildNetworkLevel(NetworkStructure structure, Integer authorId, int currentDepth,
                                   int maxDepth) {
        if (currentDepth > maxDepth) {
            return;
        }

        List<Pair<Integer, Long>> collaborators = findTopCollaborators(authorId);

        for (Pair<Integer, Long> collaborator : collaborators) {
            var collaboratorId = collaborator.a;
            var publicationCount = collaborator.b;

            structure.addAuthor(collaboratorId, currentDepth);
            structure.addConnection(authorId, collaboratorId, publicationCount);
        }

        for (Pair<Integer, Long> collaborator : collaborators) {
            if (currentDepth < maxDepth) {
                var collaboratorId = collaborator.a;
                buildNetworkLevel(structure, collaboratorId, currentDepth + 1, maxDepth);
            }
        }
    }

    private List<Pair<Integer, Long>> findTopCollaborators(Integer authorId) {
        try {
            var response = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .term(t -> t
                            .field("author_ids")
                            .value(authorId)
                        )
                    )
                    .aggregations("coauthors", a -> a
                        .terms(t -> t
                            .field("author_ids")
                            .size(100)
                            .minDocCount(1)
                        )
                    ),
                Void.class
            );

            var termsAgg = response.aggregations()
                .get("coauthors")
                .lterms();

            if (Objects.isNull(termsAgg)) {
                return Collections.emptyList();
            }

            return termsAgg.buckets().array().stream()
                .map(b -> new Pair<>((int) b.key(), b.docCount()))
                .filter(p -> p.a > 0)
                .filter(p -> !p.a.equals(authorId))
                .limit(100) // Limit to prevent explosion
                .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Failed to fetch coauthors for author {}", authorId, e);
            return Collections.emptyList();
        }
    }

    private Map<Integer, PersonIndex> fetchPersonDetails(Set<Integer> authorIds) {
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Iterable<PersonIndex> persons =
                personIndexRepository.findByDatabaseIdIn(authorIds.stream().toList(),
                    Pageable.unpaged());

            Map<Integer, PersonIndex> personMap = new HashMap<>();
            for (PersonIndex person : persons) {
                if (Objects.nonNull(person.getDatabaseId())) {
                    personMap.put(person.getDatabaseId(), person);
                }
            }

            return personMap;
        } catch (Exception e) {
            log.error("Failed to fetch person details for authors: {}", authorIds, e);
            return Collections.emptyMap();
        }
    }

    private List<PersonNode> buildNodes(NetworkStructure structure,
                                        Map<Integer, PersonIndex> personMap) {

        var nodes = new ArrayList<PersonNode>();

        for (Integer authorId : structure.getAllAuthorIds()) {
            var person = personMap.get(authorId);
            var name =
                (Objects.nonNull(person) && Objects.nonNull(person.getName())) ? person.getName() :
                    "Researcher " + authorId; // should never return this

            int degree = structure.getDegree(authorId);
            int symbolSize = calculateSymbolSize(degree);
            int depth = structure.getDepth(authorId);

            var node = new PersonNode(
                String.valueOf(authorId),
                name,
                symbolSize,
                degree,
                depth // Use depth as category
            );

            nodes.add(node);
        }

        return nodes;
    }

    private List<CollaborationLink> buildLinksWithPublicationCounts(NetworkStructure structure) {
        var links = new ArrayList<CollaborationLink>();

        for (var connection : structure.getConnections()) {
            var link = new CollaborationLink(
                String.valueOf(connection.getSourceAuthorId()),
                String.valueOf(connection.getTargetAuthorId()),
                connection.getPublicationCount(),
                calculateEdgeWidth(connection.getPublicationCount())
            );

            links.add(link);
        }

        return links;
    }

    private int calculateSymbolSize(int degree) {
        return Math.min(50, degree * 10);
    }

    private int calculateEdgeWidth(long publicationCount) {
        return Math.max(1, Math.min(8, (int) (publicationCount / 2)));
    }
}
