package algorithm;

import model.Graph;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public class PanamaTutte extends LayoutAlgorithm {

    // 1. Układ pamięci dla struktury 'Node' z graph.h
    private static final GroupLayout NATIVE_NODE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"), // char* name (8 bajtów)
            ValueLayout.JAVA_DOUBLE.withName("x"),    // double x   (8 bajtów)
            ValueLayout.JAVA_DOUBLE.withName("y")     // double y   (8 bajtów)
    );

    // 2. Układ pamięci dla struktury 'Edge' z graph.h
    private static final GroupLayout NATIVE_EDGE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),         // char* name (8 bajtów)
            ValueLayout.JAVA_INT.withName("node1_index"),  // int node1_index (4 bajty)
            ValueLayout.JAVA_INT.withName("node2_index"),  // int node2_index (4 bajty)
            ValueLayout.JAVA_DOUBLE.withName("weight")     // double weight (8 bajtów)
    );

    // 3. Układ pamięci dla struktury 'Graph' z graph.h
    private static final GroupLayout NATIVE_GRAPH_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("nodes"),   // Node* nodes    (8 bajtów)
            ValueLayout.ADDRESS.withName("edges"),   // Edge* edges    (8 bajtów)
            ValueLayout.JAVA_INT.withName("n_count"), // int n_count    (4 bajty)
            ValueLayout.JAVA_INT.withName("e_count")  // int e_count    (4 bajty)
    );

    // Dynamiczne uchwyty do pól struktur dla bezpiecznego zapisu bez ręcznego liczenia bajtów
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

    private static MethodHandle runTutteHandle;

    static {
        try {
            System.loadLibrary("graphalgo");
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            Linker linker = Linker.nativeLinker();

            // Zakładamy, że funkcja w C nazywa się "run_tutte" (zweryfikuj z nagłówkiem tutte.h / graph.h)
            MemorySegment functionAddress = lookup.find("run_tutte").orElseThrow(() ->
                    new UnsatisfiedLinkError("Nie znaleziono funkcji run_tutte w pliku DLL")
            );

            // Sygnatura w C: void run_tutte(Graph* g)
            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS, // Graph* g
                    ValueLayout.JAVA_INT  // int iterations
            );
            runTutteHandle = linker.downcallHandle(functionAddress, descriptor);

        } catch (UnsatisfiedLinkError e) {
            System.err.println("Błąd ładowania biblioteki DLL w PanamaTutte. Upewnij się, że plik dll jest w java.library.path.");
            e.printStackTrace();
        }
    }
    @Override
    public void layout(Graph javaGraph) {
        int nCount = javaGraph.nodes.size();
        if (nCount == 0 || runTutteHandle == null) return;

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

                MemorySegment nativeString = arena.allocateFrom("ID_" + jNode.id, java.nio.charset.StandardCharsets.UTF_8);

                NODE_NAME.set(nodeElement, 0L, nativeString);
                // Przekazujemy pozycje startowe (nawet jeśli to 0.0)
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

                int idx1 = -1;
                int idx2 = -1;
                String targetStart = String.valueOf(jEdge.startNode).replace("ID_", "").trim();
                String targetEnd = String.valueOf(jEdge.endNode).replace("ID_", "").trim();

                for (int k = 0; k < nCount; k++) {
                    String currentId = String.valueOf(javaGraph.nodes.get(k).id).replace("ID_", "").trim();
                    if (currentId.equals(targetStart)) idx1 = k;
                    if (currentId.equals(targetEnd)) idx2 = k;
                }

                if (idx1 == -1) idx1 = 0;
                if (idx2 == -1) idx2 = 0;

                EDGE_NODE1.set(edgeElement, 0L, idx1);
                EDGE_NODE2.set(edgeElement, 0L, idx2);
                EDGE_WEIGHT.set(edgeElement, 0L, (double) jEdge.len);
            }

            // ==========================================
            // 3. BUDOWANIE STRUKTURY GRAPH DLA C
            // ==========================================
            MemorySegment nativeGraphStruct = arena.allocate(NATIVE_GRAPH_LAYOUT);
            GRAPH_NODES.set(nativeGraphStruct, 0L, nativeNodesArray);
            GRAPH_EDGES.set(nativeGraphStruct, 0L, nativeEdgesArray);
            GRAPH_N_COUNT.set(nativeGraphStruct, 0L, nCount);
            GRAPH_E_COUNT.set(nativeGraphStruct, 0L, eCount);

            // ==========================================
            // 4. URUCHOMIENIE ALGORYTMU TUTTE'A W C (Z LICZBĄ ITERACJI)
            // ==========================================
            // Przekazujemy dwa parametry: strukturę grafu oraz np. 100 iteracji wygładzania
            int numberOfIterations = 100;
            runTutteHandle.invokeExact(nativeGraphStruct, numberOfIterations);

            // ==========================================
            // 5. PRZEPISANIE NOWYCH POZYCJI Z TRANSLACJĄ NA ŚRODEK EKRANU
            // ==========================================
            for (int i = 0; i < nCount; i++) {
                model.Node jNode = javaGraph.nodes.get(i);
                MemorySegment nodeElement = nativeNodesArray.asSlice(
                        i * NATIVE_NODE_LAYOUT.byteSize(),
                        NATIVE_NODE_LAYOUT.byteSize()
                );

                double rawX = (double) NODE_X.get(nodeElement, 0L);
                double rawY = (double) NODE_Y.get(nodeElement, 0L);

                // Ponieważ C wyśrodkowało punkty w układzie (0,0) z promieniem 100,
                // przesuwamy je w Javie na punkt (400, 400) i skalujemy x2 dla lepszej widoczności:
                jNode.X = 400.0 + (rawX * 2.5);
                jNode.Y = 400.0 + (rawY * 2.5);
            }

        } catch (Throwable t) {
            System.err.println("Wystąpił błąd podczas wywołania PanamaTutte:");
            t.printStackTrace();
        }
    }

}