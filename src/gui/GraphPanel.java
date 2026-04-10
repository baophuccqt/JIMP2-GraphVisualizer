package gui;

import model.Edge;
import model.Graph;
import model.Node;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

public class GraphPanel extends JPanel {
    private Graph graph;

    private double scale   = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private int dragStartX, dragStartY;

    private static final int    NODE_RADIUS  = 10;
    private static final double ZOOM_FACTOR  = 1.05; // 5% per scroll tick

    public GraphPanel() {
        setBackground(Color.WHITE);
        addMouseWheelListener(this::onScroll);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                offsetX += e.getX() - dragStartX;
                offsetY += e.getY() - dragStartY;
                dragStartX = e.getX();
                dragStartY = e.getY();
                repaint();
            }
        });
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        resetView();
        repaint();
    }

    public Graph getGraph() {
        return graph;
    }

    public void resetView() {
        scale   = 1.0;
        offsetX = 0;
        offsetY = 0;
        repaint();
    }

    // Zoom centred on the cursor position
    private void onScroll(MouseWheelEvent e) {
        // getPreciseWheelRotation() handles touchpad fractional values correctly.
        // getWheelRotation() truncates to int, causing touchpad events near 0 to always zoom in.
        double factor = (e.getPreciseWheelRotation() < 0) ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
        double mx = e.getX();
        double my = e.getY();
        offsetX = mx - factor * (mx - offsetX);
        offsetY = my - factor * (my - offsetY);
        scale  *= factor;
        repaint();
    }

    private Node findNode(int id) {
        for (Node node : graph.nodes) {
            if (node.id == id) return node;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graph == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform original = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        // Keep edge line width at 1px on screen regardless of zoom level
        g2.setStroke(new BasicStroke((float) (1.0 / scale)));

        // Draw edges clipped to node boundary so they don't hide under node circles.
        // NODE_RADIUS is in screen pixels, so divide by scale to get graph-space radius.
        g2.setColor(Color.BLACK);
        double r = NODE_RADIUS / scale;
        for (Edge e : graph.edges) {
            Node a = findNode(e.startNode);
            Node b = findNode(e.endNode);
            if (a == null || b == null) continue;

            double dx  = b.X - a.X;
            double dy  = b.Y - a.Y;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 2 * r) continue; // nodes overlap on screen, nothing to draw

            // Unit vector along the edge
            double ux = dx / len;
            double uy = dy / len;

            // Start/end points offset by node radius from each centre.
            // Use double precision so the transform scales exact values — no rounding before scale.
            double x1 = a.X + ux * r;
            double y1 = a.Y + uy * r;
            double x2 = b.X - ux * r;
            double y2 = b.Y - uy * r;

            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }

        // Restore transform so nodes stay fixed size on screen
        g2.setTransform(original);

        for (Node node : graph.nodes) {
            int sx = (int) (node.X * scale + offsetX);
            int sy = (int) (node.Y * scale + offsetY);

            g2.setColor(Color.RED);
            g2.fillOval(sx - NODE_RADIUS, sy - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            g2.setColor(Color.BLACK);
            g2.drawString(String.valueOf(node.id), sx + NODE_RADIUS + 2, sy + 4);
        }
    }
}
