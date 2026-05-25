package algorithm;

import model.Graph;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public class PanamaFruchterman extends LayoutAlgorithm {

    // 1. Układ pamięci dla struktury 'Node' z graph.h
    private static final GroupLayout NATIVE_NODE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),
            ValueLayout.JAVA_DOUBLE.withName("x"),
            ValueLayout.JAVA_DOUBLE.withName("y")
    );

    // 2. Układ pamięci dla struktury 'Edge' z graph.h
    private static final GroupLayout NATIVE_EDGE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),
            ValueLayout.JAVA_INT.withName("node1_index"),
            ValueLayout.JAVA_INT.withName("node2_index"),
            ValueLayout.JAVA_DOUBLE.withName("weight")
    );

    // 3. Układ pamięci dla struktury 'Graph' z graph.h
    private static final GroupLayout NATIVE_GRAPH_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("nodes"),
            ValueLayout.ADDRESS.withName("edges"),
            ValueLayout.JAVA_INT.withName("n_count"),
            ValueLayout.JAVA_INT.withName("e_count")
    );

    // Dynamiczne uchwyty do pól struktur (eliminują potrzebę ręcznego wpisywania offsetów typu 8, 12, 16)
    private static final VarHandle NODE_NAME = NATIVE_NODE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("name"));
    private static final VarHandle NODE_X = NATIVE_NODE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("x"));
    private static final VarHandle NODE_Y = NATIVE_NODE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("y"));

    private static final VarHandle EDGE_NAME = NATIVE_EDGE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("name"));
    private static final VarHandle EDGE_NODE1 = NATIVE_EDGE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("node1_index"));
    private static final VarHandle EDGE_NODE2 = NATIVE_EDGE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("node2_index"));
    private static final VarHandle EDGE_WEIGHT = NATIVE_EDGE_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("weight"));

    private static final VarHandle GRAPH_NODES = NATIVE_GRAPH_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("nodes"));
    private static final VarHandle GRAPH_EDGES = NATIVE_GRAPH_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("edges"));
    private static final VarHandle GRAPH_N_COUNT = NATIVE_GRAPH_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("n_count"));
    private static final VarHandle GRAPH_E_COUNT = NATIVE_GRAPH_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("e_count"));

    private static MethodHandle runFruchtermanHandle;

    static {
        try {
            System.loadLibrary("graphalgo");
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            Linker linker = Linker.nativeLinker();

            MemorySegment functionAddress = lookup.find("run_fruchterman").orElseThrow(() ->
                    new UnsatisfiedLinkError("Nie znaleziono funkcji run_fruchterman w pliku DLL")
            );

            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
            runFruchtermanHandle = linker.downcallHandle(functionAddress, descriptor);

        } catch (UnsatisfiedLinkError e) {
            System.err.println("Błąd ładowania biblioteki DLL. Upewnij się, że archiwum graphalgo.dll znajduje się w java.library.path.");
            e.printStackTrace();
        }
    }

    @Override
    public void layout(Graph javaGraph) {
        int nCount = javaGraph.nodes.size();
        if (nCount == 0 || runFruchtermanHandle == null) return;

        try (Arena arena = Arena.ofConfined()) {

            // ==========================================
            // 1. ALOKACJA I KOPIOWANIE WIERZCHOŁKÓW (NODES)
            // ==========================================
            MemorySegment nativeNodesArray = arena.allocate(NATIVE_NODE_LAYOUT, nCount);

            for (int i = 0; i < nCount; i++) {
                model.Node jNode = javaGraph.nodes.get(i);
                MemorySegment nodeElement = nativeNodesArray.asSlice(
                        i * NATIVE_NODE_LAYOUT.byteSize(),
                        NATIVE_NODE_LAYOUT.byteSize()
                );

                // Alokacja nazwy wierzchołka z poprawnym kodowaniem UTF-8
                MemorySegment nativeString = arena.allocateFrom("ID_" + jNode.id, java.nio.charset.StandardCharsets.UTF_8);

                // Zapis danych przy użyciu VarHandle i wymaganego przesunięcia 0L
                NODE_NAME.set(nodeElement, 0L, nativeString);
                NODE_X.set(nodeElement, 0L, (double) jNode.X);
                NODE_Y.set(nodeElement, 0L, (double) jNode.Y);
            }

            // ==========================================
            // 2. ALOKACJA I KOPIOWANIE KRAWĘDZI (EDGES)
            // ==========================================
            int eCount = javaGraph.edges.size();
            MemorySegment nativeEdgesArray = arena.allocate(NATIVE_EDGE_LAYOUT, eCount);

            for (int i = 0; i < eCount; i++) {
                model.Edge jEdge = javaGraph.edges.get(i);
                MemorySegment edgeElement = nativeEdgesArray.asSlice(
                        i * NATIVE_EDGE_LAYOUT.byteSize(),
                        NATIVE_EDGE_LAYOUT.byteSize()
                );

                MemorySegment edgeName = arena.allocateFrom("E_" + i, java.nio.charset.StandardCharsets.UTF_8);
                EDGE_NAME.set(edgeElement, 0L, edgeName);

                // Oczyszczanie ID z przedrostków tekstowych w celu dokładnego porównania typu String
                int idx1 = -1;
                int idx2 = -1;
                String targetStart = String.valueOf(jEdge.startNode).replace("ID_", "").trim();
                String targetEnd = String.valueOf(jEdge.endNode).replace("ID_", "").trim();

                for (int k = 0; k < nCount; k++) {
                    String currentId = String.valueOf(javaGraph.nodes.get(k).id).replace("ID_", "").trim();
                    if (currentId.equals(targetStart)) {
                        idx1 = k;
                    }
                    if (currentId.equals(targetEnd)) {
                        idx2 = k;
                    }
                }

                // Zabezpieczenie na wypadek braku dopasowania – zapobiega ujemnym indeksom w pamięci C
                if (idx1 == -1) idx1 = 0;
                if (idx2 == -1) idx2 = 0;

                // Zapis struktury krawędzi przy użyciu VarHandle i przesunięcia 0L
                EDGE_NODE1.set(edgeElement, 0L, idx1);
                EDGE_NODE2.set(edgeElement, 0L, idx2);
                EDGE_WEIGHT.set(edgeElement, 0L, (double) jEdge.len);
            }

            // ==========================================
            // 3. BUDOWANIE GŁÓWNEJ STRUKTURY GRAPH
            // ==========================================
            MemorySegment nativeGraphStruct = arena.allocate(NATIVE_GRAPH_LAYOUT);
            GRAPH_NODES.set(nativeGraphStruct, 0L, nativeNodesArray);
            GRAPH_EDGES.set(nativeGraphStruct, 0L, nativeEdgesArray);
            GRAPH_N_COUNT.set(nativeGraphStruct, 0L, nCount);
            GRAPH_E_COUNT.set(nativeGraphStruct, 0L, eCount);

            // ==========================================
            // 4. URUCHOMIENIE OBLICZEŃ NATYWNYCH W PLIKU DLL
            // ==========================================
            runFruchtermanHandle.invokeExact(nativeGraphStruct);

            // ==========================================
            // 5. ODEBRANIE I AKTUALIZACJA WSPÓŁRZĘDNYCH W JAVIE
            // ==========================================
            for (int i = 0; i < nCount; i++) {
                model.Node jNode = javaGraph.nodes.get(i);
                MemorySegment nodeElement = nativeNodesArray.asSlice(
                        i * NATIVE_NODE_LAYOUT.byteSize(),
                        NATIVE_NODE_LAYOUT.byteSize()
                );

                // Pobranie nowych współrzędnych X i Y z pamięci natywnej
                jNode.X = (double) NODE_X.get(nodeElement, 0L);
                jNode.Y = (double) NODE_Y.get(nodeElement, 0L);
            }

        } catch (Throwable t) {
            System.err.println("Wystąpił błąd podczas wywołania PanamaFruchterman:");
            t.printStackTrace();
        }
    }
}