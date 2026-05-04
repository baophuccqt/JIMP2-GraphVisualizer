package gui;

import javax.swing.*;
import java.awt.*;

public class FilePanel extends JPanel {
    public JButton fileType;
    public JButton readBtn;
    public JButton exportFile;

    public FilePanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setPreferredSize(new Dimension(0, 40));

        fileType = new JButton("Text:\nBinary\n");
        readBtn = new JButton("Choose file");
        exportFile = new JButton("Export");

        add(fileType);
        add(readBtn);
        add(exportFile);

        setAlignmentX(Component.RIGHT_ALIGNMENT);
        setAlignmentY(Component.BOTTOM_ALIGNMENT);
    }

}
