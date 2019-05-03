import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import org.apache.commons.math3.analysis.solvers.*;
import org.apache.commons.math3.complex.Complex;

/**
 * A very simple leader implementation that only generates random prices
 * 
 * @author Xin
 */
final class Group3Leader extends PlayerImpl {
	/* The randomizer used to generate random price */
	private final Random m_randomizer = new Random(System.currentTimeMillis());
	private int currentDay;
	private ArrayList<Record> records = new ArrayList<Record>();
	private static final int MIN_WINDOW_SIZE = 10;
	private static final int MAX_WINDOW_SIZE = 99;


	private Group3Leader() throws RemoteException, NotBoundException {
		super(PlayerType.LEADER, "Group 3 Leader");

		System.out.println("Group 3 Leader, Online");
	}

	@Override
	public void goodbye() throws RemoteException {
		ExitTask.exit(500);
	}

	@Override
	public void startSimulation(int p_steps) throws RemoteException {
		System.out.println("started");
	}

	/**
	 * To inform this instance to proceed to a new simulation day
	 * 
	 * @param p_date The date of the new day
	 * @throws RemoteException
	 */
	@Override
	public void proceedNewDay(int p_date) throws RemoteException {
		currentDay = p_date;
		HashMap<Integer, Float> windowsSizeToDifference = new HashMap();
		HashMap<Integer, Double[]> windowsSizeToCoeficients = new HashMap();


		records.clear();
		
		for (int i = 1; i < MIN_WINDOW_SIZE; i++) {
			records.add(m_platformStub.query(m_type, p_date - i)); // 1 indexed..
		}

		for (int i = MIN_WINDOW_SIZE; i < MAX_WINDOW_SIZE; i++) {
			records.add(m_platformStub.query(m_type, p_date - i)); // 1 indexed..
			ReactionFunction reactionFunction = new ReactionFunction(records);
			windowsSizeToDifference.put(i, reactionFunction.reactionDifferenceFromActual);
			windowsSizeToCoeficients.put(i, getEquationCoeffs(reactionFunction));
		}

		// window with the minimum distance is the best one.

		Integer bestWindowSize = Collections.min(windowsSizeToDifference.entrySet(), Map.Entry.comparingByValue())
										.getKey();
		System.out.println("on day: " + p_date + " window size: " + bestWindowSize + " was chosen, with a difference of: " + windowsSizeToDifference.get(bestWindowSize) + " with C, X, X^2: " + windowsSizeToCoeficients.get(bestWindowSize)[0] + ", " +  windowsSizeToCoeficients.get(bestWindowSize)[1] + ", " +  windowsSizeToCoeficients.get(bestWindowSize)[2] + "." );

		m_platformStub.publishPrice(m_type, genPrice(windowsSizeToCoeficients.get(bestWindowSize)));
	}

	/**
	 * @return The generated price
	 */
	private float genPrice(Double[] equationCoeffs) {
		// Find max of each graph for each window size.
		// Once max found, find the solution that corresponds to that maximum (providied x > 1).
		// That solution is the price.
		return findPriceThatMaximisesGraph(equationCoeffs);
	}

	private float findPriceThatMaximisesGraph(Double[] equationCoeffs) {
		float max = findMaxOfGraph(equationCoeffs);

		double[] coefs = {0,0,0};
		coefs[0] = equationCoeffs[0] - max;
		coefs[1] = equationCoeffs[1];
		coefs[2] = equationCoeffs[2];

		// System.out.println("c - max: " + coefs[0]);

		// solve: xSquaredCoeff x^2 + xCoeff x + (constant - max) = 0
		LaguerreSolver solver = new LaguerreSolver();
		Complex[] complexRoots = solver.solveAllComplex(coefs, 1);

		float price = -1f;
		for (Complex root : complexRoots) {
			if (root.getReal() > 1) {
				price = (float) root.getReal();
			}
		}

		// it's gone wrong  - choose the average of the last 5 days or 1.75
		if (price < 0) {
			try {
				List<Record> rList = new ArrayList();
				for (int i = 1; i <= 5; i++) {
					rList.add(m_platformStub.query(m_type, currentDay - i)); // 1 indexed..
				}

				float avg = 0f;
				for (Record r : rList) {
					avg += r.m_leaderPrice;
				}
				price = avg / 5.0f;
				if (price < 1.0f) {
					price = 1.75f;
				}
			} catch (Exception e) {
				e.printStackTrace();
				price = 1.75f;
			}
		}

		return price;
	}

	private float findMaxOfGraph(Double[] equationCoeffs) {

		return (float) (equationCoeffs[0] - ((equationCoeffs[1] * equationCoeffs[1]) / (4 * equationCoeffs[2])));
	}

	private Double[] getEquationCoeffs(ReactionFunction reactionFunction) {

		double xSquaredCoeff = 0.3 * reactionFunction.bPrime - 1;
		double xCoeff = 0.3 * reactionFunction.aPrime - 0.3 * reactionFunction.bPrime + 3;
		double constant = -0.3 * reactionFunction.aPrime - 2;

		return new Double[] {constant, xCoeff, xSquaredCoeff};
	}

	public static void main(final String[] p_args) throws RemoteException, NotBoundException {
		new Group3Leader();
	}

	/**
	 * The task used to automatically exit the leader process
	 * 
	 * @author Xin
	 */
	private static class ExitTask extends TimerTask {
		static void exit(final long p_delay) {
			(new Timer()).schedule(new ExitTask(), p_delay);
		}

		@Override
		public void run() {
			System.exit(0);
		}
	}

	class ReactionFunction {
		public float aPrime;
		public float bPrime;
		public List<Record> records;
		public float reactionDifferenceFromActual;

		public ReactionFunction(List<Record> records) {
			this.records = records;
			this.aPrime = calculateA();
			this.bPrime = calculateB();
			this.reactionDifferenceFromActual = reactionDifferenceFromActual();
		}

		private float calculateA() {
			double T = records.size();
			double sumOfX = 0;
			double sumOfY = 0;
			double sumOfXY = 0;
			double sumOfXSquared = 0;

			for (int i = 0; i < records.size(); i++) {
				sumOfX += records.get(i).m_leaderPrice;
				sumOfY += records.get(i).m_followerPrice;

				sumOfXY += (records.get(i).m_leaderPrice * records.get(i).m_followerPrice);
				sumOfXSquared += (records.get(i).m_leaderPrice * records.get(i).m_leaderPrice);
			}

			double result = ((sumOfXSquared * sumOfY) - (sumOfX * sumOfXY)) / ((T * sumOfXSquared) - (sumOfX * sumOfX));
			

			return (float) result;
		}

		private float calculateB() {
			float T = records.size();
			BigDecimal TBigDecimal = BigDecimal.valueOf(records.size());
			BigDecimal sumOfX = BigDecimal.ZERO;
			BigDecimal sumOfY = BigDecimal.ZERO;
			BigDecimal sumOfXY = BigDecimal.ZERO;
			BigDecimal sumOfXSquared = BigDecimal.ZERO;

			for (int i = 0; i < T; i++) {
				sumOfX = sumOfX.add(BigDecimal.valueOf(records.get(i).m_leaderPrice));
				sumOfY = sumOfY.add(BigDecimal.valueOf(records.get(i).m_followerPrice));

				sumOfXY = sumOfXY.add(BigDecimal.valueOf((records.get(i).m_leaderPrice * records.get(i).m_followerPrice)));
				sumOfXSquared = sumOfXSquared.add(BigDecimal.valueOf(records.get(i).m_leaderPrice * records.get(i).m_leaderPrice));
			}

			BigDecimal numerator = ((TBigDecimal.multiply(sumOfXY)).subtract(sumOfX.multiply(sumOfY)));
			BigDecimal denominator1 = (TBigDecimal.multiply(sumOfXSquared));
			BigDecimal denominator2 = sumOfX.pow(2);
			BigDecimal denominator = (denominator1.subtract(denominator2));


			BigDecimal resultBigDec = numerator.divide(denominator, BigDecimal.ROUND_HALF_EVEN) ;

			float result = resultBigDec.floatValue() ;
			
			return result;
		}

		private float reactionDifferenceFromActual() {
			float result = 0;

			float temp = records.get(records.size() - 1).m_followerPrice - (aPrime + bPrime * records.get(records.size() - 1).m_leaderPrice);
			result = Math.abs(temp);

			return result;
		}
	}
}
