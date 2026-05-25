package algorithm;

import model.Graph;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * Layout algorithm implementation using Project Panama (Foreign Function & Memory API)
 * to offload Tutte's barycentric embedding algorithm calculations to a native C library.
 */
public class PanamaTutte extends LayoutAlgorithm {

    // 1. Memory layout matching the native 'Node' struct defined in C (graph.h)
    private static final GroupLayout NATIVE_NODE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"), // char* name (8 bytes on 64-bit)
            ValueLayout.JAVA_DOUBLE.withName("x"),    // double x   (8 bytes)
            ValueLayout.JAVA_DOUBLE.withName("y")     // double y   (8 bytes)
    );

    // 2. Memory layout matching the native 'Edge' struct defined in C (graph.h)
    private static final GroupLayout NATIVE_EDGE_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("name"),         // char* name       (8 bytes)
            ValueLayout.JAVA_INT.withName("node1_index"),  // int node1_index (4 bytes)
            ValueLayout.JAVA_INT.withName("node2_index"),  // int node2_index (4 bytes)
            ValueLayout.JAVA_DOUBLE.withName("weight")     // double weight   (8 bytes)
    );

    // 3. Memory layout matching the native main 'Graph' struct defined in C (graph.h)
    private static final GroupLayout NATIVE_GRAPH_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("nodes"),   // Node* nodes    (8 bytes, pointer to array)
            ValueLayout.ADDRESS.withName("edges"),   // Edge* edges    (8 bytes, pointer to array)
            ValueLayout.JAVA_INT.withName("n_count"), // int n_count    (4 bytes, total nodes count)
            ValueLayout.JAVA_INT.withName("e_count")  // int e_count    (4 bytes, total edges count)
    );

    // Dynamic variable handles to safely access individual struct members without hardcoded byte offsets
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

    // Method handle serving as a direct invocation bridge to the C function
    private static MethodHandle runTutteHandle;

    static {
        try {
            // Load the shared native library compiled from C
            System.loadLibrary("graphalgo");
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            Linker linker = Linker.nativeLinker();

            // Locate the function symbol inside the loaded library
            MemorySegment functionAddress = lookup.find("run_tutte").orElseThrow(() ->
                    new UnsatisfiedLinkError("Could not find run_tutte function in the native library")
            );

            // Define the C function signature: void run_tutte(Graph* g, int iterations)
            FunctionDescriptor descriptor = FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS, // Parameter 1: Graph* g (pointer)
                    ValueLayout.JAVA_INT  // Parameter 2: int iterations
            );

            // Create the downcall handle allowing Java to call the native function
            runTutteHandle = linker.downcallHandle(functionAddress, descriptor);

        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error loading native library in PanamaTutte. Ensure graphalgo library is available in java.library.path.");
            e.printStackTrace();
        }
    }

    @Override
    public void layout(Graph javaGraph) {
        int nCount = javaGraph.nodes.size();
        if (nCount == 0 || runTutteHandle == null) return;

        // Use a bounded Arena to safely allocate off-heap memory; auto-frees memory when block exits
        try (Arena arena = Arena.ofConfined()) {

            // ==========================================
            // 1. ALLOCATE AND COPY NODES ARRAY TO NATIVE
            // ==========================================
            MemorySegment nativeNodesArray = arena.allocate(NATIVE_NODE_LAYOUT, nCount);

            for (int i = 0; i < nCount; i++) {
                model.Node jNode = javaGraph.nodes.get(i);
                // Slice the continuous array memory block to get the specific Node struct segment
                MemorySegment nodeElement = nativeNodesArray.asSlice(
                        i * NATIVE_NODE_LAYOUT.byteSize(),
                        NATIVE_NODE_LAYOUT.byteSize()
                );

                // Allocate native null-terminated C-string for the node identifier using UTF-8
                MemorySegment nativeString = arena.allocateFrom("ID_" + jNode.id, java.nio.charset.StandardCharsets.UTF_8);

                // Write initial values into the native struct memory layout
                NODE_NAME.set(nodeElement, 0L, nativeString);
                NODE_X.set(nodeElement, 0L, (double) jNode.X); // Pass initial positions (even if 0.0)
                NODE_Y.set(nodeElement, 0L, (double) jNode.Y);
            }

            // ==========================================
            // 2. ALLOCATE AND COPY EDGES ARRAY TO NATIVE
            // ==========================================
            int eCount = javaGraph.edges.size();
            MemorySegment nativeEdgesArray = arena.allocate(NATIVE_EDGE_LAYOUT, eCount);

            for (int i = 0; i < eCount; i++) {
                model.Edge jEdge = javaGraph.edges.get(i);
                MemorySegment edgeElement = nativeEdgesArray.asSlice(
                        i * NATIVE_EDGE_LAYOUT.byteSize(),
                        NATIVE_EDGE_LAYOUT.byteSize()
                );

                // Allocate native C-string for the edge name
                MemorySegment edgeName = arena.allocateFrom("E_" + i, java.nio.charset.StandardCharsets.UTF_8);
                EDGE_NAME.set(edgeElement, 0L, edgeName);

                // Strip text prefixes to accurately compare node IDs as strings and map them to C array indices
                int idx1 = -1;
                int idx2 = -1;
                String targetStart = String.valueOf(jEdge.startNode).replace("ID_", "").trim();
                String targetEnd = String.valueOf(jEdge.endNode).replace("ID_", "").trim();

                for (int k = 0; k < nCount; k++) {
                    String currentId = String.valueOf(javaGraph.nodes.get(k).id).replace("ID_", "").trim();
                    if (currentId.equals(targetStart)) idx1 = k;
                    if (currentId.equals(targetEnd)) idx2 = k;
                }

                // Fallback safe guard to prevent negative or boundary indices out of bounds in C memory
                if (idx1 == -1) idx1 = 0;
                if (idx2 == -1) idx2 = 0;

                // Write mapped indices and edge attributes into the native edge struct
                EDGE_NODE1.set(edgeElement, 0L, idx1);
                EDGE_NODE2.set(edgeElement, 0L, idx2);
                EDGE_WEIGHT.set(edgeElement, 0L, (double) jEdge.len);
            }

            // ==========================================
            // 3. BUILD THE MAIN GRAPH STRUCTURE FOR C
            // ==========================================
            MemorySegment nativeGraphStruct = arena.allocate(NATIVE_GRAPH_LAYOUT);
            GRAPH_NODES.set(nativeGraphStruct, 0L, nativeNodesArray);
            GRAPH_EDGES.set(nativeGraphStruct, 0L, nativeEdgesArray);
            GRAPH_N_COUNT.set(nativeGraphStruct, 0L, nCount);
            GRAPH_E_COUNT.set(nativeGraphStruct, 0L, eCount);

            // ==========================================
            // 4. EXECUTE NATIVE TUTTE'S ALG IN THE DLL WITH ITERATIONS
            // ==========================================
            // Pass two arguments: the graph structure pointer and the smoothing iteration count (e.g., 100)
            int numberOfIterations = 100;
            runTutteHandle.invokeExact(nativeGraphStruct, numberOfIterations);

            // ==========================================
            // 5. EXTRACT NEW COORDINATES WITH TRANSLATION TO THE CENTER
            // ==========================================
            for (int i = 0; i < nCount; i++) {
                model.Node jNode = javaGraph.nodes.get(i);
                MemorySegment nodeElement = nativeNodesArray.asSlice(
                        i * NATIVE_NODE_LAYOUT.byteSize(),
                        NATIVE_NODE_LAYOUT.byteSize()
                );

                // Fetch raw X and Y values from the native off-heap memory
                double rawX = (double) NODE_X.get(nodeElement, 0L);
                double rawY = (double) NODE_Y.get(nodeElement, 0L);

                // Since the C library centers fixed outer nodes around (0,0) within a radius of 100,
                // we shift them in Java to point (400, 400) and scale by 2.5 for optimized screen visualization:
                jNode.X = 400.0 + (rawX * 2.5);
                jNode.Y = 400.0 + (rawY * 2.5);
            }

        } catch (Throwable t) {
            System.err.println("An error occurred during PanamaTutte native execution:");
            t.printStackTrace();
        }
    }
}