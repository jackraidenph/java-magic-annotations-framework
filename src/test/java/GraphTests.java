import dev.jackraidenph.jmaf.graph.DirectedHashGraph;
import dev.jackraidenph.jmaf.graph.DirectedGraph;
import dev.jackraidenph.jmaf.graph.GraphIterators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTests {

    private DirectedGraph<String> graph;

    @BeforeEach
    public void setup() {
        graph = new DirectedHashGraph<>(true);
    }

    @Test
    public void testAddNode() {
        assertTrue(graph.addNode("Test1"));
        assertTrue(graph.getNodes().contains("Test1"));
    }

    @Test
    public void testRemoveNode() {
        graph.addNode("Test1");

        assertTrue(graph.removeNode("Test1"));
        assertFalse(graph.removeNode("Test1"));
        assertFalse(graph.getNodes().contains("Test1"));
    }

    @Test
    public void testAddEdge() {
        assertTrue(graph.addEdge("Test1", "Test2"));
        assertTrue(graph.getAdjacentNodes("Test1").contains("Test2"));
    }

    @Test
    public void testBreadthFirstSearch() {
        graph.addEdge("0", "1");
        graph.addEdge("0", "2");
        graph.addEdge("0", "3");

        graph.addEdge("1", "4");
        graph.addEdge("2", "4");
        graph.addEdge("3", "4");

        List<String> list = new ArrayList<>();
        GraphIterators.breadth(graph).forEachRemaining(list::add);

        assertEquals(List.of("0", "1", "2", "3", "4"), list);
    }

    @Test
    public void testDepthFirstSearch() {
        graph.addEdge("0", "3");
        graph.addEdge("0", "2");
        graph.addEdge("0", "1");

        graph.addEdge("1", "4");
        graph.addEdge("2", "4");
        graph.addEdge("3", "4");

        List<String> list = new ArrayList<>();
        GraphIterators.depth(graph).forEachRemaining(list::add);

        assertEquals(List.of("0", "1", "4", "2", "3"), list);
    }

    @Test
    public void testGraphGetReversed() {
        graph.addEdge("0", "1");
        graph.addEdge("0", "2");
        graph.addEdge("0", "3");

        graph.addEdge("1", "4");
        graph.addEdge("2", "4");
        graph.addEdge("3", "4");

        DirectedGraph<String> reversal = graph.getReversal();

        assertTrue(reversal.getAdjacentNodes("0").isEmpty());
        assertTrue(reversal.getAdjacentNodes("1").contains("0"));
        assertTrue(reversal.getAdjacentNodes("2").contains("0"));
        assertTrue(reversal.getAdjacentNodes("3").contains("0"));
        assertTrue(reversal.getAdjacentNodes("4").containsAll(List.of("1", "2", "3")));
    }

    @Test
    public void testSubgraph() {
        graph.addEdge("0", "1");
        graph.addEdge("1", "2");

        graph.addEdge("0", "3");
        graph.addEdge("3", "5");
        graph.addEdge("3", "4");
        graph.addEdge("4", "6");

        DirectedGraph<String> subgraph = graph.getSubgraph("3");

        assertTrue(subgraph.getNodes().containsAll(List.of("3", "4", "6", "5")));
    }

    @Test
    public void testHasCycles() {
        graph.addEdge("0", "1");
        graph.addEdge("1", "2");
        graph.addEdge("2", "3");
        graph.addEdge("3", "4");
        graph.addEdge("4", "1");

        assertTrue(graph.hasCycles());
    }

    @Test
    public void testHasNoCycles() {
        graph.addEdge("0", "1");
        graph.addEdge("0", "2");

        graph.addEdge("1", "2");
        graph.addEdge("1", "3");

        graph.addEdge("2", "3");
        graph.addEdge("2", "4");

        graph.addEdge("3", "4");

        assertFalse(graph.hasCycles());
    }

    @Test
    public void testHasCyclesInSecondComponent() {
        //First component - no cycles
        graph.addEdge("0", "1");
        graph.addEdge("0", "2");
        graph.addEdge("1", "2");
        graph.addEdge("1", "3");
        graph.addEdge("2", "3");
        graph.addEdge("2", "4");
        graph.addEdge("3", "4");

        //Second component - yes cycle
        graph.addEdge("5", "6");
        graph.addEdge("6", "5");

        assertTrue(graph.hasCycles());
    }

    @Test
    public void testIsDisconnected() {
        graph.addEdge("0", "1");
        graph.addEdge("1", "2");
        graph.addEdge("2", "3");
        graph.addEdge("3", "4");

        graph.addNode("5");

        assertTrue(graph.isDisconnected());
    }

    @Test
    public void testIsNotDisconnected() {
        graph.addEdge("0", "1");
        graph.addEdge("1", "2");
        graph.addEdge("2", "3");
        graph.addEdge("3", "4");

        assertFalse(graph.isDisconnected());
    }

    @Test
    public void testEmptyIterator() {
        assertThrows(NoSuchElementException.class, () -> graph.iterator().next());
    }

    @Test
    public void testCycleCheckAtAddEdge() {
        graph = new DirectedHashGraph<>(false);
        graph.addEdge("0", "1");
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge("1", "0"));
    }
}
