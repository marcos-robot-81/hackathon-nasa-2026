package user;

public record MetricBaseline(
    double mean,
    double stdDev
) {
    public double zScore(double sampleValue) {
        if (stdDev <= 0.0001) {
            return 0.0;
        }
        return (sampleValue - mean) / stdDev;
    }
}
