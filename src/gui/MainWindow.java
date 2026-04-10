package gui;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    public GraphPanel graphPanel;
    public ToolPanel toolPanel;

    public MainWindow() {
        setTitle("Graph Visualizer");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        graphPanel = new GraphPanel();
        toolPanel  = new ToolPanel(graphPanel);

        add(toolPanel,  BorderLayout.WEST);
        add(graphPanel, BorderLayout.CENTER);

        setVisible(true);
    }
}
