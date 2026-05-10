package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Component
public class GraphTraversalWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "GRAPH_TRAVERSAL";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int nodes = WorkloadPayloads.intValue(payload, "nodes", 2_000, 100, 10_000);
        int edgesPerNode = WorkloadPayloads.intValue(payload, "edgesPerNode", 4, 1, 16);

        List<List<Integer>> graph = new ArrayList<>(nodes);
        for (int node = 0; node < nodes; node++) {
            List<Integer> edges = new ArrayList<>(edgesPerNode);
            for (int offset = 1; offset <= edgesPerNode; offset++) {
                edges.add((node * 31 + offset * 17) % nodes);
            }
            graph.add(edges);
        }

        boolean[] visited = new boolean[nodes];
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(0);
        visited[0] = true;
        int visitedCount = 0;

        while (!queue.isEmpty()) {
            int node = queue.remove();
            visitedCount++;
            for (int next : graph.get(node)) {
                if (!visited[next]) {
                    visited[next] = true;
                    queue.add(next);
                }
            }
        }

        long edgesScanned = (long) visitedCount * edgesPerNode;
        return new WorkloadResult("visited %d of %d nodes".formatted(visitedCount, nodes), edgesScanned);
    }
}
