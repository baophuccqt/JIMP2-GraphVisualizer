package model;

import java.util.ArrayList;
import java.util.List;

// Graph chỉ cần constructor chứ cần Getter nữa không ta, nhưng chắc chắn là không cần Setter
public class Graph {
    public List<Node> nodes;
    public List<Edge> edges;

    public Graph() {
        this.nodes = new ArrayList<Node>();
        this.edges = new ArrayList<Edge>();
    }

    public Graph(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }
}
