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

/**
 * A very simple leader implementation that only generates random prices
 * @author Xin
 */
final class Group3Leader
	extends PlayerImpl
{
	/* The randomizer used to generate random price */
	private final Random m_randomizer = new Random(System.currentTimeMillis());

	private ArrayList<Record> records = new ArrayList<Record>();
	private static final int WINDOW_SIZE = 20;


	private Group3Leader()
		throws RemoteException, NotBoundException
	{
		super(PlayerType.LEADER, "Group 3 Leader");

		System.out.println("Group 3 Leader, Online");
	}

	@Override
	public void goodbye()
		throws RemoteException
	{
		ExitTask.exit(500);
	}

	@Override
	public void startSimulation(int p_steps)
		throws RemoteException
	{
		for (int i = 0; i < WINDOW_SIZE; i++) {
			records.add(m_platformStub.query(m_type, i + 1)); //1 indexed..
			// m_platformStub.log(m_type, "Output: " + String.valueOf(m_platformStub.query(m_type, i).m_followerPrice));
		}
		ReactionFunction reactionFunction = new ReactionFunction(records);
		m_platformStub.log(m_type, String.valueOf(records.size() + " Records slurped"));

		m_platformStub.log(m_type, String.valueOf(reactionFunction.reactionDifferenceFromActual + " difference"));
	}

	/**
	 * To inform this instance to proceed to a new simulation day
	 * @param p_date The date of the new day
	 * @throws RemoteException
	 */
	@Override
	public void proceedNewDay(int p_date)
		throws RemoteException
	{
		m_platformStub.publishPrice(m_type, genPrice(1.8f, 0.05f));
	}

	/**
	 * Generate a random price based Gaussian distribution. The mean is p_mean,
	 * and the diversity is p_diversity
	 * @param p_mean The mean of the Gaussian distribution
	 * @param p_diversity The diversity of the Gaussian distribution
	 * @return The generated price
	 */
	private float genPrice(final float p_mean, final float p_diversity)
	{
		return (float) (p_mean + m_randomizer.nextGaussian() * p_diversity);
	}

	public static void main(final String[] p_args)
		throws RemoteException, NotBoundException
	{
		new Group3Leader();
	}

	/**
	 * The task used to automatically exit the leader process
	 * @author Xin
	 */
	private static class ExitTask
		extends TimerTask
	{
		static void exit(final long p_delay)
		{
			(new Timer()).schedule(new ExitTask(), p_delay);
		}
		
		@Override
		public void run()
		{
			System.exit(0);
		}
	}

	class ReactionFunction {
		public float aPrime;
		public float bPrime;
		public List<Record> records;
		public float reactionDifferenceFromActual;

		public ReactionFunction() {
		}

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
