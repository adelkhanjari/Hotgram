package org.telegram.Adel;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import org.telegram.Adel.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;

public class ZangoolehListAdapter extends BaseAdapter
{
	Context              mContext;
	ArrayList<Zangooleh> zangoolehList;
	Typeface             typeface;

	public ZangoolehListAdapter(Context context, Typeface typeface, ArrayList<Zangooleh> zangoolehList)
	{
		this.mContext = context;
		this.zangoolehList = zangoolehList;
		this.typeface = typeface;
	}

	@Override
	public int getCount()
	{
		return zangoolehList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return zangoolehList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View       view = convertView;
		ViewHolder holder;

		if (convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.zangooleh_row, parent, false);

			holder = new ViewHolder(view, typeface);

			view.setTag(holder);
		}
		else
		{
			holder = (ViewHolder) view.getTag();
		}

		// ImageView
		if (zangoolehList.get(position).ImageUrl.length() > 5)
		{
			holder.imgMain.setVisibility(View.VISIBLE);
			Picasso.with(mContext)
			       .load(zangoolehList.get(position).ImageUrl)
			       .into(holder.imgMain);
		}
		else
		{
			holder.imgMain.setVisibility(View.GONE);
		}

		// Button
		if (zangoolehList.get(position).Link.length() > 5)
		{
			holder.btn1.setText(zangoolehList.get(position).ButtonText);
			holder.btn1.setVisibility(View.VISIBLE);
			holder.btn1.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					Browser.openUrl(mContext, zangoolehList.get(position).Link);
				}
			});
			holder.imgMain.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					Browser.openUrl(mContext, zangoolehList.get(position).Link);
				}
			});
		}
		else
		{
			holder.btn1.setVisibility(View.GONE);
		}

		holder.txtTitle.setText(zangoolehList.get(position).Title);
		holder.txtContent.setText(zangoolehList.get(position).Content);
		holder.txtTime.setText(zangoolehList.get(position).Time);

		return view;
	}

	public static class ViewHolder
	{
		public ImageView imgMain;
		public TextView  txtTitle, txtContent, txtTime;
		public Button btn1;

		public ViewHolder(View view, Typeface typeface)
		{
			imgMain = (ImageView) view.findViewById(R.id.imgMain);
			(txtTitle = (TextView) view.findViewById(R.id.txtTitle)).setTypeface(typeface);
			(txtContent = (TextView) view.findViewById(R.id.txtContent)).setTypeface(typeface);
			(txtTime = (TextView) view.findViewById(R.id.txtTime)).setTypeface(typeface);
			(btn1 = (Button) view.findViewById(R.id.btn1)).setTypeface(typeface);
		}
	}
}
