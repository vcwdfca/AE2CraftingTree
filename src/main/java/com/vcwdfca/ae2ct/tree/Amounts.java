package com.vcwdfca.ae2ct.tree;

public record Amounts(long missing, long stored, long craft) {
    public static Amounts empty() {
        return new Amounts(0, 0, 0);
    }

    public boolean hasMissing() {
        return missing > 0;
    }
}
