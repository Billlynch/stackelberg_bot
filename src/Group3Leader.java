import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

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

	private ArrayList<Record> records = new ArrayList<Record>();
	private static final int WINDOW_SIZE = 20;

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
		for (int i = 0; i < WINDOW_SIZE; i++) {
			records.add(m_platformStub.query(m_type, i + 1)); // 1 indexed..
			// m_platformStub.log(m_type, "Output: " +
			// String.valueOf(m_platformStub.query(m_type, i).m_followerPrice));
		}
		ReactionFunction reactionFunction = new ReactionFunction(records);
		m_platformStub.log(m_type, String.valueOf(records.size() + " Records slurped"));

		m_platformStub.log(m_type, String.valueOf(reactionFunction.reactionDifferenceFromActual + " difference"));
	}

	/**
	 * To inform this instance to proceed to a new simulation day
	 * 
	 * @param p_date The date of the new day
	 * @throws RemoteException
	 */
	@Override
	public void proceedNewDay(int p_date) throws RemoteException {
		m_platformStub.publishPrice(m_type, genPrice());
	}

	/**
	 * @return The generated price
	 */
	private float genPrice() {
		// Find max of each graph for each window size.
		// Once max found, find the solution that corresponds to that maximum (providied x > 1).
		// That solution is the price.

		ReactionFunction reactionFunction = new ReactionFunction(records);

		double[] equationCoeffs = getEquationCoeffs(reactionFunction);

		return findPriceThatMaximisesGraph(equationCoeffs);
	}

	private float findPriceThatMaximisesGraph(double[] equationCoeffs) {

		float max = findMaxOfGraph(equationCoeffs);

		// solve: xSquaredCoeff x^2 + xCoeff x + (constant - max) = 0

		LaguerreSolver solver = new LaguerreSolver();
		Complex[] complexRoots = solver.solveAllComplex(equationCoeffs, 1);
		float price = 1f;
		for (Complex root : complexRoots) {
			if (root.getReal() > 1) {
				price = (float) root.getReal();
			}
		}
		return price;
	}

	private float findMaxOfGraph(double[] equationCoeffs) {
		// Max = c - (b^2 / 4a)
		return (float) (equationCoeffs[0] - ((equationCoeffs[1] * equationCoeffs[1]) / (4 * equationCoeffs[2])));
	}

	private double[] getEquationCoeffs(ReactionFunction reactionFunction) {
		// Formula: (0.3b - 1)x^2 + (0.3a - 0.3b + 3)x + (- 0.3a - 2)
		double xSquaredCoeff = 0.3 * reactionFunction.bPrime - 1;
		double xCoeff = 0.3 * reactionFunction.aPrime - 0.3 * reactionFunction.bPrime + 3;
		double constant = -0.3 * reactionFunction.aPrime - 2;

		return new double[] {constant, xCoeff, xSquaredCoeff};
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
			float T = records.size();
			float sumOfX = 0;
			float sumOfY = 0;
			float sumOfXY = 0;
			float sumOfXSquared = 0;

			for (int i = 0; i < records.size(); i++) {
				sumOfX += records.get(i).m_leaderPrice;
				sumOfY += records.get(i).m_followerPrice;

				sumOfXY += (records.get(i).m_leaderPrice * records.get(i).m_followerPrice);
				sumOfXSquared += (records.get(i).m_leaderPrice * records.get(i).m_leaderPrice);
			}

			float result = ((sumOfXSquared * sumOfY) - (sumOfX * sumOfXY)) / ((T * sumOfXSquared) - (sumOfX * sumOfX));

			return result;
		}

		private float calculateB() {
			float T = records.size();
			float sumOfX = 0;
			float sumOfY = 0;
			float sumOfXY = 0;
			float sumOfXSquared = 0;

			for (int i = 0; i < records.size(); i++) {
				sumOfX += records.get(i).m_leaderPrice;
				sumOfY += records.get(i).m_followerPrice;

				sumOfXY += (records.get(i).m_leaderPrice * records.get(i).m_followerPrice);
				sumOfXSquared += (records.get(i).m_leaderPrice * records.get(i).m_leaderPrice);
			}

			float result = ((T * sumOfXY) - (sumOfX * sumOfY)) / ((T * sumOfXSquared) - (sumOfX * sumOfX));

			return result;
		}

		private float reactionDifferenceFromActual() {
			float result = 0;
			for (int i = 0; i < records.size(); i++) {
				float temp = records.get(i).m_followerPrice - (aPrime + bPrime * records.get(i).m_leaderPrice);
				result += (temp * temp);
			}

			return result;
		}
	}
}
