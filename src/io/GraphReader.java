package io;

import model.Edge;
import model.Graph;
import model.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class GraphReader {

    public Graph read(String filename) throws FileNotFoundException {
        Graph graph = new Graph();
        Scanner sc = new Scanner(new File(filename));
        Set<Integer> seen = new HashSet<Integer>(); // to check if the node exists

        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length < 4) continue;

            String edgeName = parts[0];
            int startNode = Integer.parseInt(parts[1]);
            int endNode = Integer.parseInt(parts[2]);
            double edgeWeight = Double.parseDouble(parts[3]);

            graph.edges.add(new Edge(edgeName, startNode, endNode, edgeWeight));

            if (seen.add(startNode)) {
                graph.nodes.add(new Node(startNode, 0, 0));
            }

            if (seen.add(endNode)) {
                graph.nodes.add(new Node(endNode, 0, 0));
            }
        }

        return graph;
    }
}
