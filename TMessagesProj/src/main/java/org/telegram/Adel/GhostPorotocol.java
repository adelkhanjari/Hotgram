package org.telegram.Adel;

import org.telegram.messenger.MessagesController;

public class GhostPorotocol
{
	public static void toggleGhostPortocol()
	{
		boolean m = !Setting.getGhostMode();
		turn(m);
	}

	public static void update()
	{
		boolean m = Setting.getGhostMode();
		turn(m);
	}

	public static void turn(boolean on)
	{
		Setting.setGhostMode(on);
		Setting.setsendDeliver(on);
		Setting.setSendTyping(on);

		MessagesController.getInstance().reRunUpdateTimerProc();
	}

}
