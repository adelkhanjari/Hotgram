/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.telegram.Adel.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.telegram.Adel.TextView;

import com.readystatesoftware.viewbadger.BadgeView;

import org.telegram.Adel.FavoriteController;
import org.telegram.Adel.GhostPorotocol;
import org.telegram.Adel.HiddenController;
import org.telegram.Adel.OnSwipeTouchListener;
import org.telegram.Adel.Setting;
import org.telegram.Adel.TabSetting;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.query.SearchQuery;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.Adapters.DialogsSearchAdapter;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HashtagSearchCell;
import org.telegram.ui.Cells.HintDialogCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class DialogsActivity extends BaseFragment
        implements NotificationCenter.NotificationCenterDelegate {

    private TabLayout                                tabHost; // Adel
	private ArrayList<BadgeView>                     badges; // Adel
	private RecyclerListView.OnItemClickListener     onItemListenerForDialogs; // Adel
    private RecyclerListView.OnItemLongClickListener onItemListenerLongForDialogs; // Adel
    private OnSwipeTouchListener                     onSwipeTouchListener; // Adel
    private boolean                                  hiddenMode; // Adel
    private boolean                                  enterHiddenPassMode = false; // Adel
    private ImageView                                floatingButtonLock; // Adel
    private ActionBarMenuItem                        searchFiledItem; // Adel

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private DialogsAdapter dialogsAdapter;
    private DialogsSearchAdapter dialogsSearchAdapter;
    private EmptyTextProgressView searchEmptyView;
    private RadialProgressView progressView;
    private LinearLayout emptyView;
    private ActionBarMenuItem passcodeItem;
    private ImageView floatingButton;
    private RecyclerView sideMenu;
    private FragmentContextView fragmentContextView;

    private TextView emptyTextView1;
    private TextView emptyTextView2;

    private AlertDialog permissionDialog;

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;
    private int dialogsType;

    public static boolean dialogsLoaded;
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private long selectedDialog;
    private String searchString;
    private long openedDialogId;
    private boolean cantSendToChannels;

    private DialogsActivityDelegate delegate;

    public interface DialogsActivityDelegate {
        void didSelectDialog(DialogsActivity fragment, long dialog_id, boolean param);
    }

    public DialogsActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            cantSendToChannels = arguments.getBoolean("cantSendToChannels", false);
            dialogsType = arguments.getInt("dialogsType", 0);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
            hiddenMode = arguments.getBoolean("hiddenMode", false);
        }

        if (searchString == null) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByAck);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByServer);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didLoadedReplyMessages);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.reloadHints);
        }


        if (!dialogsLoaded) {
            MessagesController.getInstance().loadDialogs(0, 100, true);
            ContactsController.getInstance().checkInviteText();
            MessagesController.getInstance().loadPinnedDialogs(0, null);
            StickersQuery.checkFeaturedStickers();
            dialogsLoaded = true;
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (searchString == null) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByAck);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByServer);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didLoadedReplyMessages);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.reloadHints);
        }
        delegate = null;
    }

    @Override
    public View createView(final Context context) {
        // Adel Add BOT Start
        if (!MessagesController.getInstance().loadingDialogs)
        {
            SharedPreferences sharedPreferences = context.getSharedPreferences("myAdd", Activity.MODE_PRIVATE);
            if (UserConfig.isClientActivated() && sharedPreferences.getBoolean("startBootfromAdv", false))
            {
                sharedPreferences.edit().putBoolean("startBootfromAdv", false).commit();

                String  botUsername = sharedPreferences.getString("startedbootusername", "");
                boolean isBotMute   = sharedPreferences.getBoolean("isBotMuted", true);

                Bot(botUsername, isBotMute);
            }
        }
        // Adel Add BOT End
        searching = false;
        searchWas = false;

        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Theme.createChatResources(context, false);
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        if (!onlySelect && searchString == null) {
            passcodeItem = menu.addItem(1, R.drawable.lock_close);
            updatePasscodeButton();
        }

        // Adel ------------- Search Start ----------------
        searchFiledItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() // Adel
        {
            @Override
            public void onSearchExpand()
            {
                searching = true;
                if (listView != null)
                {
                    if (searchString != null)
                    {
                        listView.setEmptyView(searchEmptyView);
                        progressView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                    }
                    if (!onlySelect)
                    {
                        //						floatingButton.setVisibility(View.GONE); // Adel Commented
                    }
                }
                updatePasscodeButton();
            }

            @Override
            public boolean canCollapseSearch()
            {
                if (searchString != null)
                {
                    finishFragment();
                    return false;
                }
                return true;
            }

            @Override
            public void onSearchCollapse()
            {
                // Adel Start
                if (Setting.HideHavePass() && searchFiledItem != null && enterHiddenPassMode)
                {
                    searchFiledItem.getSearchField().setInputType(524289);
                    searchFiledItem.getSearchField().setTransformationMethod(null);
                    searchFiledItem.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));
                }
                enterHiddenPassMode = false;
                // Adel End

                searching = false;
                searchWas = false;
                if (listView != null)
                {
                    searchEmptyView.setVisibility(View.GONE);
                    if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) // Adel
                    {
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    }
                    else
                    {
                        progressView.setVisibility(View.GONE);
                        listView.setEmptyView(emptyView);
                    }
                    if (!onlySelect)
                    {
                        floatingButton.setVisibility(View.VISIBLE);
                        floatingHidden = true;
                        floatingButton.setTranslationY(AndroidUtilities.dp(100));
                        hideFloatingButton(false);
                    }
                    if (listView.getAdapter() != dialogsAdapter)
                    {
                        listView.setAdapter(dialogsAdapter);
                        dialogsAdapter.notifyDataSetChanged();
                    }
                }
                if (dialogsSearchAdapter != null)
                {
                    dialogsSearchAdapter.searchDialogs(null);
                }
                updatePasscodeButton();
            }

            @Override
            public void onTextChanged(EditText editText)
            {
                String text = editText.getText().toString();
                if (!enterHiddenPassMode) // Adel
                {
                    if (text.length() != 0 || dialogsSearchAdapter != null && dialogsSearchAdapter.hasRecentRearch())
                    {
                        searchWas = true;
                        if (dialogsSearchAdapter != null && listView.getAdapter() != dialogsSearchAdapter)
                        {
                            listView.setAdapter(dialogsSearchAdapter);
                            dialogsSearchAdapter.notifyDataSetChanged();
                        }
                        if (searchEmptyView != null && listView.getEmptyView() != searchEmptyView)
                        {
                            emptyView.setVisibility(View.GONE);
                            progressView.setVisibility(View.GONE);
                            searchEmptyView.showTextView();
                            listView.setEmptyView(searchEmptyView);
                        }
                    }
                    if (dialogsSearchAdapter != null)
                    {
                        dialogsSearchAdapter.searchDialogs(text);
                    }
                }
                else if (Setting.CheckHidePassword(text)) // Adel
                {
                    editText.setText(null);
                    if (actionBar != null && actionBar.isSearchFieldVisible())
                    {
                        actionBar.closeSearchField();
                    }

                    Snackbar.make(listView, LocaleController.getString("HiddenChats", R.string.HiddenChats), Snackbar.LENGTH_SHORT).show();

                    floatingButtonLock.setVisibility(View.VISIBLE);
                    hiddenMode = true;
                    dialogsAdapter.hiddenMode = true;
                    dialogsAdapter.notifyDataSetChanged();
                }
            }
        });
        searchFiledItem.getSearchField().setHint(LocaleController.getString("Search", R.string.Search)); // Adel
        // Adel ------------- Search End ----------------
        // Adel ------------- Ghost Start ----------------
        GhostPorotocol.update();
        final ActionBarMenuItem ghostmenu = menu.addItem(4, (Setting.getGhostMode() ? R.drawable.ic_ghost_selected : R.drawable.ic_ghost));
        ghostmenu.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (Setting.GetGhostFirstTime())
                {
                    new AlertDialog.Builder(context)
                            .setTitle(LocaleController.getString("GhostFirstTimeTitle", R.string.GhostFirstTimeTitle))
                            .setMessage(LocaleController.getString("GhostFirstTimeMessage", R.string.GhostFirstTimeMessage))
                            .setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialogInterface, int i)
                                {
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();

                    Setting.SetGhostFirstTime();
                }

                if (Setting.getGhostMode())
                {
                    ghostmenu.setIcon(R.drawable.ic_ghost);
                    Snackbar snack = Snackbar.make(listView, LocaleController.getString("GhostModeIsNotActive", R.string.GhostModeIsNotActive), Snackbar.LENGTH_SHORT);
                    View     viewz = snack.getView();
                    android.widget.TextView tv    = (android.widget.TextView) viewz.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
	                tv.setTypeface(AndroidUtilities.getTypeface(null));
                    snack.show();
                    GhostPorotocol.turn(false);
                }
                else
                {
                    ghostmenu.setIcon(R.drawable.ic_ghost_selected);
                    Snackbar snack = Snackbar.make(listView, LocaleController.getString("GhostModeIsActive", R.string.GhostModeIsActive), Snackbar.LENGTH_LONG);
                    View     viewz = snack.getView();
	                android.widget.TextView tv    = (android.widget.TextView) viewz.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
	                tv.setTypeface(AndroidUtilities.getTypeface(null));
                    snack.show();
                    GhostPorotocol.turn(true);
                }
            }
        });
        // Adel ------------- Ghost End ----------------

        hiddenMode = Setting.GetHiddenMode(); // Adel

        if (onlySelect) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
        } else {
            if (searchString != null) {
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            } else {
                actionBar.setBackButtonDrawable(new MenuDrawable());
            }
            if (BuildVars.DEBUG_VERSION) {
                actionBar.setTitle(LocaleController.getString("AppNameBeta", R.string.AppNameBeta));
            } else {
                actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
            }
        }
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (onlySelect) {
                        finishFragment();
                    } else if (parentLayout != null) {
                        parentLayout.getDrawerLayoutContainer().openDrawer(false);
                    }
                } else if (id == 1) {
                    UserConfig.appLocked = !UserConfig.appLocked;
                    UserConfig.saveConfig(false);
                    updatePasscodeButton();
                }
            }
        });

        if (sideMenu != null) {
            sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.getAdapter().notifyDataSetChanged();
        }

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        
        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(true);
        listView.setItemAnimator(null);
        listView.setInstantClick(true);
        listView.setLayoutAnimation(null);
        listView.setTag(4);
        layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);

        // Adel ------------------ Tabs Start ---------------------
        Boolean isTabsUpside = Setting.getTabIsUp();
        if (isTabsUpside)
        {
            frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT, 0, 34, 0, 0)); // Adel changed 48 to 34
        }
        else
        {
            frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT, 0, 0, 0, 34)); // Adel changed 48 to 34
        }

        // Tabs
        tabHost = new TabLayout(context);
        tabHost.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        tabHost.setSelectedTabIndicatorColor(0xffffffff);
        tabHost.setTabMode(TabLayout.MODE_FIXED);
        tabHost.setTabGravity(TabLayout.GRAVITY_FILL);
        tabHost.setVisibility(Setting.getProTelegram() ? View.VISIBLE : View.GONE);
        tabHost.setSelectedTabIndicatorHeight(AndroidUtilities.dp(3));
        tabHost.setTabTextColors(Color.argb(100, 255, 255, 255), Color.WHITE);
        tabHost.setClipToPadding(true);
        tabHost.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                int position = tab.getPosition();
	            TabSetting.setTabIcon(tab, TabSetting.getSelectedIcon(position));
	            floatingButton.setVisibility(View.VISIBLE);
                listView.setOnItemLongClickListener(onItemListenerLongForDialogs);
                listView.setOnItemClickListener(onItemListenerForDialogs);
                dialogsAdapter.categoryId = TabSetting.getTabModels().get(position).getId();
                dialogsAdapter.hiddenMode = hiddenMode;
                dialogsAdapter.notifyDataSetChanged();
                actionBar.setTitle(LocaleController.getString("TabTitle", TabSetting.getTabModels().get(position).getTitle()));

                Setting.SetLastTabIndex(position); // Adel
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
                TabSetting.setTabIcon(tab, TabSetting.getUnselectedIcon(tab.getPosition()));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });
	    badges = new ArrayList<>();

        // ---
        if (isTabsUpside)
        {
            frameLayout.addView(tabHost, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 34, Gravity.TOP)); // Adel Changed Wrap_Content to 34
        }
        else
        {
            frameLayout.addView(tabHost, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 34, Gravity.BOTTOM)); // Adel Changed Wrap_Content to 34
        }
        // Adel ------------------ Tabs End -----------------------

        onItemListenerForDialogs = new RecyclerListView.OnItemClickListener()
        {
            @Override
            public void onItemClick(View view, int position)
            {
                if (listView == null || listView.getAdapter() == null)
                {
                    return;
                }
                long                 dialog_id  = 0;
                int                  message_id = 0;
                RecyclerView.Adapter adapter    = listView.getAdapter();
                if (adapter == dialogsAdapter)
                {
                    TLRPC.TL_dialog dialog = dialogsAdapter.getItem(position);
                    if (dialog == null)
                    {
                        return;
                    }

                    MessagesController.getInstance().dialogsUnreadOnly.remove(dialog); // Adel
                    dialog_id = dialog.id;
                }
                else if (adapter == dialogsSearchAdapter)
                {
                    Object obj = dialogsSearchAdapter.getItem(position);
                    if (obj instanceof TLRPC.User)
                    {
                        dialog_id = ((TLRPC.User) obj).id;
                        if (dialogsSearchAdapter.isGlobalSearch(position))
                        {
                            ArrayList<TLRPC.User> users = new ArrayList<>();
                            users.add((TLRPC.User) obj);
                            MessagesController.getInstance().putUsers(users, false);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                        }
                        if (!onlySelect)
                        {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.User) obj);
                        }
                    }
                    else if (obj instanceof TLRPC.Chat)
                    {
                        if (dialogsSearchAdapter.isGlobalSearch(position))
                        {
                            ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                            chats.add((TLRPC.Chat) obj);
                            MessagesController.getInstance().putChats(chats, false);
                            MessagesStorage.getInstance().putUsersAndChats(null, chats, false, true);
                        }
                        if (((TLRPC.Chat) obj).id > 0)
                        {
                            dialog_id = -((TLRPC.Chat) obj).id;
                        }
                        else
                        {
                            dialog_id = AndroidUtilities.makeBroadcastId(((TLRPC.Chat) obj).id);
                        }
                        if (!onlySelect)
                        {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.Chat) obj);
                        }
                    }
                    else if (obj instanceof TLRPC.EncryptedChat)
                    {
                        dialog_id = ((long) ((TLRPC.EncryptedChat) obj).id) << 32;
                        if (!onlySelect)
                        {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.EncryptedChat) obj);
                        }
                    }
                    else if (obj instanceof MessageObject)
                    {
                        MessageObject messageObject = (MessageObject) obj;
                        dialog_id = messageObject.getDialogId();
                        message_id = messageObject.getId();
                        dialogsSearchAdapter.addHashtagsFromMessage(dialogsSearchAdapter.getLastSearchString());
                    }
                    else if (obj instanceof String)
                    {
                        actionBar.openSearchField((String) obj);
                    }
                }

                if (dialog_id == 0)
                {
                    return;
                }

                if (onlySelect)
                {
                    didSelectResult(dialog_id, true, false);
                }
                else
                {
                    Bundle args       = new Bundle();
                    int    lower_part = (int) dialog_id;
                    int    high_id    = (int) (dialog_id >> 32);
                    if (lower_part != 0)
                    {
                        if (high_id == 1)
                        {
                            args.putInt("chat_id", lower_part);
                        }
                        else
                        {
                            if (lower_part > 0)
                            {
                                args.putInt("user_id", lower_part);
                            }
                            else if (lower_part < 0)
                            {
                                if (message_id != 0)
                                {
                                    TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                                    if (chat != null && chat.migrated_to != null)
                                    {
                                        args.putInt("migrated_to", lower_part);
                                        lower_part = -chat.migrated_to.channel_id;
                                    }
                                }
                                args.putInt("chat_id", -lower_part);
                            }
                        }
                    }
                    else
                    {
                        args.putInt("enc_id", high_id);
                    }
                    if (message_id != 0)
                    {
                        args.putInt("message_id", message_id);
                    }
                    else
                    {
                        if (actionBar != null)
                        {
                            actionBar.closeSearchField();
                        }
                    }
                    if (AndroidUtilities.isTablet())
                    {
                        if (openedDialogId == dialog_id && adapter != dialogsSearchAdapter)
                        {
                            return;
                        }
                        if (dialogsAdapter != null)
                        {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = dialog_id);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null)
                    {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this))
                        {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                        }
                    }
                    else
                    {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this))
                        {
                            presentFragment(new ChatActivity(args));
                        }
                    }
                }
            }
        };

        onItemListenerLongForDialogs = new RecyclerListView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemClick(View view, int position)
            {
                if (onlySelect || searching && searchWas || getParentActivity() == null)
                {
                    if (searchWas && searching || dialogsSearchAdapter.isRecentSearchDisplayed())
                    {
                        RecyclerView.Adapter adapter = listView.getAdapter();
                        if (adapter == dialogsSearchAdapter)
                        {
                            Object item = dialogsSearchAdapter.getItem(position);
                            if (item instanceof String || dialogsSearchAdapter.isRecentSearchDisplayed())
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                                builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i)
                                    {
                                        if (dialogsSearchAdapter.isRecentSearchDisplayed())
                                        {
                                            dialogsSearchAdapter.clearRecentSearch();
                                        }
                                        else
                                        {
                                            dialogsSearchAdapter.clearRecentHashtags();
                                        }
                                    }
                                });
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                showDialog(builder.create());
                                return true;
                            }
                        }
                    }
                    return false;
                }
                TLRPC.TL_dialog            dialog;
                ArrayList<TLRPC.TL_dialog> dialogs = getDialogsArray();
                if (position < 0 || position >= dialogs.size())
                {
                    return false;
                }
                dialog = dialogs.get(position);
                selectedDialog = dialog.id;
                final boolean pinned = dialog.pinned;

                BottomSheet.Builder builder  = new BottomSheet.Builder(getParentActivity());
                int                 lower_id = (int) selectedDialog;
                int                 high_id  = (int) (selectedDialog >> 32);

                if (DialogObject.isChannel(dialog))
                {
                    final TLRPC.Chat chat     = MessagesController.getInstance().getChat(-lower_id);
                    CharSequence     items[];
                    final boolean    isFavor  = FavoriteController.isFavor(selectedDialog); // Adel
                    final boolean    isHidden = HiddenController.IsHidden(selectedDialog);  // Adel
                    int icons[] = new int[]{
                            R.drawable.ic_star_gray_24dp, // Adel
                            R.drawable.menu_settings, // Adel
                            R.drawable.ic_remove_red_eye_white_24dp, // Adel
                            MessagesController.getInstance().isDialogMuted(selectedDialog) ? R.drawable.volume_on : R.drawable.ic_volume_off_white_24dp, // Adel
                            dialog.pinned ? R.drawable.chats_unpin : R.drawable.chats_pin,
                            R.drawable.chats_clear,
                            R.drawable.chats_leave
                    };
                    if (chat != null && chat.megagroup)
                    {
                        items = new CharSequence[]{
                                isFavor ? LocaleController.getString("RemoveFromFavorites", R.string.RemoveFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites), // Adel
                                isHidden ? LocaleController.getString("RemoveFromHiddens", R.string.RemoveFromHiddens) : LocaleController.getString("AddToHiddens", R.string.AddToHiddens), // Adel
                                LocaleController.getString("MarkAsRead", R.string.MarkAsRead), // Adel
                                MessagesController.getInstance().isDialogMuted(selectedDialog) ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications), // Adel
                                dialog.pinned || MessagesController.getInstance().canPinDialog(false) ? (dialog.pinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop)) : null,
                                LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache),
                                chat == null || !chat.creator ? LocaleController.getString("LeaveMegaMenu", R.string.LeaveMegaMenu) : LocaleController.getString("DeleteMegaMenu", R.string.DeleteMegaMenu)};
                    }
                    else
                    {
                        items = new CharSequence[]{
                                isFavor ? LocaleController.getString("RemoveFromFavorites", R.string.RemoveFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites), // Adel
                                isHidden ? LocaleController.getString("RemoveFromHiddens", R.string.RemoveFromHiddens) : LocaleController.getString("AddToHiddens", R.string.AddToHiddens), // Adel
                                LocaleController.getString("MarkAsRead", R.string.MarkAsRead), // Adel
                                MessagesController.getInstance().isDialogMuted(selectedDialog) ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications), // Adel
                                dialog.pinned || MessagesController.getInstance().canPinDialog(false) ? (dialog.pinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop)) : null,
                                LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache),
                                chat == null || !chat.creator ? LocaleController.getString("LeaveChannelMenu", R.string.LeaveChannelMenu) : LocaleController.getString("ChannelDeleteMenu", R.string.ChannelDeleteMenu)};
                    }
                    builder.setItems(items, icons, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, final int which)
                        {
                            if (which == 0) // Adel
                            {
                                if (!FavoriteController.isFavor(selectedDialog))
                                {
                                    FavoriteController.addToFavor(selectedDialog);
                                }
                                else
                                {
                                    FavoriteController.RemoveFromFavor(selectedDialog);
                                }
                                MessagesController.getInstance().sortDialogs(null);
                            }
                            else if (which == 1) // Adel
                            {
                                ToggleHidden(selectedDialog);
                            }
                            else if (which == 2) // Adel
                            {
                                markAsRead(selectedDialog);
                            }
                            else if (which == 3) // Adel
                            {
                                if (MessagesController.getInstance().isDialogMuted(selectedDialog))
                                {
                                    SharedPreferences        preferences = context.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                    SharedPreferences.Editor editor      = preferences.edit();
                                    editor.putInt("notify2_" + selectedDialog, 0);
                                    MessagesStorage.getInstance().setDialogFlags(selectedDialog, 0);
                                    editor.commit();
                                    TLRPC.TL_dialog dialog2 = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                    if (dialog2 != null)
                                    {
                                        dialog2.notify_settings = new TLRPC.TL_peerNotifySettings();
                                    }
                                    NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                }
                                else
                                {
                                    showDialog(AlertsCreator.createMuteAlert(getParentActivity(), selectedDialog));
                                }
                            }
                            else if (which == 4)
                            {
                                if (MessagesController.getInstance().pinDialog(selectedDialog, !pinned, null, 0) && !pinned)
                                {
                                    listView.smoothScrollToPosition(0);
                                }
                            }
                            else
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                if (which == 5)
                                {
                                    if (chat != null && chat.megagroup)
                                    {
                                        builder.setMessage(LocaleController.getString("AreYouSureClearHistorySuper", R.string.AreYouSureClearHistorySuper));
                                    }
                                    else
                                    {
                                        builder.setMessage(LocaleController.getString("AreYouSureClearHistoryChannel", R.string.AreYouSureClearHistoryChannel));
                                    }
                                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            MessagesController.getInstance().deleteDialog(selectedDialog, 2);
                                        }
                                    });
                                }
                                else
                                {
                                    if (chat != null && chat.megagroup)
                                    {
                                        if (!chat.creator)
                                        {
                                            builder.setMessage(LocaleController.getString("MegaLeaveAlert", R.string.MegaLeaveAlert));
                                        }
                                        else
                                        {
                                            builder.setMessage(LocaleController.getString("MegaDeleteAlert", R.string.MegaDeleteAlert));
                                        }
                                    }
                                    else
                                    {
                                        if (chat == null || !chat.creator)
                                        {
                                            builder.setMessage(LocaleController.getString("ChannelLeaveAlert", R.string.ChannelLeaveAlert));
                                        }
                                        else
                                        {
                                            builder.setMessage(LocaleController.getString("ChannelDeleteAlert", R.string.ChannelDeleteAlert));
                                        }
                                    }
                                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, UserConfig.getCurrentUser(), null);
                                            if (AndroidUtilities.isTablet())
                                            {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                            }
                                        }
                                    });
                                }
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                showDialog(builder.create());
                            }
                            listView.getAdapter().notifyDataSetChanged(); // Adel
                        }
                    });
                    showDialog(builder.create());
                }
                else
                {
                    final boolean isChat = lower_id < 0 && high_id != 1;
                    TLRPC.User    user   = null;
                    if (!isChat && lower_id > 0 && high_id != 1)
                    {
                        user = MessagesController.getInstance().getUser(lower_id);
                    }
                    final boolean isBot    = user != null && user.bot;
                    final boolean isFavor  = FavoriteController.isFavor(selectedDialog); // Adel
                    final boolean isHidden = HiddenController.IsHidden(selectedDialog);  // Adel

                    builder.setItems(new CharSequence[]{
                            isFavor ? LocaleController.getString("RemoveFromFavorites", R.string.RemoveFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites), // Adel
                            isHidden ? LocaleController.getString("RemoveFromHiddens", R.string.RemoveFromHiddens) : LocaleController.getString("AddToHiddens", R.string.AddToHiddens), // Adel
                            LocaleController.getString("MarkAsRead", R.string.MarkAsRead), // Adel
                            MessagesController.getInstance().isDialogMuted(selectedDialog) ? LocaleController.getString("UnmuteNotifications", R.string.UnmuteNotifications) : LocaleController.getString("MuteNotifications", R.string.MuteNotifications), // Adel
                            dialog.pinned || MessagesController.getInstance().canPinDialog(lower_id == 0) ? (dialog.pinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop)) : null,
                            LocaleController.getString("ClearHistory", R.string.ClearHistory),
                            isChat ? LocaleController.getString("DeleteChat", R.string.DeleteChat) : isBot ? LocaleController.getString("DeleteAndStop", R.string.DeleteAndStop) : LocaleController.getString("Delete", R.string.Delete)
                    }, new int[]{
                            R.drawable.ic_star_gray_24dp, // Adel
                            R.drawable.menu_settings, // Adel
                            R.drawable.ic_remove_red_eye_white_24dp, // Adel
                            MessagesController.getInstance().isDialogMuted(selectedDialog) ? R.drawable.volume_on : R.drawable.ic_volume_off_white_24dp, // Adel
                            dialog.pinned ? R.drawable.chats_unpin : R.drawable.chats_pin,
                            R.drawable.chats_clear,
                            isChat ? R.drawable.chats_leave : R.drawable.chats_delete
                    }, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, final int which)
                        {
                            if (which == 0) // Adel
                            {
                                if (!FavoriteController.isFavor(selectedDialog))
                                {
                                    FavoriteController.addToFavor(selectedDialog);
                                }
                                else
                                {
                                    FavoriteController.RemoveFromFavor(selectedDialog);
                                }
                                MessagesController.getInstance().sortDialogs(null);
                            }
                            else if (which == 1) // Adel
                            {
                                ToggleHidden(selectedDialog);
                            }
                            else if (which == 2) // Adel
                            {
                                markAsRead(selectedDialog);
                            }
                            else if (which == 3) // Adel
                            {
                                if (MessagesController.getInstance().isDialogMuted(selectedDialog))
                                {
                                    SharedPreferences        preferences = context.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
                                    SharedPreferences.Editor editor      = preferences.edit();
                                    editor.putInt("notify2_" + selectedDialog, 0);
                                    MessagesStorage.getInstance().setDialogFlags(selectedDialog, 0);
                                    editor.commit();
                                    TLRPC.TL_dialog dialog2 = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                                    if (dialog2 != null)
                                    {
                                        dialog2.notify_settings = new TLRPC.TL_peerNotifySettings();
                                    }
                                    NotificationsController.updateServerNotificationsSettings(selectedDialog);
                                }
                                else
                                {
                                    showDialog(AlertsCreator.createMuteAlert(getParentActivity(), selectedDialog));
                                }
                            }
                            else if (which == 4)
                            {
                                if (MessagesController.getInstance().pinDialog(selectedDialog, !pinned, null, 0) && !pinned)
                                {
                                    listView.smoothScrollToPosition(0);
                                }
                            }
                            else
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                if (which == 5)
                                {
                                    builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));
                                }
                                else
                                {
                                    if (isChat)
                                    {
                                        builder.setMessage(LocaleController.getString("AreYouSureDeleteAndExit", R.string.AreYouSureDeleteAndExit));
                                    }
                                    else
                                    {
                                        builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                                    }
                                }
                                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i)
                                    {
                                        if (which != 5)
                                        {
                                            if (isChat)
                                            {
                                                TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
                                                if (currentChat != null && ChatObject.isNotInChat(currentChat))
                                                {
                                                    MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                                }
                                                else
                                                {
                                                    MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, MessagesController.getInstance().getUser(UserConfig.getClientUserId()), null);
                                                }
                                            }
                                            else
                                            {
                                                MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                            }
                                            if (isBot)
                                            {
                                                MessagesController.getInstance().blockUser((int) selectedDialog);
                                            }
                                            if (AndroidUtilities.isTablet())
                                            {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                            }
                                        }
                                        else
                                        {
                                            MessagesController.getInstance().deleteDialog(selectedDialog, 1);
                                        }
                                    }
                                });
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                showDialog(builder.create());
                            }
                            listView.getAdapter().notifyDataSetChanged();
                        }
                    });
                    showDialog(builder.create());
                }
                return true;
            }
        };

        onSwipeTouchListener = new OnSwipeTouchListener(context)
        {
            public void onSwipeRight()
            {
                DialogsActivity.this.onSwipeRight();
            }

            public void onSwipeLeft()
            {
                DialogsActivity.this.onSwipeLeft();
            }
        };

        listView.setOnItemClickListener(onItemListenerForDialogs);
        listView.setOnItemLongClickListener(onItemListenerLongForDialogs);
        listView.setOnTouchListener(onSwipeTouchListener);

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        frameLayout.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setOnTouchListener(onSwipeTouchListener); // Adel
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.NO_GRAVITY, 0, (isTabsUpside ? 34 : 0), 0, (isTabsUpside ? 0 : 34))); // Adel

        emptyTextView1 = new TextView(context);
        emptyTextView1.setText(LocaleController.getString("NoChats", R.string.NoChats));
        emptyTextView1.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
        emptyTextView1.setGravity(Gravity.CENTER);
        emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.addView(emptyTextView1, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        emptyTextView2 = new TextView(context);
        String help = LocaleController.getString("NoChatsHelp", R.string.NoChatsHelp);
        if (AndroidUtilities.isTablet() && !AndroidUtilities.isSmallTablet()) {
            help = help.replace('\n', ' ');
        }
        emptyTextView2.setText(help);
        emptyTextView2.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
        emptyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyTextView2.setGravity(Gravity.CENTER);
        emptyTextView2.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), 0);
        emptyTextView2.setLineSpacing(AndroidUtilities.dp(2), 1);
        emptyView.addView(emptyTextView2, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        progressView = new RadialProgressView(context);
        progressView.setVisibility(View.GONE);
        frameLayout.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        floatingButton = new ImageView(context);
        floatingButton.setVisibility(onlySelect ? View.GONE : View.VISIBLE);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);

        // Adel
        floatingButtonLock = new ImageView(context);
        floatingButtonLock.setVisibility(!hiddenMode ? View.GONE : View.VISIBLE);
        floatingButtonLock.setScaleType(ImageView.ScaleType.CENTER);
        floatingButtonLock.setBackgroundResource(R.drawable.floating_pink);
        floatingButtonLock.setImageResource(R.drawable.lock_close);

        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (Build.VERSION.SDK_INT < 21) {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.MULTIPLY));
        floatingButton.setImageResource(R.drawable.floating_pencil);
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }

        frameLayout.addView(floatingButton, LayoutHelper.createFrame(Build.VERSION.SDK_INT >= 21 ? 56 : 60, Build.VERSION.SDK_INT >= 21 ? 56 : 60, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, (isTabsUpside ? 14 : 55))); // Adel
        frameLayout.addView(floatingButtonLock, LayoutHelper.createFrame(Build.VERSION.SDK_INT >= 21 ? 56 : 60, Build.VERSION.SDK_INT >= 21 ? 56 : 60, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, (isTabsUpside ? 74 : 110))); // Adel

        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("destroyAfterSelect", true);
                presentFragment(new ContactsActivity(args));
            }
        });

        // Adel
        floatingButtonLock.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                hiddenMode = false;
                dialogsAdapter.hiddenMode = hiddenMode;
                dialogsAdapter.notifyDataSetChanged();
                floatingButtonLock.setVisibility(View.GONE);
                Snackbar.make(listView, LocaleController.getString("NormalChats", R.string.NormalChats), Snackbar.LENGTH_LONG).show();
            }
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                int totalItemCount = recyclerView.getAdapter().getItemCount();

                if (searching && searchWas) {
                    if (visibleItemCount > 0 && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                        dialogsSearchAdapter.loadMoreSearchMessages();
                    }
                    return;
                }
                if (visibleItemCount > 0) {
                    if (layoutManager.findLastVisibleItemPosition() >= getDialogsArray().size() - 10) {
                        boolean fromCache = !MessagesController.getInstance().dialogsEndReached;
                        if (fromCache || !MessagesController.getInstance().serverDialogsEndReached) {
                            MessagesController.getInstance().loadDialogs(-1, 100, fromCache);
                        }
                    }
                }

                if (floatingButton.getVisibility() != View.GONE) {
                    final View topChild = recyclerView.getChildAt(0);
                    int firstViewTop = 0;
                    if (topChild != null) {
                        firstViewTop = topChild.getTop();
                    }
                    boolean goingDown;
                    boolean changed = true;
                    if (prevPosition == firstVisibleItem) {
                        final int topDelta = prevTop - firstViewTop;
                        goingDown = firstViewTop < prevTop;
                        changed = Math.abs(topDelta) > 1;
                    } else {
                        goingDown = firstVisibleItem > prevPosition;
                    }
                    if (changed && scrollUpdated) {
                        hideFloatingButton(goingDown);
                    }
                    prevPosition = firstVisibleItem;
                    prevTop = firstViewTop;
                    scrollUpdated = true;
                }
            }
        });

        if (searchString == null) {
            dialogsAdapter = new DialogsAdapter(context, dialogsType);
            if (AndroidUtilities.isTablet() && openedDialogId != 0) {
                dialogsAdapter.setOpenedDialogId(openedDialogId);
            }
            dialogsAdapter.categoryId = TabSetting.getTabModels().get(0).getId(); // Adel
            dialogsAdapter.hiddenMode = hiddenMode; // Adel
            listView.setAdapter(dialogsAdapter);
        }
        int type = 0;
        if (searchString != null) {
            type = 2;
        } else if (!onlySelect) {
            type = 1;
        }
        dialogsSearchAdapter = new DialogsSearchAdapter(context, type, dialogsType);
        dialogsSearchAdapter.setDelegate(new DialogsSearchAdapter.DialogsSearchAdapterDelegate() {
            @Override
            public void searchStateChanged(boolean search) {
                if (searching && searchWas && searchEmptyView != null) {
                    if (search) {
                        searchEmptyView.showProgress();
                    } else {
                        searchEmptyView.showTextView();
                    }
                }
            }

            @Override
            public void didPressedOnSubDialog(int did) {
                if (onlySelect) {
                    didSelectResult(did, true, false);
                } else {
                    Bundle args = new Bundle();
                    if (did > 0) {
                        args.putInt("user_id", did);
                    } else {
                        args.putInt("chat_id", -did);
                    }
                    if (actionBar != null) {
                        actionBar.closeSearchField();
                    }
                    if (AndroidUtilities.isTablet()) {
                        if (dialogsAdapter != null) {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = did);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                        }
                    } else {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            presentFragment(new ChatActivity(args));
                        }
                    }
                }
            }

            @Override
            public void needRemoveHint(final int did) {
                if (getParentActivity() == null) {
                    return;
                }
                TLRPC.User user = MessagesController.getInstance().getUser(did);
                if (user == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(LocaleController.formatString("ChatHintsDelete", R.string.ChatHintsDelete, ContactsController.formatName(user.first_name, user.last_name)));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SearchQuery.removePeer(did);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            }
        });

        if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
            searchEmptyView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            listView.setEmptyView(progressView);
        } else {
            searchEmptyView.setVisibility(View.GONE);
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }
        if (searchString != null) {
            actionBar.openSearchField(searchString);
        }

        if (!onlySelect && dialogsType == 0) {
            frameLayout.addView(fragmentContextView = new FragmentContextView(context, this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
        }

        // Adel
        floatingButton.setOnLongClickListener(new View.OnLongClickListener()
        {
            public boolean onLongClick(View view)
            {
                gotoHiddenMode(context);
                return true;
            }
        });

        // Adel
        floatingButtonLock.setOnLongClickListener(new View.OnLongClickListener()
        {
            public boolean onLongClick(View view)
            {
                ChangePassword(context);
                return true;
            }
        });

        // Adel
        TabInit(context);

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdateTabHost(); // Adel

        if (dialogsAdapter != null) {
            dialogsAdapter.hiddenMode = hiddenMode; // Adel
            dialogsAdapter.notifyDataSetChanged();
        }
        if (dialogsSearchAdapter != null) {
            dialogsSearchAdapter.notifyDataSetChanged();
        }
        if (checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionContacts", R.string.PermissionContacts));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons();
                    }
                }
            }
        }
    }

    // Adel
    private void TabInit(final Context context)
    {
        TabSetting.GetTabs(tabHost, badges);

        final ViewGroup test   = (ViewGroup) (tabHost.getChildAt(0));
        int             tabLen = test.getChildCount();
        for (int i = 0; i < tabLen; i++)
        {
            View v = test.getChildAt(i);
            v.setPadding(0, 0, 0, 0);

            final Integer tabId = i;
            // Adel
            v.setOnLongClickListener(new View.OnLongClickListener()
            {
                @Override
                public boolean onLongClick(View view)
                {
                    BottomSheet.Builder builder = new BottomSheet.Builder(context);
                    builder.setItems(new CharSequence[]{
                            LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)
                    }, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            markAllAsRead(tabId);
                        }
                    });

                    builder.create().show();
                    return false;
                }
            });
        }

        UpdateTabHost();
    }

    // Adel
    private void UpdateTabHost()
    {
        try
        {
            TabSetting.setBadges(badges);
            tabHost.getTabAt(Setting.GetLastTabIndex()).select();
        } catch (Exception ignored)
        {
            tabHost.getTabAt(0).select();
        }
    }

    // Adel
    private void ToggleHidden(long selectedDialog)
    {
        if (!HiddenController.IsHidden(selectedDialog))
        {
            HiddenController.addToHidden(selectedDialog);
        }
        else
        {
            HiddenController.RemoveFromHidden(selectedDialog);
        }
        dialogsAdapter.hiddenMode = hiddenMode;
        dialogsAdapter.notifyDataSetChanged();
        //listView.getAdapter().notifyDataSetChanged();
        if (!Setting.HiddenMsgDisplayed())
        {
            Setting.HiddenMsgDisplayedYes();
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            builder.setMessage(LocaleController.getString("HiddenMsg1", R.string.HiddenMsg1));
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    dialogInterface.dismiss();
                }
            });
            builder.create().show();
        }
    }

    // Adel
    private void gotoHiddenMode(final Context context)
    {
        if (Setting.HideHavePass())
        {
            searchFiledItem.openSearch(true);
            searchFiledItem.getSearchField().setHint(LocaleController.getString("PasscodePassword", R.string.PasscodePassword));
            if (Setting.getHidePasswordType() == 0)
            {
                searchFiledItem.getSearchField().setInputType(3);
            }
            else
            {
                searchFiledItem.getSearchField().setInputType(129);
            }
            searchFiledItem.getSearchField().setTransformationMethod(PasswordTransformationMethod.getInstance());
            enterHiddenPassMode = true;
        }
        else
        {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
            builder.setMessage(LocaleController.getString("EnterPasswordForHideChats", R.string.EnterPasswordForHideChats));
            builder.setTitle(LocaleController.getString("CreatePassword", R.string.CreatePassword));

            // Set up the input
            final EditText input = new EditText(context);
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(3);
            builder.setView(input);

            // Set up the buttons
            builder.setPositiveButton(LocaleController.getString("SavePassword", R.string.SavePassword), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    String m_Text = input.getText().toString();
                    if (Setting.setHidePassword(m_Text))
                    {
                        Snackbar.make(listView, LocaleController.getString("PasswordSaved", R.string.PasswordSaved), Snackbar.LENGTH_SHORT).show();

                        dialog.cancel();
                        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("ChangePassword", R.string.ChangePassword));
                        builder.setMessage(LocaleController.getString("ChangePasswordMsg", R.string.ChangePasswordMsg));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                dialogInterface.dismiss();
                            }
                        });
                        builder.create().show();
                        hiddenMode = true;
                        floatingButtonLock.setVisibility(View.VISIBLE);
                        dialogsAdapter.hiddenMode = true;
                        dialogsAdapter.notifyDataSetChanged();
                    }
                    else
                    {
                        input.setText(null);
                        Snackbar.make(listView, LocaleController.getString("PasswordError", R.string.PasswordError), Snackbar.LENGTH_SHORT).show();
                        gotoHiddenMode(context);
                    }
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.cancel();
                }
            });

            android.app.AlertDialog Dialogx = builder.create();
            Dialogx.show();
            Dialogx.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            Dialogx.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        }
    }

    // Adel
    private void ChangePassword(final Context context)
    {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setMessage(LocaleController.getString("EnterNewPassword", R.string.EnterNewPassword));
        builder.setTitle(LocaleController.getString("ChangePassword", R.string.ChangePassword));

        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(3);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(LocaleController.getString("SavePassword", R.string.SavePassword), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String m_Text = input.getText().toString();
                if (Setting.setHidePassword(m_Text))
                {
                    Snackbar.make(listView, LocaleController.getString("PasswordSaved", R.string.PasswordSaved), Snackbar.LENGTH_SHORT).show();
                    dialog.cancel();
                }
                else
                {
                    input.setText(null);
                    Snackbar.make(listView, LocaleController.getString("PasswordError", R.string.PasswordError), Snackbar.LENGTH_SHORT).show();
                    ChangePassword(context);
                }

            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });
        android.app.AlertDialog Dialogx = builder.create();
        Dialogx.show();
        Dialogx.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        Dialogx.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    // Adel
    private void onSwipeRight()
    {
        try
        {
            int current = tabHost.getSelectedTabPosition();
            if (current == 0)
            {
                current = tabHost.getTabCount() - 1;
            }
            else
            {
                current--;
            }
            tabHost.getTabAt(current).select();
        }
        catch (Exception ignored)
        {
            Log.d("_Adel", "Swipe Exception" + ignored.getMessage());
        }
    }

    // Adel
    private void onSwipeLeft()
    {
        try
        {
            int current = tabHost.getSelectedTabPosition();
            if (current == tabHost.getTabCount() - 1)
            {
                current = 0;
            }
            else
            {
                current++;
            }
            tabHost.getTabAt(current).select();
        }
        catch (Exception ignored)
        {
            Log.d("_Adel", "Swipe Exception" + ignored.getMessage());
        }
    }

    // Adel
    private void markAsRead(long dialog_id)
    {
        try
        {
            int t = MessagesController.getInstance().dialogMessage.get(dialog_id).getId();
            MessagesController.getInstance().markDialogAsRead(dialog_id, t, t, 0, true, false);
        } catch (Exception ignored)
        {

        }
    }

    // Adel
    private void markAllAsRead(int tabId)
    {
        try
        {
            // Read All Tab Messages
            tabHost.getTabAt(tabId).select();

            ArrayList<TLRPC.TL_dialog> list = dialogsAdapter.getDialogsArray();
            for (int j = 0; j < list.size(); j++)
            {
                if (list.get(j).unread_count > 0)
                {
                    markAsRead(list.get(j).id);
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    // Adel
    @Override
    protected void onBecomeFullyVisible()
    {
        super.onBecomeFullyVisible();

        TabSetting.setBadges(badges);
    }

    // Adel
    public void Bot(String username, boolean mute)
    {
        if (getParentActivity() == null)
        {
            return;
        }

        TLRPC.User user = (TLRPC.User) MessagesController.getInstance().getUserOrChat(username); // Adel Temp
        if (user == null)
        {
            MessagesController.getInstance().openByUserNameAsHidden(username);
            return;
        }

        MessagesController.getInstance().unblockUser(user.id);
        SendMessagesHelper.getInstance().sendMessage("/start", (long) user.id, null, null, false, null, null, null);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_CONTACTS);
            permissons.add(Manifest.permission.WRITE_CONTACTS);
            permissons.add(Manifest.permission.GET_ACCOUNTS);
        }
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        String[] items = permissons.toArray(new String[permissons.size()]);
        try {
            activity.requestPermissions(items, 1);
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!onlySelect && floatingButton != null) {
            floatingButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    floatingButton.setTranslationY(floatingHidden ? AndroidUtilities.dp(100) : 0);
                    floatingButton.setClickable(!floatingHidden);
                    if (floatingButton != null) {
                        floatingButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a || grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        ContactsController.getInstance().readContacts();
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        ImageLoader.getInstance().checkMediaPaths();
                        break;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        // Adel
        TabSetting.setBadges(badges);

        if (id == NotificationCenter.dialogsNeedReload) {
            if (dialogsAdapter != null) {
                if (dialogsAdapter.isDataSetChanged()) {
                    dialogsAdapter.hiddenMode = hiddenMode; // Adel
                    dialogsAdapter.notifyDataSetChanged();
                } else {
                    updateVisibleRows(MessagesController.UPDATE_MASK_NEW_MESSAGE);
                }
            }
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
            if (listView != null) {
                try {
                    if (MessagesController.getInstance().loadingDialogs && dialogsAdapter != null && dialogsAdapter.getDialogsArray().isEmpty()) { // Adel
                        searchEmptyView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    } else {
                        progressView.setVisibility(View.GONE);
                        if (searching && searchWas) {
                            emptyView.setVisibility(View.GONE);
                            listView.setEmptyView(searchEmptyView);
                        } else {
                            searchEmptyView.setVisibility(View.GONE);
                            listView.setEmptyView(emptyView);
                        }
                    }

	                // Adel
	                if (!MessagesController.getInstance().loadingDialogs && dialogsAdapter.getDialogsArray().isEmpty()) {
		                emptyView.setVisibility(View.VISIBLE);
	                }
	                else {
		                emptyView.setVisibility(View.GONE);
	                }
                } catch (Exception e) {
                    FileLog.e(e); //TODO fix it in other way?
                }
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.updateInterfaces) {
            updateVisibleRows((Integer) args[0]);
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded = false;
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.openedChatChanged) {
            if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                boolean close = (Boolean) args[1];
                long dialog_id = (Long) args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                if (dialogsAdapter != null) {
                    dialogsAdapter.setOpenedDialogId(openedDialogId);
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.messageReceivedByAck || id == NotificationCenter.messageReceivedByServer || id == NotificationCenter.messageSendError) {
            updateVisibleRows(MessagesController.UPDATE_MASK_SEND_STATE);
        } else if (id == NotificationCenter.didSetPasscode) {
            updatePasscodeButton();
        } else if (id == NotificationCenter.needReloadRecentDialogsSearch) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.loadRecentSearch();
            }
        } else if (id == NotificationCenter.didLoadedReplyMessages) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.reloadHints) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
        }
    }

    private ArrayList<TLRPC.TL_dialog> getDialogsArray() {
        if (dialogsType == 0) {
            dialogsAdapter.hiddenMode = hiddenMode; // Adel
            return dialogsAdapter.getDialogsArray(); // Adel
        } else if (dialogsType == 1) {
            return MessagesController.getInstance().dialogsServerOnly;
        } else if (dialogsType == 2) {
            return MessagesController.getInstance().dialogsGroupsOnly;
        }
        return null;
    }

    public void setSideMenu(RecyclerView recyclerView) {
        sideMenu = recyclerView;
        sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
    }

    private void updatePasscodeButton() {
        if (passcodeItem == null) {
            return;
        }
        if (UserConfig.passcodeHash.length() != 0 && !searching) {
            passcodeItem.setVisibility(View.VISIBLE);
            if (UserConfig.appLocked) {
                passcodeItem.setIcon(R.drawable.lock_close);
            } else {
                passcodeItem.setIcon(R.drawable.lock_open);
            }
        } else {
            passcodeItem.setVisibility(View.GONE);
        }
    }

    private void hideFloatingButton(boolean hide) {
        // Adel
        if (hide) {
            return;
        }

        // Adel
        if (hiddenMode) {
            return;
        }

        if (floatingHidden == hide) {
            return;
        }
        floatingHidden = hide;
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatingButton, "translationY", floatingHidden ? AndroidUtilities.dp(100) : 0).setDuration(300);
        animator.setInterpolator(floatingInterpolator);
        floatingButton.setClickable(!hide);
        animator.start();
    }

    private void updateVisibleRows(int mask) {
        if (listView == null) {
            return;
        }

        TabSetting.setBadges(badges); // Adel

        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof DialogCell) {
                if (listView.getAdapter() != dialogsSearchAdapter) {
                    DialogCell cell = (DialogCell) child;
                    if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0) {
                        cell.checkCurrentDialogIndex();
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else if ((mask & MessagesController.UPDATE_MASK_SELECT_DIALOG) != 0) {
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else {
                        cell.update(mask);
                    }

                    MessagesController.getInstance().processUnreadDialogs(); // Adel
                    listView.getAdapter().notifyDataSetChanged(); // Adel
                }
            } else if (child instanceof UserCell) {
                ((UserCell) child).update(mask);
            } else if (child instanceof ProfileSearchCell) {
                ((ProfileSearchCell) child).update(mask);
            } else if (child instanceof RecyclerListView) {
                RecyclerListView innerListView = (RecyclerListView) child;
                int count2 = innerListView.getChildCount();
                for (int b = 0; b < count2; b++) {
                    View child2 = innerListView.getChildAt(b);
                    if (child2 instanceof HintDialogCell) {
                        ((HintDialogCell) child2).checkUnreadCounter(mask);
                    }
                }
            }
        }
    }

    public void setDelegate(DialogsActivityDelegate dialogsActivityDelegate) {
        delegate = dialogsActivityDelegate;
    }

    public void setSearchString(String string) {
        searchString = string;
    }

    public boolean isMainDialogList() {
        return delegate == null && searchString == null;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (addToGroupAlertString == null) {
            if ((int) dialog_id < 0) {
                TLRPC.Chat chat = MessagesController.getInstance().getChat(-(int) dialog_id);
                if (ChatObject.isChannel(chat) && !chat.megagroup && (cantSendToChannels || !ChatObject.isCanWriteToChannel(-(int) dialog_id))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setMessage(LocaleController.getString("ChannelCantSendMessage", R.string.ChannelCantSendMessage));
                    builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                    return;
                }
            }
        }
        if (useAlert && (selectAlertString != null && selectAlertStringGroup != null || addToGroupAlertString != null)) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            int lower_part = (int) dialog_id;
            int high_id = (int) (dialog_id >> 32);
            if (lower_part != 0) {
                if (high_id == 1) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(lower_part);
                    if (chat == null) {
                        return;
                    }
                    builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                } else {
                    if (lower_part > 0) {
                        TLRPC.User user = MessagesController.getInstance().getUser(lower_part);
                        if (user == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
                    } else if (lower_part < 0) {
                        TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                        if (chat == null) {
                            return;
                        }
                        if (addToGroupAlertString != null) {
                            builder.setMessage(LocaleController.formatStringSimple(addToGroupAlertString, chat.title));
                        } else {
                            builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                        }
                    }
                }
            } else {
                TLRPC.EncryptedChat chat = MessagesController.getInstance().getEncryptedChat(high_id);
                TLRPC.User user = MessagesController.getInstance().getUser(chat.user_id);
                if (user == null) {
                    return;
                }
                builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
            }

            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(dialog_id, false, false);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                delegate.didSelectDialog(DialogsActivity.this, dialog_id, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate сellDelegate = new ThemeDescription.ThemeDescriptionDelegate() {
            @Override
            public void didSetColor(int color) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof ProfileSearchCell) {
                        ((ProfileSearchCell) child).update(0);
                    } else if (child instanceof DialogCell) {
                        ((DialogCell) child).update(0);
                    }
                }
                RecyclerListView recyclerListView = dialogsSearchAdapter.getInnerListView();
                if (recyclerListView != null) {
                    count = recyclerListView.getChildCount();
                    for (int a = 0; a < count; a++) {
                        View child = recyclerListView.getChildAt(a);
                        if (child instanceof HintDialogCell) {
                            ((HintDialogCell) child).update();
                        }
                    }
                }
            }
        };
        return new ThemeDescription[]{
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(searchEmptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),
                new ThemeDescription(searchEmptyView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle),

                new ThemeDescription(emptyTextView1, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),
                new ThemeDescription(emptyTextView2, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),

                new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon),
                new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground),
                new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground),

                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.avatar_photoDrawable, Theme.avatar_broadcastDrawable}, null, Theme.key_avatar_text),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundRed),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundOrange),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundViolet),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundGreen),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundCyan),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundBlue),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundPink),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, Theme.dialogs_namePaint, null, null, Theme.key_chats_name),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, Theme.dialogs_nameEncryptedPaint, null, null, Theme.key_chats_secretName),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_lockDrawable}, null, Theme.key_chats_secretIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_groupDrawable, Theme.dialogs_broadcastDrawable, Theme.dialogs_botDrawable}, null, Theme.key_chats_nameIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_pinnedDrawable}, null, Theme.key_chats_pinnedIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePaint, null, null, Theme.key_chats_message),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_chats_nameMessage),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_chats_draft),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_chats_attachMessage),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePrintingPaint, null, null, Theme.key_chats_actionMessage),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_timePaint, null, null, Theme.key_chats_date),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_pinnedPaint, null, null, Theme.key_chats_pinnedOverlay),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_tabletSeletedPaint, null, null, Theme.key_chats_tabletSelectedOverlay),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_checkDrawable, Theme.dialogs_halfCheckDrawable}, null, Theme.key_chats_sentCheck),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_clockDrawable}, null, Theme.key_chats_sentClock),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_errorPaint, null, null, Theme.key_chats_sentError),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_errorDrawable}, null, Theme.key_chats_sentErrorIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedCheckDrawable}, null, Theme.key_chats_verifiedCheck),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedDrawable}, null, Theme.key_chats_verifiedBackground),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_muteDrawable}, null, Theme.key_chats_muteIcon),

                new ThemeDescription(sideMenu, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_chats_menuBackground),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuName),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhone),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhoneCats),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuCloudBackgroundCats),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, new String[]{"cloudDrawable"}, null, null, null, Theme.key_chats_menuCloud),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chat_serviceBackground),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadow),

                new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemIcon),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText),

                new ThemeDescription(sideMenu, 0, new Class[]{DividerCell.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(listView, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle),

                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_offlinePaint, null, null, Theme.key_windowBackgroundWhiteGrayText3),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_onlinePaint, null, null, Theme.key_windowBackgroundWhiteBlueText3),

                new ThemeDescription(listView, 0, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection),

                new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{HashtagSearchCell.class}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),

                new ThemeDescription(progressView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle),

                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter),
                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted),
                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText),
                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),

                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerBackground),
                new ThemeDescription(fragmentContextView, 0, new Class[]{FragmentContextView.class}, new String[]{"playButton"}, null, null, null, Theme.key_inappPlayerPlayPause),
                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerTitle),
                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerPerformer),
                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{FragmentContextView.class}, new String[]{"closeButton"}, null, null, null, Theme.key_inappPlayerClose),

                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_returnToCallBackground),
                new ThemeDescription(fragmentContextView, 0, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_returnToCallText),

                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackgroundGray),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlack),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextLink),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLinkSelection),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue2),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue3),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue4),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextRed),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray2),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray3),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray4),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogIcon),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextHint),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputField),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputFieldActivated),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareCheck),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareUnchecked),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareDisabled),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackgroundChecked),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogProgressCircle),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButton),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButtonSelector),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogScrollGlow),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBox),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBoxCheck),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeText),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgress),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgressBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogGrayLine),
        };
    }
}
