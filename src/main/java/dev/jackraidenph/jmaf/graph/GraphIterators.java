package dev.jackraidenph.jmaf.graph;

import dev.jackraidenph.jmaf.graph.iterator.BreadthFirstIterator;
import dev.jackraidenph.jmaf.graph.iterator.DepthFirstIterator;

public final class GraphIterators {
    private GraphIterators() {

    }

    public static <T> BreadthFirstIterator<T> breadth(DirectedGraph<T> graph) {
        return new BreadthFirstIterator<>(graph);
    }

    public static <T> BreadthFirstIterator<T> breadth(DirectedGraph<T> graph, T origin) {
        return new BreadthFirstIterator<>(graph, origin);
    }

    public static <T> DepthFirstIterator<T> depth(DirectedGraph<T> graph) {
        return new DepthFirstIterator<>(graph);
    }

    public static <T> DepthFirstIterator<T> depth(DirectedGraph<T> graph, T origin) {
        return new DepthFirstIterator<>(graph, origin);
    }
}
