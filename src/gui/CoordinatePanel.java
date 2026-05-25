package gui;

import javax.swing.*;
import java.awt.*;

public class CoordinatePanel extends JPanel {
    private JLabel labelX = new JLabel("Coordinate X: ");
    private JLabel labelY = new JLabel("Coordinate Y: ");
    private JLabel labelScale = new JLabel("Zoom: 1.0x");

    public CoordinatePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(0, 60));

        labelX.setAlignmentY(Component.CENTER_ALIGNMENT);
        labelY.setAlignmentY(Component.CENTER_ALIGNMENT);
        add(labelX);
        add(labelY);
        add(labelScale);
    }

    // set coordinates if were changed
    public void setCoordinates(double x, double y) {
        labelX.setText(String.format("Coordinate X: %.2f", x));
        labelY.setText(String.format("Coordinate Y: %.2f", y));
    }

    public void setScale(double scale) {
        labelScale.setText(String.format("Zoom: %.2f x", scale));
    }
}
