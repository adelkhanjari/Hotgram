package org.telegram.Adel;

import android.content.Context;
import android.util.AttributeSet;

import org.telegram.messenger.AndroidUtilities;

public class RadioButton extends android.support.v7.widget.AppCompatRadioButton
{
	public RadioButton(Context context)
	{
		super(context);
		setTypeface(AndroidUtilities.getTypeface(null));
	}

	public RadioButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setTypeface(AndroidUtilities.getTypeface(null));
	}

	public RadioButton(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		setTypeface(AndroidUtilities.getTypeface(null));
	}
}
