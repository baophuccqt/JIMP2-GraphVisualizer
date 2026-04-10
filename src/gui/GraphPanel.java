package gui;

import model.Edge;
import model.Graph;
import model.Node;

import javax.swing.*;
import java.awt.*;

public class GraphPanel extends JPanel {
    private Graph graph;

    public GraphPanel() {
        setBackground(Color.WHITE);
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        repaint(); // ask Swing to re-render
    }

    public Graph getGraph() {
        return graph;
    }

    private Node findNote(int id) {
        for (Node node : graph.nodes) {
            if (node.id == id) return node;
        }
        return null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // delete old canvas
        if (graph == null) return;

        // Draw edges
        g.setColor(Color.BLACK);
        for (Edge e: graph.edges) {
            Node a = findNote(e.startNode);
            Node b = findNote(e.endNode);

            if (a != null && b != null) {
                g.drawLine((int) a.X, (int) a.Y, (int) b.X, (int) b.Y);
            }
        }

        // draw nodes
        g.setColor(Color.RED);
        for (Node node : graph.nodes) {
            int r = 10;
            g.fillOval((int) node.X - r,  (int) node.Y - r, r * 2, r * 2);
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(node.id), (int) node.X, (int) node.Y);
            g.setColor(Color.RED);
        }
    }
}
