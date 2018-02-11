package org.telegram.Adel;

import java.util.ArrayList;

public class HiddenController
{
	private static ArrayList<Long> hiddenList;

	public static void initHiddenList()
	{
		hiddenList = new ArrayList<>();

		String[] hiddenIdString = Setting.getHiddenList().split("-");

		for (String Id : hiddenIdString)
		{
			try
			{
				hiddenList.add(Long.parseLong(Id));
			} catch (Exception ignored)
			{
			}
		}
	}

	public static void addToHidden(Long id)
	{
		String m = Setting.getHiddenList();
		m = m + "-" + String.valueOf(id);
		Setting.setHiddenList(m);

		hiddenList.add(id);
	}

	public static void removeFromHidden(Long id)
	{
		String m = Setting.getHiddenList();
		m = m.replace(String.valueOf(id), "");
		Setting.setHiddenList(m);

		hiddenList.remove(id);
	}

	public static boolean isHidden(Long id)
	{
		return hiddenList.contains(Math.abs(id));
	}
}
