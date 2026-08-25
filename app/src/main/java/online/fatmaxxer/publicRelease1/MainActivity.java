package com.example.ble;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final Map<String, File> currentLogFiles = new HashMap<>();
    private final Map<Double, double[]> detrendingFactorMatrices = new HashMap<>();
    
    private Log logManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logManager = new Log();
    }

    public void detrendingFactorMatrix(double lambda, int T) {
        if (!detrendingFactorMatrices.containsKey(lambda) || detrendingFactorMatrices.get(lambda).length <= T) {
            int newSize = Math.max(T + 100, 440);
            detrendingFactorMatrices.put(lambda, new double[newSize]);
        }
    }

    public void deleteCurrentLogFiles() {
        for (Map.Entry<String, File> entry : currentLogFiles.entrySet()) {
            File file = entry.getValue();
            if (file != null && file.exists()) {
                deleteFile(file);
            }
        }
    }

    private void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public double[] polyFit(double[] x, double[] y, int degree) {
        int n = x.length;
        int m = degree + 1;
        double[][] A = new double[m][m];
        double[] B = new double[m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                double val = Math.pow(x[i], j);
                for (int k = 0; k < m; k++) {
                    A[j][k] += Math.pow(x[i], k) * val;
                }
                B[j] += y[i] * val;
            }
        }

        for (int i = 0; i < m; i++) {
            int maxRow = i;
            for (int k = i + 1; k < m; k++) {
                if (Math.abs(A[k][i]) > Math.abs(A[maxRow][i])) {
                    maxRow = k;
                }
            }
            double[] tempRow = A[i];
            A[i] = A[maxRow];
            A[maxRow] = tempRow;

            double tempB = B[i];
            B[i] = B[maxRow];
            B[maxRow] = tempB;

            if (Math.abs(A[i][i]) < 1e-12) {
                continue;
            }

            for (int k = i + 1; k < m; k++) {
                double factor = A[k][i] / A[i][i];
                B[k] -= factor * B[i];
                for (int j = i; j < m; j++) {
                    A[k][j] -= factor * A[i][j];
                }
            }
        }

        double[] result = new double[m];
        for (int i = m - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < m; j++) {
                sum += A[i][j] * result[j];
            }
            result[i] = (B[i] - sum) / A[i][i];
        }

        return result;
    }

    public double dfaAlpha1V1(double[] rrIntervals, double[] exp_scales) {
        double[] scales;
        if (exp_scales == null || exp_scales.length == 0) {
            scales = new double[]{4, 8, 12, 16};
        } else {
            scales = exp_scales.clone();
        }

        return 1.0;
    }

    public void expireLogFiles() {
        File logDir = getExternalFilesDir(null);
        if (logDir != null && logDir.exists()) {
            File[] files = logDir.listFiles();
            if (files != null) {
                long currentTime = System.currentTimeMillis();
                long maxAge = 7 * 24 * 60 * 60 * 1000L;
                for (File file : files) {
                    if (currentTime - file.lastModified() > maxAge) {
                        file.delete();
                    }
                }
            }
        }
    }

    private class Log {
        public void write(String message) {
        }
    }
}