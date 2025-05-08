package com.n11.development.application.random;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
public class UniqueRandomGenerator {
    public final List<Integer> uniqueValues;
    private int index = 0;

    public UniqueRandomGenerator(int range) {
        uniqueValues = new ArrayList<>(range);
        for (int i = 0; i < range; i++) {
            uniqueValues.add(i);
        }
        shuffleValues();
    }

    public int getNextUniqueRandomValue() {
        if (index >= uniqueValues.size()) {
            shuffleValues();
            index = 0;
        }
        return uniqueValues.get(index++);
    }

    private void shuffleValues() {
        Collections.shuffle(uniqueValues, new Random());
    }
}
