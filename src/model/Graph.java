package model;

import java.util.ArrayList;
import java.util.List;

public class Graph {
    //public lists containing nodes and edges
    public List<Node> nodes;
    public List<Edge> edges;

    //graph constructor
    public Graph() {
        this.nodes = new ArrayList<Node>();
        this.edges = new ArrayList<Edge>();
    }
}
