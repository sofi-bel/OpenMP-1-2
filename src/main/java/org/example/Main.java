package org.example;

import java.util.*;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;

public class Main {

    public static void main(String[] args) {
        final int size = 1024;
        double[][] a = new double[size][size];
        double[][] b = new double[size][size];

        Random random = new Random();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                a[i][j] = random.nextDouble();
                b[i][j] = random.nextDouble();
            }
        }

        int n = Runtime.getRuntime().availableProcessors();
        int iter = 10;
        List<Future<Double[][]>> futureList = new ArrayList<>(n);

        for (int i = 1; i < iter + 1; i++) {
            futureList.clear();

            System.out.print(i + ": ");

            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(n);
            int step = size / n;
            long start = currentTimeMillis();

            for (int j = 0; j < n; j++) {
                int remains = (j == n - 1) ? size % n : 0;
                futureList.add(fixedThreadPool.submit(new MatrixClass(j * step, (j + 1) * step + remains, a, b)));
            }

            Map<Integer, Double[][]> treeMapMatrix = new TreeMap<>();
            int position = 0;

            for (Future<Double[][]> future : futureList) {
                try {
                    Double[][] matrix = future.get();
                    treeMapMatrix.put(position++, matrix);
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println(e.getMessage());
                }
            }

            Double[][] result8 = new Double[size][size];
            position = 0;

            for (Map.Entry<Integer, Double[][]> entry : treeMapMatrix.entrySet()) {
                Double[][] currentMatrix = entry.getValue();

                for (int row = 0; row < currentMatrix.length; row++) {
                    System.arraycopy(currentMatrix[row], 0, result8[row + position], 0, size);
                }

                position += currentMatrix.length;
            }

            long time = currentTimeMillis() - start;
            System.out.print("time 8 (" + time + "), ");

            try {
                start = currentTimeMillis();
                Double[][] result = fixedThreadPool.submit(new MatrixClass(0, size, a, b)).get();
                time = currentTimeMillis() - start;

                for (int r1 = 0; r1 < size; ++r1) {
                    for (int r2 = 0; r2 < size; ++r2) {
                        if (!Objects.equals(result8[r1][r2], result[r1][r2])) {
                            throw new RuntimeException();
                        }
                    }
                }

                System.out.println("time 1 (" + time + ").");
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            } finally {
                fixedThreadPool.shutdown();
            }
        }
    }

    static class MatrixClass implements Callable<Double[][]> {
        final int from;
        final int to;
        final int size;
        final double[][] a;
        final double[][] b;
        final Double[][] res;

        public MatrixClass(int from, int to, double[][] a, double[][] b) {
            this.from = from;
            this.to = to;
            size = a.length;
            this.a = a;
            this.b = b;
            res = new Double[to - from][size];
        }

        @Override
        public Double[][] call() {
            for (int i = from; i < to; ++i) {
                for (int j = 0; j < size; ++j) {
                    int iResult = i - from;
                    res[iResult][j] = 0d;

                    for (int j2 = 0; j2 < size; ++j2) {
                        res[iResult][j] += a[i][j2] * b[j2][j];
                    }
                }
            }

            return res;
        }
    }
}
