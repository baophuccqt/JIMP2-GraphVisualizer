package algorithm;

import model.Graph;

/**
 * Abstract base class for graph layout algorithms.
 * All algorithms must implement the layout() method,
 * which assigns X and Y coordinates to each node in the graph.
 */
public abstract class LayoutAlgorithm {
    public abstract void layout(Graph graph);
}