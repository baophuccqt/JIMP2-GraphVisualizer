package io;

import model.Edge;
import model.Graph;
import model.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;


/**
 * Utility class responsible for reading graph data from a text file
 * and populating a Graph object.
 */
public class GraphReader {

    /**
     * Reads a graph definition file where each line represents an edge connection.
     * Automatically creates unique nodes based on the referenced IDs.
     * * @param filename                 the path to the text file containing the graph data
     * @return                         a populated Graph object with nodes and edges
     * @throws FileNotFoundException   if the specified file does not exist
     */
    public Graph read(String filename) throws FileNotFoundException {
        Graph graph = new Graph();
        Scanner sc = new Scanner(new File(filename));
        // A set to keep track of already created node IDs to prevent duplicates
        Set<Integer> seen = new HashSet<Integer>();

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            //Skips empty lines
            if (line.isEmpty()) continue;

            // Split the line by any whitespace character (spaces, tabs, etc.)
            String[] parts = line.split("\\s+");
            // Ensure the line contains all 4 required components
            if (parts.length < 4) continue;

            // Parse edge data from the text tokens
            String edgeName = parts[0];
            int startNode = Integer.parseInt(parts[1]);
            int endNode = Integer.parseInt(parts[2]);
            double edgeWeight = Double.parseDouble(parts[3]);

            //add edge to graph
            graph.edges.add(new Edge(edgeName, startNode, endNode, edgeWeight));

            //if statements making sure that each node present in the input file gets added to the graph
            if (seen.add(startNode)) {
                graph.nodes.add(new Node(startNode, 0, 0));
            }

            if (seen.add(endNode)) {
                graph.nodes.add(new Node(endNode, 0, 0));
            }
        }
        //Close scanner to prevent memory leaks
        sc.close();

        return graph;
    }
}
