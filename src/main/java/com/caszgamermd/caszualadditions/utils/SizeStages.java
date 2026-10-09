package com.caszgamermd.caszualadditions.utils;

public final class SizeStages {
    private static final double[] STAGES = {0.125, 0.25, 0.5, 1.0, 1.25, 1.5, 2.0, 2.5};
    private static final double EPSILON = 0.00001;
    private SizeStages() {}
    public static double next(double current, boolean grow) {
        if (grow) {
            for (double stage : STAGES) if (stage > current + EPSILON) return stage;
            return STAGES[STAGES.length - 1];
        }
        for (int i = STAGES.length - 1; i >= 0; i--) if (STAGES[i] < current - EPSILON) return STAGES[i];
        return STAGES[0];
    }
}