package com.celfinder.Model;

import java.util.Map;

public class ProductRatingStats {
    private double averageRating;
    private int totalReviews;
    private Map<Integer, Double> starPercentages;

    public ProductRatingStats(double averageRating, int totalReviews, Map<Integer, Double> starPercentages) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.starPercentages = starPercentages;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public Map<Integer, Double> getStarPercentages() {
        return starPercentages;
    }
}
