package gui;

import algorithm.Fr;
import algorithm.LayoutAlgorithm;
import algorithm.Tutte;
import io.GraphReader;
import model.Graph;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ToolPanel extends JPanel {
    private GraphPanel graphPanel;

    public ToolPanel(GraphPanel graphPanel) {
        this.graphPanel = graphPanel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // so it locates vertically
        setPreferredSize(new Dimension(150, 0));

        JButton loadBtn = new JButton("Load File");
        loadBtn.addActionListener(e -> loadFile());

        add(loadBtn);

        JButton tutteBtn = new JButton("Run Tutte");
        tutteBtn.addActionListener(e -> runLayout(new Tutte()));

        JButton frBtn = new JButton("Run FR");
        frBtn.addActionListener(e -> runLayout(new Fr()));

        add(tutteBtn);
        add(frBtn);
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Graph graph = new GraphReader().read(file.getAbsolutePath());
                graphPanel.setGraph(graph);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "Error: " + exception.getMessage());
            }
        }
    }

    private void runLayout(LayoutAlgorithm algorithm) {
        Graph graph = graphPanel.getGraph();
        if (graph == null) {
            JOptionPane.showMessageDialog(this, "Error: Graph is null, load it first");
            return;
        }

        algorithm.layout(graph);
        graphPanel.repaint();
    }
}
