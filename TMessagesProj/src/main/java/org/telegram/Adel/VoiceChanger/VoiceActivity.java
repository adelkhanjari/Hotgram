package org.telegram.Adel.VoiceChanger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import org.telegram.Adel.Setting;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public class VoiceActivity extends BaseFragment {

    private int selectedRate   = 0;

    @Override
    public View createView(final Context context)  {
        actionBar.setBackgroundColor(Theme.ACTION_BAR_MEDIA_PICKER_COLOR);
//        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("voicechanger", R.string.voicechanger));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });


        fragmentView = new FrameLayout(context);
        fragmentView.setLayoutParams(new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.voice_activity, null);
        RadioGroup radioGroup = (RadioGroup) fragmentView.findViewById(R.id.radio_group);
        Button save = (Button) fragmentView.findViewById(R.id.save_bottom);
//        save.setBackgroundColor(AndroidUtilities.themeColor);
	    save.setTypeface(AndroidUtilities.getTypeface(null)); // Adel
        LinearLayout ml = (LinearLayout) fragmentView.findViewById(R.id.confvoice);
        final TextCheckCell cell = new TextCheckCell(context);
        cell.setTextAndCheck(LocaleController.getString("chatvoiceRow", R.string.chatvoiceRow), Setting.getVerifyBeforeSendVoiceAndVideo() == 2, false);
        cell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean verify = Setting.getVerifyBeforeSendVoiceAndVideo() == 2;
	            Setting.setVerifyBeforeSendVoiceAndVideo(verify ? 1 : 2);
	            cell.setChecked(!verify);
            }
        });
	    ml.addView(cell);

	    int rate = Setting.getVoiceRate();
	    int index = R.id.radio_01;
	    switch (rate) {
		    case 16000:
		    	index = R.id.radio_01;
			    break;
		    case 25000:
		    	index = R.id.radio_02;
			    break;
		    case 20000:
			    index = R.id.radio_03;
			    break;
		    case 14000:
			    index = R.id.radio_04;
			    break;
		    case 12000:
			    index = R.id.radio_05;
			    break;
		    case 9000:
			    index = R.id.radio_06;
			    break;
	    }
	    radioGroup.check(index);

	    radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.radio_01:
                        selectedRate = 16000 ;
                        break;
                    case R.id.radio_02:
                        selectedRate = 25000 ;
                        break;
                    case R.id.radio_03:
                        selectedRate = 20000 ;
                        break;
                    case R.id.radio_04:
                        selectedRate = 14000 ;
                        break;
                    case R.id.radio_05:
                        selectedRate = 12000 ;
                        break;
                    case R.id.radio_06:
                        selectedRate = 9000 ;
                        break;
                }
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
	            if (selectedRate != 0) {
		            Setting.setVoiceRate(selectedRate);
	            }
                finishFragment();
            }
        });

        return fragmentView;
    }
}
