package model;

public class Edge {
    //edge id in string format
    public String name;
    public int startNode;
    public int endNode;
    //length or weight of edge
    public double len;

    //constructor assigning all values
    public Edge(String name, int st, int en, double len) {
        this.name = name;
        this.startNode = st;
        this.endNode = en;
        this.len = len;
    }
}
