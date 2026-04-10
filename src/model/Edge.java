package model;

public class Edge {
    public String name;
    public int startNode;
    public int endNode;
    public double len;

    public Edge(String name, int st, int en, double len) {
        this.name = name;
        this.startNode = st;
        this.endNode = en;
        this.len = len;
    }
}
