package se.nefalas;

import java.util.Arrays;

class Utils {
    static boolean intArrayContains(int[] array, int value) {
        return Arrays.stream(array).anyMatch(i -> i == value);
    }

    static boolean intArrayDoesNotContain(int[] array, int value) {
        return Arrays.stream(array).noneMatch(i -> i == value);
    }
}
