package dev.jackraidenph.jmaf.graph.iterator;

import java.util.Iterator;

public abstract class GraphIterator<T> implements Iterator<T>, Iterable<T> {
    @Override
    public Iterator<T> iterator() {
        return this;
    }
}
