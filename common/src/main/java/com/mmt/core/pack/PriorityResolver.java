package com.mmt.core.pack;

import com.mmt.core.data.model.TranslateMethod;

public class PriorityResolver {
    public static boolean shouldOverride(String existingMethod, String newMethod) {
        int existingPriority = getPriority(existingMethod);
        int newPriority = getPriority(newMethod);

        if (newPriority < existingPriority) {
            return true;
        }

        if (newPriority == existingPriority) {
            return true;
        }

        return false;
    }

    public static int getPriority(String method) {
        if (method == null || method.isEmpty()) {
            return 0;
        }

        TranslateMethod tm = TranslateMethod.fromString(method);
        if (tm != null) {
            return tm.getPriority();
        }

        return 0;
    }

    public static boolean areSamePriority(String method1, String method2) {
        TranslateMethod tm1 = TranslateMethod.fromString(method1);
        TranslateMethod tm2 = TranslateMethod.fromString(method2);

        if (tm1 == null || tm2 == null) {
            return false;
        }

        return tm1.comparePriority(tm2) == 0;
    }
}