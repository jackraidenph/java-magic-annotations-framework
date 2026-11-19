package dev.jackraidenph.jmaf.graph.iterator;

import dev.jackraidenph.jmaf.graph.DirectedGraph;

import java.util.*;

public class BreadthFirstIterator<T> extends GraphIterator<T> {

    private final Map<T, Integer> depthMap = new HashMap<>();
    private final Set<T> visited = new HashSet<>();
    private final Queue<T> queue = new LinkedList<>();
    private final DirectedGraph<T> graph;
    T current = null;
    private int currentIndexAtDepth;

    public BreadthFirstIterator(DirectedGraph<T> graph) {
        this(graph, graph.getOrigin());
    }

    public BreadthFirstIterator(DirectedGraph<T> graph, T origin) {
        this.graph = graph;

        if (origin != null && !graph.getNodes().contains(origin)) {
            throw new IllegalArgumentException("[%s] is not a part of the graph".formatted(origin));
        } else if (origin != null) {
            this.queue.add(origin);
            depthMap.put(origin, 0);
            this.visited.add(origin);
        }
    }

    public DirectedGraph<T> getGraph() {
        return this.graph;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        int oldDepth = getLevel();
        T next = queue.remove();
        current = next;
        int currentDepth = getLevel();

        if (oldDepth != currentDepth) {
            currentIndexAtDepth = 0;
        } else {
            currentIndexAtDepth++;
        }

        Collection<T> adjacent = this.getGraph().getAdjacentNodes(next);

        for (T adj : adjacent) {
            if (!visited.contains(adj)) {
                visited.add(adj);
                queue.add(adj);
                depthMap.put(adj, currentDepth + 1);
            }
        }

        return next;
    }

    public int getLevel() {
        return current == null ? -1 : depthMap.get(current);
    }

    public int getIndexOnLevel() {
        return currentIndexAtDepth;
    }
}
