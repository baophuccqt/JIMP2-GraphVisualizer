import gui.MainWindow;

import javax.swing.*;
import java.io.*;

class Main {
    final public static String fileName = "./test-graphs/test.txt";

    public static void main(String[] args) throws FileNotFoundException {
        SwingUtilities.invokeLater(() -> new MainWindow());

    }
}
