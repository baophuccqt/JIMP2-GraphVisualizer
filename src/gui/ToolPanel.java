package gui;

import algorithm.LayoutAlgorithm;
import io.GraphReader;
import model.Graph;
import algorithm.PanamaFruchterman;
import algorithm.PanamaTutte;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class ToolPanel extends JPanel {
    private GraphPanel graphPanel;
    private File lastDirectory = new File("./test-graphs"); // default to project test folder
    public LayoutAlgorithm currentAlgorithm = new PanamaTutte();
    private JTextField pathField = new  JTextField("Last location: (none)");

    public ToolPanel(GraphPanel graphPanel) {
        this.graphPanel = graphPanel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // so it locates vertically
        setPreferredSize(new Dimension(150, 0));

        //toggle labels button
        JCheckBox labelToggle = new JCheckBox("Show Labels", true);
        labelToggle.addActionListener(e -> {
            graphPanel.setShowLabels(labelToggle.isSelected());
        });
        add(labelToggle);

        // Select file button
        JButton selectBtn = new JButton("Select File");
        selectBtn.addActionListener(e -> loadFile());
        add(selectBtn);


        // Path field show chosen file's directory
        pathField.setFont(pathField.getFont().deriveFont(10f));
        pathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, pathField.getPreferredSize().height));
        pathField.setEditable(false);
        add(pathField);



        // Export to PNG button
        JLabel textAction = new JLabel("Actions");
        add(textAction);
        JButton exportBtn = new JButton("Export to PNG");
        exportBtn.addActionListener(e -> exportPNG());
        add(exportBtn);

        JButton exportTextBtn = new JButton("Export to TXT");
        exportTextBtn.addActionListener(e -> exportToText());
        add(exportTextBtn);

        JButton exportBinBtn = new JButton("Export to binary");
        exportBinBtn.addActionListener(e -> exportToBin());
        add(exportBinBtn);


        // Algorithm options
        JLabel textAlgo = new JLabel("Algorithm");
        JRadioButton tutteRadio = new JRadioButton("Tutte");
        // need to setSelected before add ItemListener
        // if we do it in a reverse way, it's gonna trigger layout before we even load the graph
        tutteRadio.setSelected(true);
        tutteRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentAlgorithm = new PanamaTutte();
                runLayout(currentAlgorithm);
            }
        });
        add(textAlgo);
        add(tutteRadio);

        JRadioButton frRadio = new JRadioButton("Fruchterman");
        frRadio.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentAlgorithm = new PanamaFruchterman();
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
        Graph graph = graphPanel.getGraph();
        if (graph == null) {
            JOptionPane.showMessageDialog(this, "Error: No graph loaded to export.");
            return;
        }

        File outputDir = new File("./output");
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdir();
            if (!created) {
                JOptionPane.showMessageDialog(this, "Error: Could not create output directory.");
                return;
            }
        }

        JFileChooser saveChooser = new JFileChooser(outputDir);
        saveChooser.setDialogTitle("Save graph as PNG");

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = saveChooser.getSelectedFile();

            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".png")) {
                file = new File(path + ".png");
            }

            try {
                BufferedImage image = new BufferedImage(
                        graphPanel.getWidth(),
                        graphPanel.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );

                Graphics2D g2 = image.createGraphics();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                graphPanel.paintAll(g2);
                g2.dispose();

                if (javax.imageio.ImageIO.write(image, "png", file)) {
                    JOptionPane.showMessageDialog(this, "Export successful!");
                } else {
                    JOptionPane.showMessageDialog(this, "Error: Standard PNG writer not found.");
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error during export: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    private void exportToText() {
        Graph currentGraph = graphPanel.getGraph();
        if (currentGraph == null) {
            JOptionPane.showMessageDialog(this, "Error: No graph loaded to export.");
            return;
        }

        JFileChooser saveChooser = new JFileChooser(lastDirectory);
        saveChooser.setDialogTitle("Save graph as Text File");

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = saveChooser.getSelectedFile();
            String path = file.getAbsolutePath();

            if (!path.toLowerCase().endsWith(".txt")) {
                path += ".txt";
            }

            exportToTxt(currentGraph, path);
        }
    }

    public void exportToTxt(Graph javaGraph, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("[WIERZCHOLKI]");
            for (model.Node node : javaGraph.nodes) {
                writer.printf(java.util.Locale.US, "ID_%s %.2f %.2f\n", node.id, node.X, node.Y);
            }

            // Jeśli zapis się udał, wyświetlamy radosny komunikat o sukcesie
            JOptionPane.showMessageDialog(this, "Graph successfully exported to text file!");

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error during text export: " + e.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportToBin() {
        Graph currentGraph = graphPanel.getGraph();
        if (currentGraph == null) {
            JOptionPane.showMessageDialog(this, "Error: No graph loaded to export.");
            return;
        }

        JFileChooser saveChooser = new JFileChooser(lastDirectory);
        saveChooser.setDialogTitle("Save graph as Binary File");

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = saveChooser.getSelectedFile();
            String path = file.getAbsolutePath();

            if (!path.toLowerCase().endsWith(".bin")) {
                path += ".bin";
            }

            writeBinaryFile(currentGraph, path);
        }
    }

    public void writeBinaryFile(Graph graph, String filePath) {
        // Sort id ascending
        // Copy it to isolate the output files and the lists used for drawing
        List<model.Node> sortedNodes = new ArrayList<>(graph.nodes);
        sortedNodes.sort(Comparator.comparingInt(n -> n.id));

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filePath))) {
            // 20-byte buffer used for every vertex
            ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

            for (model.Node node : sortedNodes) {
                buffer.clear();
                buffer.putInt(node.id);    // 4 byte, offset 0
                buffer.putDouble(node.X);  // 8 byte, offset 4
                buffer.putDouble(node.Y);  // 8 byte, offset 12
                out.write(buffer.array()); // exactly 20 byte
            }

            JOptionPane.showMessageDialog(this, "Graph successfully exported to binary file! (" + (sortedNodes.size() * 20) + " bytes)");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error during binary export: " + e.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
