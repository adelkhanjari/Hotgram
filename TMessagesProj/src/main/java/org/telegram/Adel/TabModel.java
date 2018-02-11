package org.telegram.Adel;

import java.util.HashMap;

import org.telegram.messenger.R;

public class TabModel
{
	private String id;
	private int    title;
	private int    selectedIcon;
	private int    unselectedIcon;

	public TabModel(String id, int title)
	{
		this.id = id;
		this.title = title;
		this.selectedIcon = getSelectedIcons().get(id);
		this.unselectedIcon = getUnselectedIcons().get(id);
	}

	private HashMap<String, Integer> getUnselectedIcons()
	{
		HashMap<String, Integer> icons = new HashMap<>();
		icons.put("favor", R.drawable.ic_star_gray_24dp);
		icons.put("bot", R.mipmap.ic_tab_bot_gray);
		icons.put("unread", R.mipmap.ic_tab_timeline_grey);
		icons.put("channel", R.mipmap.ic_tab_channel_gray);
		icons.put("sgroup", R.mipmap.ic_tab_supergroup_gray);
		icons.put("ngroup", R.mipmap.ic_tab_group_gray);
		icons.put("contact", R.mipmap.ic_tab_contact_gray);
		icons.put("all", R.drawable.ic_home_gray_24dp);
		return icons;
	}

	private static HashMap<String, Integer> getSelectedIcons()
	{
		HashMap<String, Integer> icons = new HashMap<>();
		icons.put("favor", R.drawable.ic_star_white_24dp);
		icons.put("bot", R.mipmap.ic_tab_bot_blue);
		icons.put("unread", R.mipmap.ic_tab_timeline);
		icons.put("channel", R.mipmap.ic_tab_channel_blue);
		icons.put("sgroup", R.mipmap.ic_tab_supergroup_blue);
		icons.put("ngroup", R.mipmap.ic_tab_group_blue);
		icons.put("contact", R.mipmap.ic_tab_contact_blue);
		icons.put("all", R.drawable.ic_home_white_24dp);
		return icons;
	}

	public String getId()
	{
		return id;
	}

	public int getTitle()
	{
		return title;
	}

	public int getSelectedIcon()
	{
		return selectedIcon;
	}

	public int getUnselectedIcon()
	{
		return unselectedIcon;
	}
}
