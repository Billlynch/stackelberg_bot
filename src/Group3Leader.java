import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

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
		m_platformStub.log(m_type, String.valueOf(records.size() + " Records slurped"));
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
}
