package gui;

import javax.swing.*;
import java.awt.*;

public class CoordinatePanel extends JPanel {
    private JLabel labelX = new JLabel("Cordinate X: ");
    private JLabel labelY = new JLabel("Cordinate Y: ");
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

    // set cordinates if were changed
    public void setCordinates(double x, double y) {
        labelX.setText(String.format("Cordinate X: %.2f", x));
        labelY.setText(String.format("Cordinate Y: %.2f", y));
    }

    public void setScale(double scale) {
        labelScale.setText(String.format("Zoom: %.2f x", scale));
    }
}
