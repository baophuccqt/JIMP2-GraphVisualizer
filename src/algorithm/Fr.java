package algorithm;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fruchterman-Reingold force-directed layout algorithm.
 *
 * Nodes repel each other like magnets, while edges pull connected
 * nodes together like springs. A "temperature" variable controls the
 * maximum displacement per iteration and cools down over time so the
 * layout gradually settles into a stable position.
 *
 * Reference: T. Fruchterman & E. Reingold, "Graph drawing by force-directed
 * placement", Software – Practice & Experience, 1991.
 */
public class Fr extends LayoutAlgorithm {

    // Canvas dimensions – must match GraphPanel size
    private static final double WIDTH      = 800;
    private static final double HEIGHT     = 600;

    // Margin from canvas edge so nodes and their labels are never clipped
    private static final double PADDING    = 30;

    // Small value to avoid division by zero when two nodes overlap exactly
    private static final double EPSILON    = 1e-4;

    private static final int    ITERATIONS = 1000;

    @Override
    public void layout(Graph graph) {
        int n = graph.nodes.size();
        if (n == 0) return;

        // Build a map from node id → index in graph.nodes list
        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < n; i++) {
            idToIndex.put(graph.nodes.get(i).id, i);
        }

        // Ideal edge length k: balances repulsion and attraction so that
        // nodes spread evenly across the available area
        double area = WIDTH * HEIGHT;
        double k    = Math.sqrt(area / n);

        // Starting temperature: limits max displacement to WIDTH/10 pixels
        double temperature = WIDTH / 10.0;
        double dt          = temperature / ITERATIONS; // cooling step per iteration

        // Place all nodes at random starting positions within the padded area
        Random rng = new Random();
        for (Node node : graph.nodes) {
            node.X = PADDING + rng.nextDouble() * (WIDTH  - 2 * PADDING);
            node.Y = PADDING + rng.nextDouble() * (HEIGHT - 2 * PADDING);
        }

        // Per-node displacement vectors, reused each iteration
        double[] dispX = new double[n];
        double[] dispY = new double[n];

        for (int iter = 0; iter < ITERATIONS; iter++) {

            // ── A. Repulsive forces ──────────────────────────────────────────
            // Every pair of nodes pushes each other away.
            // Repulsion force magnitude: fr = k² / distance
            for (int i = 0; i < n; i++) {
                dispX[i] = 0.0;
                dispY[i] = 0.0;
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    double dx   = graph.nodes.get(i).X - graph.nodes.get(j).X;
                    double dy   = graph.nodes.get(i).Y - graph.nodes.get(j).Y;
                    double dist = Math.sqrt(dx * dx + dy * dy) + EPSILON;

                    double fr = (k * k) / dist;
                    dispX[i] += (dx / dist) * fr;
                    dispY[i] += (dy / dist) * fr;
                }
            }

            // ── B. Attractive forces ─────────────────────────────────────────
            // Each edge pulls its two endpoints toward each other.
            // Attraction force magnitude: fa = distance² / k  (scaled by weight)
            for (Edge e : graph.edges) {
                Integer vi = idToIndex.get(e.startNode);
                Integer ui = idToIndex.get(e.endNode);
                if (vi == null || ui == null) continue;

                double dx   = graph.nodes.get(vi).X - graph.nodes.get(ui).X;
                double dy   = graph.nodes.get(vi).Y - graph.nodes.get(ui).Y;
                double dist = Math.sqrt(dx * dx + dy * dy) + EPSILON;

                double fa     = (dist * dist / k) * e.len; // e.len is the edge weight
                double moveX  = (dx / dist) * fa;
                double moveY  = (dy / dist) * fa;

                dispX[vi] -= moveX;
                dispY[vi] -= moveY;
                dispX[ui] += moveX;
                dispY[ui] += moveY;
            }

            // ── C. Apply displacement, clamped to current temperature ─────────
            // Temperature prevents nodes from flying too far in early iterations.
            for (int i = 0; i < n; i++) {
                double dist        = Math.sqrt(dispX[i] * dispX[i] + dispY[i] * dispY[i]) + EPSILON;
                double limitedDist = Math.min(dist, temperature);

                graph.nodes.get(i).X += (dispX[i] / dist) * limitedDist;
                graph.nodes.get(i).Y += (dispY[i] / dist) * limitedDist;

                // Keep nodes inside the padded canvas boundaries
                graph.nodes.get(i).X = Math.max(PADDING, Math.min(WIDTH  - PADDING, graph.nodes.get(i).X));
                graph.nodes.get(i).Y = Math.max(PADDING, Math.min(HEIGHT - PADDING, graph.nodes.get(i).Y));
            }

            // ── D. Cool down ──────────────────────────────────────────────────
            temperature = Math.max(0, temperature - dt);
        }
    }
}