package com.group2.stackelbergBot;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The RMI interface of the player
 * @author Xin
 */
public interface Player
	extends Remote
{
	void checkConnection()
		throws RemoteException;

	void goodbye()
		throws RemoteException;

	void startSimulation(final int p_steps)
		throws RemoteException;

	void endSimulation()
		throws RemoteException;

	void proceedNewDay(final int p_date)
		throws RemoteException;
}
