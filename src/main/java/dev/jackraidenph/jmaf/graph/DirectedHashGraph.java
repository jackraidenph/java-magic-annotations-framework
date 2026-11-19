package dev.jackraidenph.jmaf.graph;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class DirectedHashGraph<T> implements DirectedGraph<T> {
    protected final Map<T, SequencedSet<T>> adjacencyMap;
    protected final Set<T> nodes;
    private final boolean allowsCycles;
    private T origin = null;
    private int edgesAmount = 0;

    protected DirectedHashGraph(Map<T, SequencedSet<T>> adjacencyMap, Set<T> nodes, boolean allowsCycles) {
        this.adjacencyMap = adjacencyMap;
        this.nodes = nodes;
        this.allowsCycles = allowsCycles;
    }

    public DirectedHashGraph(boolean allowsCycles) {
        this(new HashMap<>(), new LinkedHashSet<>(), allowsCycles);
    }

    public Collection<T> getNodes() {
        return Collections.unmodifiableCollection(nodes);
    }

    @Override
    public boolean addNode(T node) {
        if (node == null) {
            throw new IllegalArgumentException("Graph doesn't allow null nodes");
        }

        boolean added = nodes.add(node);

        if (added && origin == null) {
            origin = node;
        }

        return added;
    }

    @Override
    public boolean addEdge(T from, T to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Graph doesn't allow null nodes");
        }

        if (!allowsCycles && (Objects.equals(from, to) || isReachable(to, from))) {
            throw new IllegalArgumentException("Edge from [%s] to [%s] forms a cycle in a graph that doesn't allow cycles".formatted(from, to));
        }

        addNode(from);
        addNode(to);

        boolean added = adjacencyMap.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);

        if (added) {
            edgesAmount++;
        }

        return added;
    }

    private boolean isReachable(T source, T target) {
        Set<T> visited = new HashSet<>();
        Deque<T> stack = new LinkedList<>();
        visited.add(source);
        stack.push(source);
        while (!stack.isEmpty()) {
            T current = stack.pop();
            if (Objects.equals(current, target)) return true;
            for (T adj : getAdjacentNodes(current)) {
                if (visited.add(adj)) stack.push(adj);
            }
        }
        return false;
    }

    @Override
    public boolean removeNode(T node) {
        if (!nodes.remove(node)) {
            return false;
        }

        Set<T> outgoing = adjacencyMap.remove(node);
        if (outgoing != null) {
            edgesAmount -= outgoing.size();
            for (T adj : outgoing) {
                removeEdge(node, adj);
            }
        }

        if (Objects.equals(node, origin)) {
            origin = nodes.stream().findFirst().orElse(null);
        }

        return true;
    }

    @Override
    public boolean removeEdge(T from, T to) {
        Collection<T> c = adjacencyMap.get(from);
        boolean removed = false;
        if (c != null) {
            removed = c.remove(to);
        }

        if (removed) {
            edgesAmount--;
            if (c.isEmpty()) {
                adjacencyMap.remove(from);
            }
        }

        return removed;
    }

    @Override
    public Map<T, ? extends Collection<T>> getEdges() {
        return Collections.unmodifiableMap(
                adjacencyMap.entrySet().stream().collect(Collectors.toMap(
                        Entry::getKey,
                        e -> Collections.unmodifiableSet(e.getValue()))
                )
        );
    }

    @Override
    public int edgesAmount() {
        return edgesAmount;
    }

    @Override
    public DirectedGraph<T> getSubgraph(T origin) {
        DirectedHashGraph<T> subgraph = new DirectedHashGraph<>(allowsCycles);

        Iterator<T> iterator = GraphIterators.breadth(this, origin);
        while (iterator.hasNext()) {
            T node = iterator.next();

            subgraph.addNode(node);
            for (T adj : this.getAdjacentNodes(node)) {
                subgraph.addNode(adj);
                if (subgraph.adjacencyMap.computeIfAbsent(node, k -> new LinkedHashSet<>()).add(adj)) {
                    subgraph.edgesAmount++;
                }
            }
        }

        return subgraph;
    }

    public DirectedGraph<T> getReversal() {
        DirectedHashGraph<T> reversal = new DirectedHashGraph<>(allowsCycles);

        nodes.forEach(reversal::addNode);

        for (T node : this.adjacencyMap.keySet()) {
            for (T adjacent : this.adjacencyMap.get(node)) {
                if (reversal.adjacencyMap.computeIfAbsent(adjacent, k -> new LinkedHashSet<>()).add(node)) {
                    reversal.edgesAmount++;
                }
            }
        }

        reversal.setOrigin(origin);

        return reversal;
    }

    @Override
    public boolean isAcyclic() {
        return !allowsCycles || !hasCycles();
    }

    @Override
    public T getOrigin() {
        return origin;
    }

    @Override
    public boolean setOrigin(T origin) {
        if (!hasNode(origin)) {
            return false;
        }
        this.origin = origin;

        return true;
    }

    @Override
    public Collection<T> getAdjacentNodes(T node) {
        return Collections.unmodifiableCollection(adjacencyMap.getOrDefault(node, Collections.emptySortedSet()));
    }

    @Override
    public boolean hasCycles(T origin) {
        int nodesAmount = getNodes().size();
        return hasCycleStep(origin, new HashSet<>(nodesAmount), new HashSet<>(nodesAmount));
    }

    private boolean hasCycleStep(T current, Set<T> visited, Set<T> stack) {
        if (stack.contains(current)) {
            return true;
        }

        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        stack.add(current);
        for (T i : getAdjacentNodes(current)) {
            if (hasCycleStep(i, visited, stack)) {
                return true;
            }
        }
        stack.remove(current);

        return false;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public Iterator<T> iterator() {
        return GraphIterators.breadth(this);
    }
}
