package gui;

import algorithm.Fr;
import algorithm.LayoutAlgorithm;
import algorithm.Tutte;
import io.GraphReader;
import model.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;

public class ToolPanel extends JPanel {
    private GraphPanel graphPanel;
    private File lastDirectory = new File("./test-graphs"); // default to project test folder
    public LayoutAlgorithm currentAlgorithm = new Tutte();
    private JTextField pathField = new  JTextField("Last location: (none)");

    public ToolPanel(GraphPanel graphPanel) {
        this.graphPanel = graphPanel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // so it locates vertically
        setPreferredSize(new Dimension(150, 0));


        // Select file button
        JButton selectBtn = new JButton("Select File");
        selectBtn.addActionListener(e -> loadFile());
        add(selectBtn);


        // Path field show chosen file's directory
        pathField.setFont(pathField.getFont().deriveFont(10f));
        pathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathField.getPreferredSize().height));
        pathField.setEditable(false);
        add(pathField);



        // EXport to PNG button
        JLabel textAction = new JLabel("Actions");
        add(textAction);
        JButton exportBtn = new JButton("Export PNG");
        exportBtn.addActionListener(e -> exportPNG());
        add(exportBtn);


        // Algorithm options
        JLabel textAlgo = new JLabel("Algorithm");
        JRadioButton tutteRadio = new JRadioButton("Tutte");
        // need to setSelected befor add ItemListener
        // if we do it in a reverse way, it's gonna trigger layout before we even load the graph
        tutteRadio.setSelected(true);
        tutteRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentAlgorithm = new Tutte();
                runLayout(currentAlgorithm);
            }
        });
        add(textAlgo);
        add(tutteRadio);

        JRadioButton frRadio = new JRadioButton("FR");
        frRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentAlgorithm = new Fr();
                runLayout(currentAlgorithm);
            }
        });
        add(frRadio);

        // group them together
        ButtonGroup group = new ButtonGroup();
        group.add(tutteRadio);
        group.add(frRadio);

        JButton resetBtn = new JButton("Reset View");
        resetBtn.addActionListener(e -> graphPanel.resetView());
        add(resetBtn);
    }

    private void loadFile() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile(); // remember this folder for next time
            try {
                Graph graph = new GraphReader().read(file.getAbsolutePath());
                graphPanel.setGraph(graph);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, "Error: " + exception.getMessage());
            }
        }

        pathField.setText("Last location: " + lastDirectory.getAbsolutePath());
        runLayout(currentAlgorithm);
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

    private void exportPNG() {

    }
}
