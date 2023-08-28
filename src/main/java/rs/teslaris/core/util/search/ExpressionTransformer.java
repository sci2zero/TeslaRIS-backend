package rs.teslaris.core.util.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.springframework.stereotype.Component;

@Component
public class ExpressionTransformer {
    private final Map<String, Integer> priorities = Map.of("AND", 2, "OR", 1, "NOT", 3);

    public List<String> transformToPostFixNotation(List<String> expression) {
        var tokenStack = new Stack<String>();
        var postfixExpression = new ArrayList<String>();

        for (String token : expression) {
            if (!priorities.containsKey(token)) {
                postfixExpression.add(token);
            } else {
                while (!tokenStack.isEmpty() &&
                    priorities.get(token) <= priorities.getOrDefault(tokenStack.peek(), 0)) {
                    postfixExpression.add(tokenStack.pop());
                }
                tokenStack.push(token);
            }
        }

        postfixExpression.addAll(tokenStack);
        return postfixExpression;
    }

    public Query buildQueryFromPostFixExpression(List<String> postfixExpression) {
        var queryStack = new Stack<Query>();

        for (var token : postfixExpression) {
            if (token.equalsIgnoreCase("AND")) {
                var other = queryStack.pop();
                queryStack.push(BoolQuery.of(q -> {
                    q.must(other);
                    q.must(queryStack.pop());
                    return q;
                })._toQuery());
            } else if (token.equalsIgnoreCase("OR")) {
                var other = queryStack.pop();
                queryStack.push(BoolQuery.of(q -> {
                    q.should(other);
                    q.should(queryStack.pop());
                    return q;
                })._toQuery());
            } else if (token.equalsIgnoreCase("NOT")) {
                var other = queryStack.pop();
                queryStack.push(BoolQuery.of(q -> {
                    q.must(queryStack.pop());
                    q.mustNot(other);
                    return q;
                })._toQuery());
            } else {
                var fieldValueTuple = token.split(":");
                var searchType = SearchType.regular;
                if (fieldValueTuple[1].startsWith("\"") && fieldValueTuple[1].endsWith("\"")) {
                    searchType = SearchType.phrase;
                }

                queryStack.push(CustomQueryBuilder.buildQuery(
                    searchType,
                    fieldValueTuple[0],
                    fieldValueTuple[1]));
            }
        }

        return queryStack.pop();
    }
}
