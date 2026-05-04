package gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ViewPort extends JPanel {
    public ViewPort() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(300, 200));
        setBackground(Color.CYAN);
    }
}
