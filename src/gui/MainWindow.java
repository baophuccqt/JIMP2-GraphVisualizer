package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainWindow extends JFrame {
    public GraphPanel graphPanel;
    public ToolPanel toolPanel;
    public CoordinatePanel coordinatePanel;
    public ViewPort viewPort;

    public MainWindow() {
        setTitle("Graph Visualizer");
        setSize(900, 600);
        setMinimumSize(new Dimension(300, 200));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        coordinatePanel = new CoordinatePanel();
        // add paddings to cordinate panel (did it this way because there is no such
        // thing called padding in Swing)
        coordinatePanel.setBorder( new EmptyBorder(10, 10, 10, 10));

        graphPanel = new GraphPanel(coordinatePanel);
        toolPanel  = new ToolPanel(graphPanel);

//        viewPort = new ViewPort();
//        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        northPanel.setOpaque(false);
//        northPanel.add(viewPort);
//        add(northPanel, BorderLayout.NORTH);

        add(toolPanel,  BorderLayout.EAST);
        add(graphPanel, BorderLayout.CENTER);
        add(coordinatePanel, BorderLayout.SOUTH);

        setVisible(true);
    }
}
