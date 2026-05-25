======================================================================
GRAPH VISUALIZER - RUNNING INSTRUCTIONS (WINDOWS & macOS)
======================================================================

This project uses Project Panama (Foreign Function & Memory API) to bridge
Java with native graph layout algorithms. The repository already contains
precompiled native libraries:
- 'graphalgo.dll' (for Windows)
- 'libgraphalgo.dylib' (for macOS)

Follow the instructions below to configure and run the application on your
operating system.

----------------------------------------------------------------------
PREREQUISITES (ALL SYSTEMS)
----------------------------------------------------------------------
Project Panama requires a modern Java runtime environment. Ensure you
have JDK 22 or newer installed on your computer (e.g., Oracle OpenJDK,
Temurin, or Azul Zulu).

To verify your Java version, open your Terminal / Command Prompt and run:
java -version

----------------------------------------------------------------------
CRUCIAL JVM LAUNCH FLAGS
----------------------------------------------------------------------
Because Project Panama accesses native system memory, the Java Virtual
Machine (JVM) restricts execution by default. To run the application,
you MUST explicitly pass these two arguments to Java:

1. --enable-native-access=ALL-UNNAMED
   (Allows the application to invoke foreign native libraries)
2. -Djava.library.path=.
   (Tells Java where to find the .dll or .dylib file. The dot "."
   assumes the library file is located in the root folder of the project)

======================================================================
WINDOWS RUN GUIDE
======================================================================

Option A: Running via IntelliJ IDEA (Recommended)
1. Open the project in IntelliJ IDEA.
2. Click on the run configuration dropdown menu (next to the green
   "Run" play arrow) and select "Edit Configurations...".
3. In the configuration window, look for the "Modify options" dropdown
   and click "Add VM options".
4. In the new input field that appears, paste the following line:

   --enable-native-access=ALL-UNNAMED -Djava.library.path=.

5. Click "Apply" and run the application.

Option B: Running via Command Prompt
Navigate to your project directory and run the following command (adjust
the classpath "-cp" if your compiled .class files are in a different folder
like "out" or "target"):

java --enable-native-access=ALL-UNNAMED -Djava.library.path=. -cp bin gui.Main


======================================================================
macOS RUN GUIDE
======================================================================

Option A: Running via IntelliJ IDEA (Recommended)
1. Open the project in IntelliJ IDEA on your Mac.
2. Go to the run configuration dropdown next to the green play button
   and click "Edit Configurations...".
3. Click "Modify options" and select "Add VM options".
4. In the input box, paste the exact same flags:

   --enable-native-access=ALL-UNNAMED -Djava.library.path=.

5. Click "Apply" and click "Run".

Option B: Running via Terminal
Open Terminal, navigate to your project directory, and execute:

java --enable-native-access=ALL-UNNAMED -Djava.library.path=. -cp bin gui.Main


----------------------------------------------------------------------
IMPORTANT: Bypassing macOS Gatekeeper Security Restrictions
----------------------------------------------------------------------
The first time you select a graph layout algorithm (Tutte or Fruchterman)
on a Mac, macOS security (Gatekeeper) will intercept the untrusted local
binary and show a popup saying:
"libgraphalgo.dylib cannot be opened because it is from an unidentified
developer."

To authorize the library and let the program run:
1. Close the Java application (if it froze) and open your Mac's
   "System Settings".
2. Go to "Privacy & Security" and scroll down to the bottom.
3. You will see a security note stating that 'libgraphalgo.dylib'
   was blocked. Click the "Allow Anyway" button next to it.
4. Authenticate using your Mac's password or Touch ID.
5. Rerun the program in IntelliJ or Terminal. Click the algorithm button
   again—macOS will now safely execute the native code without issues!
   ======================================================================