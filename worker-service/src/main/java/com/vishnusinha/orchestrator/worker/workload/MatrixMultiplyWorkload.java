package com.vishnusinha.orchestrator.worker.workload;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class MatrixMultiplyWorkload implements WorkloadExecutor {

    @Override
    public String type() {
        return "MATRIX_MULTIPLY";
    }

    @Override
    public WorkloadResult execute(JsonNode payload) {
        int size = WorkloadPayloads.intValue(payload, "matrixSize", 64, 8, 128);
        double[][] left = new double[size][size];
        double[][] right = new double[size][size];
        double[][] result = new double[size][size];

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                left[row][column] = ((row + 1) * (column + 3)) % 17;
                right[row][column] = ((row + 5) * (column + 1)) % 19;
            }
        }

        double checksum = 0.0;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                double cell = 0.0;
                for (int index = 0; index < size; index++) {
                    cell += left[row][index] * right[index][column];
                }
                result[row][column] = cell;
                checksum += cell;
            }
        }

        long operations = 2L * size * size * size;
        return new WorkloadResult("matrix size %d checksum %.2f".formatted(size, checksum), operations);
    }
}
