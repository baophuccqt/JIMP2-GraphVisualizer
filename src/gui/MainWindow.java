package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainWindow extends JFrame {
    public GraphPanel graphPanel;
    public ToolPanel toolPanel;
    public CoordinatePanel coordinatePanel;

    public MainWindow() {
        setTitle("Graph Visualizer");
        setSize(1200, 800);
        setMinimumSize(new Dimension(300, 200));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        coordinatePanel = new CoordinatePanel();
        // add paddings to coordinate panel (did it this way because there is no such
        // thing called padding in Swing)
        coordinatePanel.setBorder( new EmptyBorder(10, 10, 10, 10));

        graphPanel = new GraphPanel(coordinatePanel);
        toolPanel  = new ToolPanel(graphPanel);

        add(toolPanel,  BorderLayout.EAST);
        add(graphPanel, BorderLayout.CENTER);
        add(coordinatePanel, BorderLayout.SOUTH);

        setVisible(true);
    }
}
