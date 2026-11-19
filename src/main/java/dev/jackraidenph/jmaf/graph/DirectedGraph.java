package dev.jackraidenph.jmaf.graph;

import java.util.*;

public interface DirectedGraph<T> extends Iterable<T> {
    Collection<T> getNodes();

    boolean addNode(T node);

    boolean addEdge(T from, T to);

    boolean removeNode(T node);

    boolean removeEdge(T from, T to);

    Map<T, ? extends Collection<T>> getEdges();

    boolean isAcyclic();

    DirectedGraph<T> getSubgraph(T origin);

    default boolean isDisconnected() {
        int visited = 0;
        for (T ignored : this) {
            visited++;
        }

        return visited != getNodes().size();
    }

    default Collection<T> getComponentsOrigins() {
        SequencedSet<T> origins = new LinkedHashSet<>();
        SequencedSet<T> notVisited = new LinkedHashSet<>(getNodes());

        T graphOrigin = getOrigin();
        T current;
        while (!notVisited.isEmpty()) {

            if (graphOrigin != null) {
                current = graphOrigin;
                graphOrigin = null;
            } else {
                current = notVisited.getFirst();
            }

            origins.add(current);

            for (T node : GraphIterators.breadth(this, current)) {
                notVisited.remove(node);
            }
        }

        return origins;
    }

    boolean hasCycles(T origin);

    default boolean hasCycles() {
        return getComponentsOrigins().stream().anyMatch(this::hasCycles);
    }

    boolean setOrigin(T origin);

    T getOrigin();

    Collection<T> getAdjacentNodes(T node);

    DirectedGraph<T> getReversal();

    default boolean hasNode(T node) {
        return getNodes().contains(node);
    }

    int edgesAmount();

    default boolean hasSelfLoops() {
        for (T node : getNodes()) {
            if (getAdjacentNodes(node).contains(node)) {
                return true;
            }
        }

        return false;
    }

    default String asString() {
        StringBuilder builder = new StringBuilder();

        StringJoiner edgesJoiner = new StringJoiner(",", "[", "]");

        for (T node : getNodes()) {
            Collection<T> adj = this.getAdjacentNodes(node);
            edgesJoiner.add(node + "->" + adj.toString());
        }

        builder.append(edgesJoiner);

        return builder.toString();
    }
}
