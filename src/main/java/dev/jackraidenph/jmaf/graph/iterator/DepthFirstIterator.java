package dev.jackraidenph.jmaf.graph.iterator;

import dev.jackraidenph.jmaf.graph.DirectedGraph;

import java.util.*;

public class DepthFirstIterator<T> extends GraphIterator<T> {

    private final DirectedGraph<T> graph;
    private final Deque<T> stack = new LinkedList<>();
    private final Set<T> visited;
    private T current;

    public DepthFirstIterator(DirectedGraph<T> graph) {
        this(graph, graph.getOrigin());
    }

    public DepthFirstIterator(DirectedGraph<T> graph, T origin) {
        this.graph = graph;

        visited = new HashSet<>(graph.getNodes().size());
        if (origin != null && !graph.getNodes().contains(origin)) {
            throw new IllegalArgumentException("[%s] is not a part of the graph".formatted(origin));
        } else if (origin != null) {
            this.stack.push(origin);
            this.current = origin;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        //Cleans duplicates, if present, needs a fix?
        while (!stack.isEmpty() && visited.contains(stack.peek())) {
            stack.pop();
        }
        return !stack.isEmpty();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        current = stack.pop();
        visited.add(current);
        for (T adj : graph.getAdjacentNodes(current)) {
            if (!visited.contains(adj)) {
                stack.push(adj);
            }
        }
        return current;
    }
}
