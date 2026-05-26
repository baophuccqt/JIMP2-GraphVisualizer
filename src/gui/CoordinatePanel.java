package gui;

import javax.swing.*;
import java.awt.*;

/**
 * A status panel that dynamically displays the current mouse coordinates
 * and the zoom level of the graph viewport.
 */
public class CoordinatePanel extends JPanel {
    // Labels for displaying the X, Y coordinates and the current scale/zoom percentage
    private JLabel labelX = new JLabel("Coordinate X: ");
    private JLabel labelY = new JLabel("Coordinate Y: ");
    private JLabel labelScale = new JLabel("Zoom: 1.0x");

    /**
     * Constructs a CoordinatePanel with a vertical box layout and default layout alignments.
     */
    public CoordinatePanel() {
        // Arrange child components vertically (one below the other)
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // Set fixed height for the status panel while letting width adjust dynamically
        setPreferredSize(new Dimension(0, 60));

        // Center the labels within the container along the Y-axis alignment
        labelX.setAlignmentY(Component.CENTER_ALIGNMENT);
        labelY.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Add the informative text labels to the panel
        add(labelX);
        add(labelY);
        add(labelScale);
    }

    /**
     * Updates the text labels to display the current X and Y coordinates.
     * Formats the floating-point values to exactly 2 decimal places.
     * * @param x the current X coordinate of the cursor
     * @param y the current Y coordinate of the cursor
     */
    public void setCoordinates(double x, double y) {
        labelX.setText(String.format("Coordinate X: %.2f", x));
        labelY.setText(String.format("Coordinate Y: %.2f", y));
    }

    /**
     * Updates the zoom label text to show the current magnification scale.
     * * @param scale the active scaling factor (e.g., 1.50 for 150% zoom)
     */
    public void setScale(double scale) {
        labelScale.setText(String.format("Zoom: %.2f x", scale));
    }
}