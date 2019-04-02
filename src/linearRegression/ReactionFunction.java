import java.util.List;

public class ReactionFunction {
    public float aPrime;
    public float bPrime;
    public List<Float> x;
    public List<Float> y;
    public float reactionDifferenceFromActual;

    public ReactionFunction(List<Float> x, List<Float> y) {
        this.x = x.clone();
        this.y = y.clone();
        this.aPrime = calculateA();
        this.bPrime = calculateB();
        this.reactionDifferenceFromActual = reactionDifferenceFromActual();
    }

    private float calculateA() {
        float T = x.size();
        float sumOfX = x.stream().sum();
        float sumOfY = y.stream().sum();
        float sumOfXY = 0;
        float sumOfXSquared = 0;

        for (int i = 0; i < x.size(); i++) {
            sumOfXY += (x.get(i) * y.get(i));
            sumOfXSquared += (x.get(i) * x.get(i));
        }

        float result = ((sumOfXSquared * sumOfY) - (sumOfX * sumOfXY)) / ((T * sumOfXSquared) - (sumOfX * sumOfX));

        return result;
    }

    private float calculateB() {
        float T = x.size();
        float sumOfX = x.stream().sum();
        float sumOfY = y.stream().sum();
        float sumOfXY = 0;
        float sumOfXSquared = 0;

        for (int i = 0; i < x.size(); i++) {
            sumOfXY += (x.get(i) * y.get(i));
            sumOfXSquared += (x.get(i) * x.get(i));
        }

        float result = ((T * sumOfXY) - (sumOfX * sumOfY)) / ((T * sumOfXSquared) - (sumOfX * sumOfX));

        return result;
    }

    private float reactionDifferenceFromActual() {
        float result = 0;
        for (int i = 0; i < x.size(); i++) {
            float temp = y.get(i) - (aPrime + bPrime * x.get(i));
            result += (temp * temp);
        }

        return result;
    }
}