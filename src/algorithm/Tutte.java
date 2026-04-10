package algorithm;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tutte's barycentric embedding algorithm.
 *
 * The first 3 nodes are pinned ("fixed") onto the vertices of a triangle
 * drawn on a circle. Every other ("inner") node is iteratively moved to
 * the average position of its neighbours until the layout converges.
 *
 * Reference: W.T. Tutte, "How to draw a graph", 1963.
 */
public class Tutte extends LayoutAlgorithm {

    // Canvas dimensions – must match GraphPanel size
    private static final double WIDTH     = 800;
    private static final double HEIGHT    = 600;

    // Margin from canvas edge so nodes and their labels are never clipped
    private static final double PADDING   = 30;

    // Radius of the outer circle, shrunk to stay inside the padded area
    private static final double RADIUS    = Math.min(WIDTH, HEIGHT) * 0.4 - PADDING;

    private static final int    MAX_ITER  = 1000;
    private static final double TOLERANCE = 1e-6;

    @Override
    public void layout(Graph graph) {
        int n = graph.nodes.size();
        if (n == 0) return;

        // Build a map from node id → index in graph.nodes list
        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idToIndex.put(graph.nodes.get(i).id, i);
        }

        // Number of nodes pinned on the outer circle (triangle = 3 corners)
        int nFixed = Math.min(3, n);

        // Place fixed nodes evenly on a circle centred in the canvas
        double cx = WIDTH  / 2.0;
        double cy = HEIGHT / 2.0;
        for (int i = 0; i < nFixed; i++) {
            double angle = 2.0 * Math.PI * i / nFixed;
            graph.nodes.get(i).X = cx + RADIUS * Math.cos(angle);
            graph.nodes.get(i).Y = cy + RADIUS * Math.sin(angle);
        }

        // Initialise inner nodes at the centre of the canvas
        for (int i = nFixed; i < n; i++) {
            graph.nodes.get(i).X = cx;
            graph.nodes.get(i).Y = cy;
        }

        // Build adjacency list: adjList.get(i) holds indices of all neighbours of node i
        List<List<Integer>> adjList = new ArrayList<>();
        for (int i = 0; i < n; i++) adjList.add(new ArrayList<>());

        for (Edge e : graph.edges) {
            Integer a = idToIndex.get(e.startNode);
            Integer b = idToIndex.get(e.endNode);
            if (a == null || b == null) continue;
            adjList.get(a).add(b);
            adjList.get(b).add(a);
        }

        // Gauss-Seidel relaxation: move each inner node to the average of its neighbours.
        // Repeat until movement falls below TOLERANCE or MAX_ITER is reached.
        for (int iter = 0; iter < MAX_ITER; iter++) {
            double maxDelta = 0.0;

            for (int i = nFixed; i < n; i++) {
                List<Integer> neighbours = adjList.get(i);
                if (neighbours.isEmpty()) continue;

                // Compute average neighbour position
                double sumX = 0.0, sumY = 0.0;
                for (int nb : neighbours) {
                    sumX += graph.nodes.get(nb).X;
                    sumY += graph.nodes.get(nb).Y;
                }
                double newX = sumX / neighbours.size();
                double newY = sumY / neighbours.size();

                // Track how much this node moved
                double dx = newX - graph.nodes.get(i).X;
                double dy = newY - graph.nodes.get(i).Y;
                double delta = dx * dx + dy * dy;
                if (delta > maxDelta) maxDelta = delta;

                graph.nodes.get(i).X = newX;
                graph.nodes.get(i).Y = newY;
            }

            // Converged when the largest single-node movement is negligible
            if (maxDelta < TOLERANCE * TOLERANCE) break;
        }
    }
}