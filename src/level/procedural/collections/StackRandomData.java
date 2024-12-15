package level.procedural.collections;

import java.util.ArrayList;

public record StackRandomData(int layer, int lastValue, int lastSize) {
    public static int getLastStackSize(ArrayList<Integer> left, ArrayList<Integer> right) {
        return left.get(left.size() - 1) + right.get(right.size() - 1) + 1;
    }
}
