package com.microswitch.application.random;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Getter
public class UniqueRandomGenerator {
    public final List<Integer> uniqueValues;
    private int index = 0;

    public UniqueRandomGenerator(int range) {
        if (range <= 0) {
            log.error("[MICROSWITCH-EXCEPTION] - Invalid range value: {}, must be positive", range);
            throw new IllegalArgumentException("Range must be positive, got: " + range);
        }
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
