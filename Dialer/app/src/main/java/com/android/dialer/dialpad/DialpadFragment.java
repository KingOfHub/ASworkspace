/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.dialpad;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.MoreContactUtils;
import com.android.contacts.common.util.PhoneNumberFormatter;
import com.android.contacts.common.util.StopWatch;
import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.NeededForReflection;
import com.android.dialer.R;
import com.android.dialer.SpecialCharSequenceMgr;
import com.android.dialer.SpeedDialListActivity;
import com.android.dialer.SpeedDialUtils;
import com.android.dialer.util.DialerUtils;
import com.android.dialerbind.analytics.AnalyticsFragment;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.phone.common.CallLogAsync;
import com.android.phone.common.HapticFeedback;
import com.android.phone.common.animation.AnimUtils;
import com.android.phone.common.dialpad.DialpadKeyButton;
import com.android.phone.common.dialpad.DialpadView;

import com.google.common.annotations.VisibleForTesting;

import java.util.HashSet;
import java.util.Locale;

import static com.android.internal.telephony.PhoneConstants.SUBSCRIPTION_KEY;

/**
 * Fragment that displays a twelve-key phone dialpad.
 */
public class DialpadFragment extends AnalyticsFragment
        implements View.OnClickListener,
        View.OnLongClickListener, View.OnKeyListener,
        AdapterView.OnItemClickListener, TextWatcher,
        PopupMenu.OnMenuItemClickListener,
        DialpadKeyButton.OnPressedListener {
    private static final String TAG = DialpadFragment.class.getSimpleName();

    private static final String ACTION_ADD_VOICEMAIL
            = "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL";

    private Context mContext;

    /**
     * This interface allows the DialpadFragment to tell its hosting Activity when and when not
     * to display the "dial" button. While this is logically part of the DialpadFragment, the
     * need to have a particular kind of slick animation puts the "dial" button in the parent.
     *
     * The parent calls dialButtonPressed() and optionsMenuInvoked() on the dialpad fragment
     * when appropriate.
     *
     * TODO: Refactor the app so this interchange is a bit cleaner.
     */
    public interface HostInterface {
        void setConferenceDialButtonVisibility(boolean enabled);
        void setConferenceDialButtonImage(boolean setAddParticipantButton);
    }

    /**
     * LinearLayout with getter and setter methods for the translationY property using floats,
     * for animation purposes.
     */
    public static class DialpadSlidingRelativeLayout extends RelativeLayout {

        public DialpadSlidingRelativeLayout(Context context) {
            super(context);
        }

        public DialpadSlidingRelativeLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public DialpadSlidingRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @NeededForReflection
        public float getYFraction() {
            final int height = getHeight();
            if (height == 0) return 0;
            return getTranslationY() / height;
        }

        @NeededForReflection
        public void setYFraction(float yFraction) {
            setTranslationY(yFraction * getHeight());
        }
    }

    public interface OnDialpadQueryChangedListener {
        void onDialpadQueryChanged(String query);
    }

    private static final boolean DEBUG = DialtactsActivity.DEBUG;

    // This is the amount of screen the dialpad fragment takes up when fully displayed
    private static final float DIALPAD_SLIDE_FRACTION = 0.67f;

    private static final String EMPTY_NUMBER = "";
    private static final char PAUSE = ',';
    private static final char WAIT = ';';

    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150;
    private static final int TONE_LENGTH_INFINITE = -1;

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 80;

    /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;

    private OnDialpadQueryChangedListener mDialpadQueryListener;

    private DialpadView mDialpadView;
    private EditText mDigits;
    private EditText mRecipients;
    private View mDigitsContainer;
    private View mDialpad;
    private int mDialpadSlideInDuration;

    /** Remembers if we need to clear digits field when the screen is completely gone. */
    private boolean mClearDigitsOnStop;

    private View mOverflowMenuButton;
    private PopupMenu mOverflowPopupMenu;
    private View mDelete;
    private ToneGenerator mToneGenerator;
    private final Object mToneGeneratorLock = new Object();
    private View mSpacer;

    private FloatingActionButtonController mFloatingActionButtonController;

    /**
     * Set of dialpad keys that are currently being pressed
     */
    private final HashSet<View> mPressedDialpadKeys = new HashSet<View>(12);

    private ListView mDialpadChooser;
    private DialpadChooserAdapter mDialpadChooserAdapter;
    private TextView mOperator;

    /**
     * Regular expression prohibiting manual phone call. Can be empty, which means "no rule".
     */
    private String mProhibitedPhoneNumberRegexp;


    // Last number dialed, retrieved asynchronously from the call DB
    // in onCreate. This number is displayed when the user hits the
    // send key and cleared in onPause.
    private final CallLogAsync mCallLog = new CallLogAsync();
    private String mLastNumberDialed = EMPTY_NUMBER;

    // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;

    // Vibration (haptic feedback) for dialer key presses.
    private final HapticFeedback mHaptic = new HapticFeedback();

    /** Identifier for the "Add Call" intent extra. */
    private static final String ADD_CALL_MODE_KEY = "add_call_mode";

    /** Identifier for the "Add Participant" intent extra. */
    private static final String ADD_PARTICIPANT_KEY = "add_participant";
    private boolean mAddParticipant = false;

    /**
     * Identifier for intent extra for sending an empty Flash message for
     * CDMA networks. This message is used by the network to simulate a
     * press/depress of the "hookswitch" of a landline phone. Aka "empty flash".
     *
     * TODO: Using an intent extra to tell the phone to send this flash is a
     * temporary measure. To be replaced with an Telephony/TelecomManager call in the future.
     * TODO: Keep in sync with the string defined in OutgoingCallBroadcaster.java
     * in Phone app until this is replaced with the Telephony/Telecom API.
     */
    private static final String EXTRA_SEND_EMPTY_FLASH
            = "com.android.phone.extra.SEND_EMPTY_FLASH";

    private String mCurrentCountryIso;

    private PhoneStateListener[] mPhoneStateListener;
    private int mNumPhones;

    private boolean mWasEmptyBeforeTextChange;

    /**
     * This field is set to true while processing an incoming DIAL intent, in order to make sure
     * that SpecialCharSequenceMgr actions can be triggered by user input but *not* by a
     * tel: URI passed by some other app.  It will be set to false when all digits are cleared.
     */
    private boolean mDigitsFilledByIntent;

    private boolean mStartedFromNewIntent = false;
    private boolean mFirstLaunch = false;
    private boolean mAnimate = false;

    private ComponentName mSmsPackageComponentName;

    private static final String PREF_DIGITS_FILLED_BY_INTENT = "pref_digits_filled_by_intent";

    /**
     * Return an Intent for launching voicemail screen.
     */
    private static Intent getVoicemailIntent() {
        return CallUtil.getCallIntent(Uri.fromParts(PhoneAccount.SCHEME_VOICEMAIL, "", null));
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) getActivity().getSystemService(Context.TELECOM_SERVICE);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mWasEmptyBeforeTextChange = TextUtils.isEmpty(s);
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
        if (mWasEmptyBeforeTextChange != TextUtils.isEmpty(input)) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
                updateMenuOverflowButton(mWasEmptyBeforeTextChange);
            }
        }

        // DTMF Tones do not need to be played here any longer -
        // the DTMF dialer handles that functionality now.
    }

    @Override
    public void afterTextChanged(Editable input) {
        // When DTMF dialpad buttons are being pressed, we delay SpecialCharSequencMgr sequence,
        // since some of SpecialCharSequenceMgr's behavior is too abrupt for the "touch-down"
        // behavior.
        if (!mDigitsFilledByIntent &&
                SpecialCharSequenceMgr.handleChars(getActivity(), input.toString(), mDigits)) {
            // A special sequence was entered, clear the digits
            mDigits.getText().clear();
        }

        if (isDigitsEmpty()) {
            mDigitsFilledByIntent = false;
            mDigits.setCursorVisible(false);
        }

        if (mDialpadQueryListener != null) {
            mDialpadQueryListener.onDialpadQueryChanged(mDigits.getText().toString());
        }
        updateDeleteButtonEnabledState();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mFirstLaunch = true;
        mCurrentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());

        mNumPhones = getTelephonyManager().getDefault().getPhoneCount();
        mPhoneStateListener = new PhoneStateListener[mNumPhones];
        for (int i = 0; i < mNumPhones; i++) {
            mPhoneStateListener[i] = getPhoneStateListener(i);
        }

        try {
            mHaptic.init(getActivity(),
                         getResources().getBoolean(R.bool.config_enable_dialer_key_vibration));
        } catch (Resources.NotFoundException nfe) {
             Log.e(TAG, "Vibrate control bool missing.", nfe);
        }

        mProhibitedPhoneNumberRegexp = getResources().getString(
                R.string.config_prohibited_phone_number_regexp);

        if (state != null) {
            mDigitsFilledByIntent = state.getBoolean(PREF_DIGITS_FILLED_BY_INTENT);
        }

        mDialpadSlideInDuration = getResources().getInteger(R.integer.dialpad_slide_in_duration);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        final View fragmentView = inflater.inflate(R.layout.dialpad_fragment, container,
                false);
        fragmentView.buildLayer();

        Resources r = getResources();

        mDialpadView = (DialpadView) fragmentView.findViewById(R.id.dialpad_view);
        mDialpadView.setCanDigitsBeEdited(true);
        mDigits = mDialpadView.getDigits();
        mRecipients = (EditText) fragmentView.findViewById(R.id.recipients);
        mDigitsContainer = fragmentView.findViewById(R.id.digits_container);
        mDialpad = fragmentView.findViewById(R.id.dialpad);
        if (mRecipients != null) {
            mRecipients.setVisibility(View.GONE);
            mRecipients.addTextChangedListener(this);
        }
        mDigits.setKeyListener(UnicodeDialerKeyListener.INSTANCE);
        mDigits.setOnClickListener(this);
        mDigits.setOnKeyListener(this);
        mDigits.setOnLongClickListener(this);
        mDigits.addTextChangedListener(this);
        mDigits.setElegantTextHeight(false);
        PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(getActivity(), mDigits);
        // Check for the presence of the keypad
        View oneButton = fragmentView.findViewById(R.id.one);
        if (oneButton != null) {
            configureKeypadListeners(fragmentView);
        }

        mDelete = mDialpadView.getDeleteButton();

        if (mDelete != null) {
            mDelete.setOnClickListener(this);
            mDelete.setOnLongClickListener(this);
        }

        // Populate the overflow menu in onCreate instead of onResume to avoid PopupMenu's memory leak.
        mOverflowMenuButton = mDialpadView.getOverflowMenuButton();
        mOverflowPopupMenu = buildOptionsMenu(mOverflowMenuButton);
        mOverflowMenuButton.setOnTouchListener(mOverflowPopupMenu.getDragToOpenListener());
        mOverflowMenuButton.setOnClickListener(this);
        mOverflowMenuButton.setVisibility(isDigitsEmpty() ? View.INVISIBLE : View.VISIBLE);

        mSpacer = fragmentView.findViewById(R.id.spacer);
        mSpacer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isDigitsEmpty()) {
                    hideAndClearDialpad(true);
                    return true;
                }
                return false;
            }
        });

        mDigits.setCursorVisible(false);

        // Set up the "dialpad chooser" UI; see showDialpadChooser().
        mDialpadChooser = (ListView) fragmentView.findViewById(R.id.dialpadChooser);
        mDialpadChooser.setOnItemClickListener(this);

        final View floatingActionButtonContainer =
                fragmentView.findViewById(R.id.dialpad_floating_action_button_container);
        final View floatingActionButton =
                (ImageButton) fragmentView.findViewById(R.id.dialpad_floating_action_button);
        floatingActionButton.setOnClickListener(this);
        mFloatingActionButtonController = new FloatingActionButtonController(getActivity(),
                floatingActionButtonContainer, floatingActionButton);

        mOperator = (TextView)fragmentView.findViewById(R.id.dialpad_floating_operator);

        return fragmentView;
    }

    private boolean isLayoutReady() {
        return mDigits != null;
    }

    public EditText getDigitsWidget() {
        return mDigits;
    }

    /**
     * @return true when {@link #mDigits} is actually filled by the Intent.
     */
    private boolean fillDigitsIfNecessary(Intent intent) {
        // Only fills digits from an intent if it is a new intent.
        // Otherwise falls back to the previously used number.
        if (!mFirstLaunch && !mStartedFromNewIntent) {
            return false;
        }

        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                if (PhoneAccount.SCHEME_TEL.equals(uri.getScheme())) {
                    // Put the requested number into the input area
                    String data = uri.getSchemeSpecificPart();
                    // Remember it is filled via Intent.
                    mDigitsFilledByIntent = true;
                    final String converted = PhoneNumberUtils.convertKeypadLettersToDigits(
                            PhoneNumberUtils.replaceUnicodeDigits(data));
                    setFormattedDigits(converted, null);
                    return true;
                } else {
                    String type = intent.getType();
                    if (People.CONTENT_ITEM_TYPE.equals(type)
                            || Phones.CONTENT_ITEM_TYPE.equals(type)) {
                        // Query the phone number
                        Cursor c = getActivity().getContentResolver().query(intent.getData(),
                                new String[] {PhonesColumns.NUMBER, PhonesColumns.NUMBER_KEY},
                                null, null, null);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    // Remember it is filled via Intent.
                                    mDigitsFilledByIntent = true;
                                    // Put the number into the input area
                                    setFormattedDigits(c.getString(0), c.getString(1));
                                    return true;
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines whether an add call operation is requested.
     *
     * @param intent The intent.
     * @return {@literal true} if add call operation was requested.  {@literal false} otherwise.
     */
    private static boolean isAddCallMode(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            // see if we are "adding a call" from the InCallScreen; false by default.
            return intent.getBooleanExtra(ADD_CALL_MODE_KEY, false);
        } else {
            return false;
        }
    }

    /**
     * Checks the given Intent and changes dialpad's UI state. For example, if the Intent requires
     * the screen to enter "Add Call" mode, this method will show correct UI for the mode.
     */
    private void configureScreenFromIntent(Activity parent) {
        // If we were not invoked with a DIAL intent,
        if (!(parent instanceof DialtactsActivity)) {
            setStartedFromNewIntent(false);
            return;
        }
        // See if we were invoked with a DIAL intent. If we were, fill in the appropriate
        // digits in the dialer field.
        Intent intent = parent.getIntent();

        if (!isLayoutReady()) {
            // This happens typically when parent's Activity#onNewIntent() is called while
            // Fragment#onCreateView() isn't called yet, and thus we cannot configure Views at
            // this point. onViewCreate() should call this method after preparing layouts, so
            // just ignore this call now.
            Log.i(TAG,
                    "Screen configuration is requested before onCreateView() is called. Ignored");
            return;
        }

        boolean needToShowDialpadChooser = false;

        // Be sure *not* to show the dialpad chooser if this is an
        // explicit "Add call" action, though.
        final boolean isAddCallMode = isAddCallMode(intent);
        if (!isAddCallMode) {

            // Don't show the chooser when called via onNewIntent() and phone number is present.
            // i.e. User clicks a telephone link from gmail for example.
            // In this case, we want to show the dialpad with the phone number.
            final boolean digitsFilled = fillDigitsIfNecessary(intent);
            if (!(mStartedFromNewIntent && digitsFilled)) {

                final String action = intent.getAction();
                if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)
                        || Intent.ACTION_MAIN.equals(action)) {
                    // If there's already an active call, bring up an intermediate UI to
                    // make the user confirm what they really want to do.
                    if (isPhoneInUse()) {
                        needToShowDialpadChooser = true;
                    }
                }

            }
        } else {
            mAddParticipant = intent.getBooleanExtra(ADD_PARTICIPANT_KEY, false);
            ((HostInterface) getActivity()).setConferenceDialButtonVisibility(true);
        }
        showDialpadChooser(needToShowDialpadChooser);
        setStartedFromNewIntent(false);
    }

    public void setStartedFromNewIntent(boolean value) {
        mStartedFromNewIntent = value;
    }

    /**
     * Sets formatted digits to digits field.
     */
    private void setFormattedDigits(String data, String normalizedNumber) {
        // strip the non-dialable numbers out of the data string.
        String dialString = PhoneNumberUtils.extractNetworkPortion(data);
        dialString =
                PhoneNumberUtils.formatNumber(dialString, normalizedNumber, mCurrentCountryIso);
        if (!TextUtils.isEmpty(dialString)) {
            Editable digits = mDigits.getText();
            digits.replace(0, digits.length(), dialString);
            // for some reason this isn't getting called in the digits.replace call above..
            // but in any case, this will make sure the background drawable looks right
            afterTextChanged(digits);
        }
    }

    private void configureKeypadListeners(View fragmentView) {
        final int[] buttonIds = new int[] {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five,
                R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star, R.id.zero, R.id.pound};

        DialpadKeyButton dialpadKey;

        for (int i = 0; i < buttonIds.length; i++) {
            dialpadKey = (DialpadKeyButton) fragmentView.findViewById(buttonIds[i]);
            dialpadKey.setOnPressedListener(this);
            // Long-pressing button from two to nine will set up speed key dial.
            if (i > 0 && i < buttonIds.length - 3) {
                dialpadKey.setOnLongClickListener(this);
            }
        }

        // Long-pressing one button will initiate Voicemail.
        final DialpadKeyButton one = (DialpadKeyButton) fragmentView.findViewById(R.id.one);
        one.setOnLongClickListener(this);

        // Long-pressing zero button will enter '+' instead.
        final DialpadKeyButton zero = (DialpadKeyButton) fragmentView.findViewById(R.id.zero);
        zero.setOnLongClickListener(this);

        // Long-pressing star button will enter ','(pause) instead.
        final DialpadKeyButton star = (DialpadKeyButton) fragmentView.findViewById(R.id.star);
        star.setOnLongClickListener(this);

        // Long-pressing pound button will enter ';'(wait) instead.
        final DialpadKeyButton pound = (DialpadKeyButton) fragmentView.findViewById(R.id.pound);
        pound.setOnLongClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        final long start = System.currentTimeMillis();
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                } catch (RuntimeException e) {
                    Log.w(TAG, "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }
        final long total = System.currentTimeMillis() - start;
        if (total > 50) {
            Log.i(TAG, "Time for ToneGenerator creation: " + total);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        final DialtactsActivity activity = (DialtactsActivity) getActivity();
        mDialpadQueryListener = activity;

        final StopWatch stopWatch = StopWatch.start("Dialpad.onResume");

        // Query the last dialed number. Do it first because hitting
        // the DB is 'slow'. This call is asynchronous.
        queryLastOutgoingCall();

        stopWatch.lap("qloc");

        final ContentResolver contentResolver = activity.getContentResolver();

        // retrieve the DTMF tone play back setting.
        mDTMFToneEnabled = Settings.System.getInt(contentResolver,
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        stopWatch.lap("dtwd");

        // Retrieve the haptic feedback setting.
        mHaptic.checkSystemSetting();

        stopWatch.lap("hptc");

        mPressedDialpadKeys.clear();

        configureScreenFromIntent(getActivity());

        stopWatch.lap("fdin");

        // While we're in the foreground, listen for phone state changes,
        // purely so that we can take down the "dialpad chooser" if the
        // phone becomes idle while the chooser UI is visible.
        listen();

        stopWatch.lap("tm");

        // Potentially show hint text in the mDigits field when the user
        // hasn't typed any digits yet.  (If there's already an active call,
        // this hint text will remind the user that he's about to add a new
        // call.)
        //
        // TODO: consider adding better UI for the case where *both* lines
        // are currently in use.  (Right now we let the user try to add
        // another call, but that call is guaranteed to fail.  Perhaps the
        // entire dialer UI should be disabled instead.)
        if (isPhoneInUse()) {
            final SpannableString hint = new SpannableString(
                    getActivity().getString(R.string.dialerDialpadHintText));
            hint.setSpan(new RelativeSizeSpan(0.8f), 0, hint.length(), 0);
            mDigits.setHint(hint);
        } else {
            // Common case; no hint necessary.
            mDigits.setHint(null);

            // Also, a sanity-check: the "dialpad chooser" UI should NEVER
            // be visible if the phone is idle!
            showDialpadChooser(false);
        }

        mFirstLaunch = false;

        stopWatch.lap("hnt");

        updateDeleteButtonEnabledState();

        stopWatch.lap("bes");

        stopWatch.stopAndLog(TAG, 50);

        mSmsPackageComponentName = DialerUtils.getSmsComponent(activity);

        if (getTelephonyManager().isMultiSimEnabled() &&
                MoreContactUtils.shouldShowOperator(mContext)) {
            if (SubscriptionManager.isVoicePromptEnabled()) {
                mOperator.setVisibility(View.GONE);
            } else {
                int subId = SubscriptionManager.getDefaultVoiceSubId();
                mOperator.setVisibility(View.VISIBLE);
                mOperator.setText(MoreContactUtils.getNetworkSpnName(mContext, subId));
            }
        } else {
            mOperator.setVisibility(View.GONE);
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        // Stop listening for phone state changes.
        stopListen();

        // Make sure we don't leave this activity with a tone still playing.
        stopTone();
        mPressedDialpadKeys.clear();

        // TODO: I wonder if we should not check if the AsyncTask that
        // lookup the last dialed number has completed.
        mLastNumberDialed = EMPTY_NUMBER;  // Since we are going to query again, free stale number.

        SpecialCharSequenceMgr.cleanup();
    }

    @Override
    public void onStop() {
        super.onStop();

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }

        if (mClearDigitsOnStop) {
            mClearDigitsOnStop = false;
            clearDialpad();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PREF_DIGITS_FILLED_BY_INTENT, mDigitsFilledByIntent);
    }

    private void keyPressed(int keyCode) {
        if (getView().getTranslationY() != 0) {
            return;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                playTone(ToneGenerator.TONE_DTMF_1, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_2:
                playTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_3:
                playTone(ToneGenerator.TONE_DTMF_3, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_4:
                playTone(ToneGenerator.TONE_DTMF_4, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_5:
                playTone(ToneGenerator.TONE_DTMF_5, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_6:
                playTone(ToneGenerator.TONE_DTMF_6, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_7:
                playTone(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_8:
                playTone(ToneGenerator.TONE_DTMF_8, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_9:
                playTone(ToneGenerator.TONE_DTMF_9, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_0:
                playTone(ToneGenerator.TONE_DTMF_0, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_POUND:
                playTone(ToneGenerator.TONE_DTMF_P, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_STAR:
                playTone(ToneGenerator.TONE_DTMF_S, TONE_LENGTH_INFINITE);
                break;
            default:
                break;
        }

        mHaptic.vibrate();
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        mDigits.onKeyDown(keyCode, event);

        // If the cursor is at the end of the text we hide it.
        final int length = mDigits.length();
        if (length == mDigits.getSelectionStart() && length == mDigits.getSelectionEnd()) {
            mDigits.setCursorVisible(false);
        }
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        switch (view.getId()) {
            case R.id.digits:
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    handleDialButtonPressed();
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * When a key is pressed, we start playing DTMF tone, do vibration, and enter the digit
     * immediately. When a key is released, we stop the tone. Note that the "key press" event will
     * be delivered by the system with certain amount of delay, it won't be synced with user's
     * actual "touch-down" behavior.
     */
    @Override
    public void onPressed(View view, boolean pressed) {
        if (DEBUG) Log.d(TAG, "onPressed(). view: " + view + ", pressed: " + pressed);
        if (pressed) {
            switch (view.getId()) {
                case R.id.one: {
                    keyPressed(KeyEvent.KEYCODE_1);
                    break;
                }
                case R.id.two: {
                    keyPressed(KeyEvent.KEYCODE_2);
                    break;
                }
                case R.id.three: {
                    keyPressed(KeyEvent.KEYCODE_3);
                    break;
                }
                case R.id.four: {
                    keyPressed(KeyEvent.KEYCODE_4);
                    break;
                }
                case R.id.five: {
                    keyPressed(KeyEvent.KEYCODE_5);
                    break;
                }
                case R.id.six: {
                    keyPressed(KeyEvent.KEYCODE_6);
                    break;
                }
                case R.id.seven: {
                    keyPressed(KeyEvent.KEYCODE_7);
                    break;
                }
                case R.id.eight: {
                    keyPressed(KeyEvent.KEYCODE_8);
                    break;
                }
                case R.id.nine: {
                    keyPressed(KeyEvent.KEYCODE_9);
                    break;
                }
                case R.id.zero: {
                    keyPressed(KeyEvent.KEYCODE_0);
                    break;
                }
                case R.id.pound: {
                    keyPressed(KeyEvent.KEYCODE_POUND);
                    break;
                }
                case R.id.star: {
                    keyPressed(KeyEvent.KEYCODE_STAR);
                    break;
                }
                default: {
                    Log.wtf(TAG, "Unexpected onTouch(ACTION_DOWN) event from: " + view);
                    break;
                }
            }
            mPressedDialpadKeys.add(view);
        } else {
            mPressedDialpadKeys.remove(view);
            if (mPressedDialpadKeys.isEmpty()) {
                stopTone();
            }
        }
    }

    /**
     * Called by the containing Activity to tell this Fragment to build an overflow options
     * menu for display by the container when appropriate.
     *
     * @param invoker the View that invoked the options menu, to act as an anchor location.
     */
    private PopupMenu buildOptionsMenu(View invoker) {
        invoker.setLayoutDirection(TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()));
        final PopupMenu popupMenu = new PopupMenu(getActivity(), invoker) {
            @Override
            public void show() {
                final Menu menu = getMenu();
                final MenuItem sendMessage = menu.findItem(R.id.menu_send_message);
                sendMessage.setVisible(mSmsPackageComponentName != null);

                final MenuItem ipCallBySlot1 = menu.findItem(R.id.menu_ip_call_by_slot1);
                final MenuItem ipCallBySlot2 = menu.findItem(R.id.menu_ip_call_by_slot2);
                if (MoreContactUtils.isMultiSimEnable(mContext, PhoneConstants.SUB1)) {
                    String sub1Name = MoreContactUtils.getMultiSimAliasesName(
                            mContext, PhoneConstants.SUB1);
                    ipCallBySlot1.setTitle(mContext.getString(
                            com.android.contacts.common.R.string.ip_call_by_slot, sub1Name));
                    ipCallBySlot1.setVisible(true);
                } else {
                    ipCallBySlot1.setVisible(false);
                }
                if (MoreContactUtils.isMultiSimEnable(mContext, PhoneConstants.SUB2)) {
                    String sub2Name = MoreContactUtils.getMultiSimAliasesName(
                            mContext, PhoneConstants.SUB2);
                    ipCallBySlot2.setTitle(mContext.getString(
                            com.android.contacts.common.R.string.ip_call_by_slot, sub2Name));
                    ipCallBySlot2.setVisible(true);
                } else {
                    ipCallBySlot2.setVisible(false);
                }

                final MenuItem VideoCallOption = menu.findItem(R.id.menu_video_call);
                VideoCallOption.setVisible(CallUtil.isCSVTEnabled()
                        || CallUtil.isVideoEnabled(mContext));

                final MenuItem ConferDialerOption
                        = menu.findItem(R.id.menu_add_to_4g_conference_call);
                ConferDialerOption.setVisible(CallUtil.isConferDialerEnabled());

                boolean enable = !isDigitsEmpty();
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setEnabled(enable);
                }

                super.show();
            }
        };
        popupMenu.inflate(R.menu.dialpad_options);
        popupMenu.setOnMenuItemClickListener(this);
        return popupMenu;
    }

    /**
     * Called by the containing Activity to tell this Fragment that the dial button has been
     * pressed.
     */
    public void dialButtonPressed() {
        mHaptic.vibrate();
        handleDialButtonPressed();
    }

    public void dialConferenceButtonPressed() {
        // show dial conference screen if it is not shown
        // If it is already shown, show normal dial screen
        boolean show = (mRecipients != null) && !mRecipients.isShown();
        Log.d(TAG, "dialConferenceButtonPressed show " + show);
        if (show) {
            showDialConference(show);
        } else {
            handleDialButtonPressed();
            showDialConference(!show);
        }
    }

    public void showDialConference(boolean enabled) {
        // Check if onCreateView() is already called by checking one of View
        // objects.
        if (!isLayoutReady()) {
            return;
        }
        Log.d(TAG, "showDialConference " + enabled);
        /*
         * if enabled is true then pick child views that should be
         * visible/invisible when dialpad is choosen from conference dial button
         * if enabled is false then pick child views that should be
         * visible/invisible when dialpad is choosen from other buttons
         */

        // viewable when choosen through conference button
        int conferenceButtonVisibility = (enabled ? View.VISIBLE : View.GONE);
        // not viewable when choosen through conference button
        int nonConferenceButtonVisibility = (enabled ? View.GONE : View.VISIBLE);

        // change the image visibility of the button
        if (mRecipients != null)
            mRecipients.setVisibility(conferenceButtonVisibility);
        if (mDigits != null)
            mDigits.setVisibility(nonConferenceButtonVisibility);
        if (mDelete != null)
            mDelete.setVisibility(nonConferenceButtonVisibility);
        if (mDialpad != null)
            mDialpad.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);

        if (enabled && (HostInterface)getActivity() != null) {
            ((HostInterface)getActivity()).setConferenceDialButtonImage(enabled);
        }
    }

    public void hideAndClearDialConference() {
        // hide the image visibility of the button
        if (mRecipients != null)
            mRecipients.setVisibility(View.GONE);
        if (mDigits != null)
            mDigits.setVisibility(View.GONE);
        if (mDelete != null)
            mDelete.setVisibility(View.GONE);
        if (mDialpad != null)
            mDialpad.setVisibility(View.GONE);
        ((DialtactsActivity) getActivity()).commitDialpadFragmentHide();
    }

    public boolean isRecipientsShown() {
        return mRecipients != null && mRecipients.isShown();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dialpad_floating_action_button:
                mHaptic.vibrate();
                handleDialButtonPressed();
                break;
            case R.id.deleteButton: {
                keyPressed(KeyEvent.KEYCODE_DEL);
                break;
            }
            case R.id.digits: {
                if (!isDigitsEmpty()) {
                    mDigits.setCursorVisible(true);
                }
                break;
            }
            case R.id.dialpad_overflow: {
                mOverflowPopupMenu.show();
                break;
            }
            default: {
                Log.wtf(TAG, "Unexpected onClick() event from: " + view);
                return;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        final Editable digits = mDigits.getText();
        final int id = view.getId();
        switch (id) {
            case R.id.deleteButton: {
                digits.clear();
                return true;
            }
            case R.id.one: {
                // '1' may be already entered since we rely on onTouch() event for numeric buttons.
                // Just for safety we also check if the digits field is empty or not.
                if (isDigitsEmpty() || TextUtils.equals(mDigits.getText(), "1")) {
                    // We'll try to initiate voicemail and thus we want to remove irrelevant string.
                    removePreviousDigitIfPossible();

                    if (isVoicemailAvailable()) {
                        callVoicemail();
                    } else if (getActivity() != null) {
                        // Voicemail is unavailable maybe because Airplane mode is turned on.
                        // Check the current status and show the most appropriate error message.
                        final boolean isAirplaneModeOn =
                                Settings.System.getInt(getActivity().getContentResolver(),
                                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
                        if (isAirplaneModeOn) {
                            DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                    R.string.dialog_voicemail_airplane_mode_message);
                            dialogFragment.show(getFragmentManager(),
                                    "voicemail_request_during_airplane_mode");
                        } else {
                            showNoVMNumberDialog();
                        }
                    }
                    return true;
                }
                return false;
            }
            case R.id.zero: {
                // Remove tentative input ('0') done by onTouch().
                removePreviousDigitIfPossible();
                keyPressed(KeyEvent.KEYCODE_PLUS);

                // Stop tone immediately
                stopTone();
                mPressedDialpadKeys.remove(view);

                return true;
            }
            case R.id.digits: {
                // Right now EditText does not show the "paste" option when cursor is not visible.
                // To show that, make the cursor visible, and return false, letting the EditText
                // show the option by itself.
                mDigits.setCursorVisible(true);
                return false;
            }
            case R.id.star: {
                if (mDigits.length() > 1) {
                    // Remove tentative input ('*') done by onTouch().
                    removePreviousDigitIfPossible();
                    keyPressed(KeyEvent.KEYCODE_COMMA);
                    stopTone();
                    mPressedDialpadKeys.remove(view);
                    return true;
                }
                return false;
            }
            case R.id.pound: {
                if (mDigits.length() > 1) {
                    // Remove tentative input ('#') done by onTouch().
                    removePreviousDigitIfPossible();
                    keyPressed(KeyEvent.KEYCODE_SEMICOLON);
                    stopTone();
                    mPressedDialpadKeys.remove(view);
                    return true;
                }
                return false;
            }
            case R.id.two:
            case R.id.three:
            case R.id.four:
            case R.id.five:
            case R.id.six:
            case R.id.seven:
            case R.id.eight:
            case R.id.nine: {
                if (mDigits.length() == 1) {
                    removePreviousDigitIfPossible();
                    final boolean isAirplaneModeOn =
                            Settings.System.getInt(getActivity().getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON, 0) != 0;
                    if (isAirplaneModeOn) {
                        DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                R.string.dialog_speed_dial_airplane_mode_message);
                        dialogFragment.show(getFragmentManager(),
                                "speed_dial_request_during_airplane_mode");
                    } else {
                        callSpeedNumber(id);
                    }
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    private void ipCallBySlot(int slotId) {
        String prefix = MoreContactUtils.getIPCallPrefix(mContext, slotId);
        if (!TextUtils.isEmpty(prefix)) {
            int[] subId = SubscriptionManager.getSubId(slotId);
            if (subId != null && subId.length >= 1) {
                ComponentName serviceName =
                        new ComponentName("com.android.phone",
                        "com.android.services.telephony.TelephonyConnectionService");
                PhoneAccountHandle account = new PhoneAccountHandle(serviceName,
                        String.valueOf(subId[0]));
                Intent callIntent = new Intent(CallUtil.getCallIntent(
                        prefix + getValidDialNumber(), account));
                startActivity(callIntent);
            } else {
                Intent callIntent = new Intent(CallUtil.getCallIntent(
                        prefix + getValidDialNumber()));
                startActivity(callIntent);
            }
        } else {
            MoreContactUtils.showNoIPNumberDialog(mContext, slotId);
        }
    }

    private String getValidDialNumber() {
        if (mDigits != null)
            return mDigits.getText().toString();
        else
            return "";
    }

    /**
     * Remove the digit just before the current position. This can be used if we want to replace
     * the previous digit or cancel previously entered character.
     */
    private void removePreviousDigitIfPossible() {
        final int currentPosition = mDigits.getSelectionStart();
        if (currentPosition > 0) {
            mDigits.setSelection(currentPosition);
            mDigits.getText().delete(currentPosition - 1, currentPosition);
        }
    }

    public void callVoicemail() {
        DialerUtils.startActivityWithErrorToast(getActivity(), getVoicemailIntent());
        hideAndClearDialpad(false);
    }

    private void hideAndClearDialpad(boolean animate) {
        ((DialtactsActivity) getActivity()).hideDialpadFragment(animate, true);
    }

    public static class ErrorDialogFragment extends DialogFragment {
        private int mTitleResId;
        private int mMessageResId;

        private static final String ARG_TITLE_RES_ID = "argTitleResId";
        private static final String ARG_MESSAGE_RES_ID = "argMessageResId";

        public static ErrorDialogFragment newInstance(int messageResId) {
            return newInstance(0, messageResId);
        }

        public static ErrorDialogFragment newInstance(int titleResId, int messageResId) {
            final ErrorDialogFragment fragment = new ErrorDialogFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_TITLE_RES_ID, titleResId);
            args.putInt(ARG_MESSAGE_RES_ID, messageResId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mTitleResId = getArguments().getInt(ARG_TITLE_RES_ID);
            mMessageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (mTitleResId != 0) {
                builder.setTitle(mTitleResId);
            }
            if (mMessageResId != 0) {
                builder.setMessage(mMessageResId);
            }
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                    });
            return builder.create();
        }
    }

    /**
     * In most cases, when the dial button is pressed, there is a
     * number in digits area. Pack it in the intent, start the
     * outgoing call broadcast as a separate task and finish this
     * activity.
     *
     * When there is no digit and the phone is CDMA and off hook,
     * we're sending a blank flash for CDMA. CDMA networks use Flash
     * messages when special processing needs to be done, mainly for
     * 3-way or call waiting scenarios. Presumably, here we're in a
     * special 3-way scenario where the network needs a blank flash
     * before being able to add the new participant.  (This is not the
     * case with all 3-way calls, just certain CDMA infrastructures.)
     *
     * Otherwise, there is no digit, display the last dialed
     * number. Don't finish since the user may want to edit it. The
     * user needs to press the dial button again, to dial it (general
     * case described above).
     */
    private void handleDialButtonPressed() {
        if (isDigitsEmpty() && (mRecipients == null || !mRecipients.isShown())) {
            // No number entered.
            handleDialButtonClickWithEmptyDigits();
        } else {
            boolean isDigitsShown = mDigits.isShown();
            final String number = isDigitsShown ? mDigits.getText().toString() :
                    mRecipients.getText().toString().trim();
            if (isDigitsShown && isDigitsEmpty()) {
                handleDialButtonClickWithEmptyDigits();
            } else if (mAddParticipant && isDigitsEmpty() && mRecipients.isShown()
                    && isRecipientEmpty()) {
                // mRecipients must be empty
                // TODO add support for conference URI in last number dialed
                // use ErrorDialogFragment instead? also see
                // android.app.AlertDialog
                android.widget.Toast.makeText(getActivity(),
                        "Error: Cannot dial.  Please provide conference recipients.",
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
                // test equipment.
                // TODO: clean it up.
                if (number != null
                        && !TextUtils.isEmpty(mProhibitedPhoneNumberRegexp)
                        && number.matches(mProhibitedPhoneNumberRegexp)) {
                    Log.i(TAG, "The phone number is prohibited explicitly by a rule.");
                    if (getActivity() != null) {
                        DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                R.string.dialog_phone_call_prohibited_message);
                        dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
                    }

                    // Clear the digits just in case.
                    clearDialpad();
                } else {
                    final Intent intent = CallUtil.getCallIntent(number,
                            (getActivity() instanceof DialtactsActivity ?
                                    ((DialtactsActivity) getActivity()).getCallOrigin() : null));
                    if (!isDigitsShown) {
                        // must be dial conference add extra
                        intent.putExtra(TelephonyProperties.EXTRA_DIAL_CONFERENCE_URI, true);
                    }
                    intent.putExtra(ADD_PARTICIPANT_KEY, (mAddParticipant && isPhoneInUse()));
                    DialerUtils.startActivityWithErrorToast(getActivity(), intent);
                    hideAndClearDialpad(false);
                }
            }
        }
    }

    public void clearDialpad() {
        mDigits.getText().clear();
    }

    private void handleDialButtonClickWithEmptyDigits() {
        if (phoneIsCdma() && isPhoneInUse()) {
            // TODO: Move this logic into services/Telephony
            //
            // This is really CDMA specific. On GSM is it possible
            // to be off hook and wanted to add a 3rd party using
            // the redial feature.
            startActivity(newFlashIntent());
        } else {
            if (!TextUtils.isEmpty(mLastNumberDialed)) {
                // Recall the last number dialed.
                mDigits.setText(mLastNumberDialed);

                // ...and move the cursor to the end of the digits string,
                // so you'll be able to delete digits using the Delete
                // button (just as if you had typed the number manually.)
                //
                // Note we use mDigits.getText().length() here, not
                // mLastNumberDialed.length(), since the EditText widget now
                // contains a *formatted* version of mLastNumberDialed (due to
                // mTextWatcher) and its length may have changed.
                mDigits.setSelection(mDigits.getText().length());
            } else {
                // There's no "last number dialed" or the
                // background query is still running. There's
                // nothing useful for the Dial button to do in
                // this case.  Note: with a soft dial button, this
                // can never happens since the dial button is
                // disabled under these conditons.
                playTone(ToneGenerator.TONE_PROP_NACK);
            }
        }
    }

    /**
     * Plays the specified tone for TONE_LENGTH_MS milliseconds.
     */
    private void playTone(int tone) {
        playTone(tone, TONE_LENGTH_MS);
    }

    /**
     * Play the specified tone for the specified milliseconds
     *
     * The tone is played locally, using the audio stream for phone calls.
     * Tones are played only if the "Audible touch tones" user preference
     * is checked, and are NOT played if the device is in silent mode.
     *
     * The tone length can be -1, meaning "keep playing the tone." If the caller does so, it should
     * call stopTone() afterward.
     *
     * @param tone a tone code from {@link ToneGenerator}
     * @param durationMs tone length.
     */
    private void playTone(int tone, int durationMs) {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager =
                (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
            || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, durationMs);
        }
    }

    /**
     * Stop the tone if it is played.
     */
    private void stopTone() {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "stopTone: mToneGenerator == null");
                return;
            }
            mToneGenerator.stopTone();
        }
    }

    /**
     * Brings up the "dialpad chooser" UI in place of the usual Dialer
     * elements (the textfield/button and the dialpad underneath).
     *
     * We show this UI if the user brings up the Dialer while a call is
     * already in progress, since there's a good chance we got here
     * accidentally (and the user really wanted the in-call dialpad instead).
     * So in this situation we display an intermediate UI that lets the user
     * explicitly choose between the in-call dialpad ("Use touch tone
     * keypad") and the regular Dialer ("Add call").  (Or, the option "Return
     * to call in progress" just goes back to the in-call UI with no dialpad
     * at all.)
     *
     * @param enabled If true, show the "dialpad chooser" instead
     *                of the regular Dialer UI
     */
    private void showDialpadChooser(boolean enabled) {
        if (getActivity() == null) {
            return;
        }
        // Check if onCreateView() is already called by checking one of View objects.
        if (!isLayoutReady()) {
            return;
        }

        if (enabled) {
            Log.i(TAG, "Showing dialpad chooser!");
            if (mDialpadView != null) {
                mDialpadView.setVisibility(View.GONE);
            }

            mFloatingActionButtonController.setVisible(false);
            mDialpadChooser.setVisibility(View.VISIBLE);

            // Instantiate the DialpadChooserAdapter and hook it up to the
            // ListView.  We do this only once.
            if (mDialpadChooserAdapter == null) {
                mDialpadChooserAdapter = new DialpadChooserAdapter(getActivity());
            }
            mDialpadChooser.setAdapter(mDialpadChooserAdapter);
        } else {
            Log.i(TAG, "Displaying normal Dialer UI.");
            if (mDialpadView != null) {
                mDialpadView.setVisibility(View.VISIBLE);
            } else {
                mDigits.setVisibility(View.VISIBLE);
            }

            mFloatingActionButtonController.setVisible(true);
            mDialpadChooser.setVisibility(View.GONE);
        }
    }

    /**
     * @return true if we're currently showing the "dialpad chooser" UI.
     */
    private boolean isDialpadChooserVisible() {
        return mDialpadChooser.getVisibility() == View.VISIBLE;
    }

    /**
     * Simple list adapter, binding to an icon + text label
     * for each item in the "dialpad chooser" list.
     */
    private static class DialpadChooserAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        // Simple struct for a single "choice" item.
        static class ChoiceItem {
            String text;
            Bitmap icon;
            int id;

            public ChoiceItem(String s, Bitmap b, int i) {
                text = s;
                icon = b;
                id = i;
            }
        }

        // IDs for the possible "choices":
        static final int DIALPAD_CHOICE_USE_DTMF_DIALPAD = 101;
        static final int DIALPAD_CHOICE_RETURN_TO_CALL = 102;
        static final int DIALPAD_CHOICE_ADD_NEW_CALL = 103;

        private static final int NUM_ITEMS = 3;
        private ChoiceItem mChoiceItems[] = new ChoiceItem[NUM_ITEMS];

        public DialpadChooserAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);

            // Initialize the possible choices.
            // TODO: could this be specified entirely in XML?

            // - "Use touch tone keypad"
            mChoiceItems[0] = new ChoiceItem(
                    context.getString(R.string.dialer_useDtmfDialpad),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_tt_keypad),
                    DIALPAD_CHOICE_USE_DTMF_DIALPAD);

            // - "Return to call in progress"
            mChoiceItems[1] = new ChoiceItem(
                    context.getString(R.string.dialer_returnToInCallScreen),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_current_call),
                    DIALPAD_CHOICE_RETURN_TO_CALL);

            // - "Add call"
            mChoiceItems[2] = new ChoiceItem(
                    context.getString(R.string.dialer_addAnotherCall),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_add_call),
                    DIALPAD_CHOICE_ADD_NEW_CALL);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        /**
         * Return the ChoiceItem for a given position.
         */
        @Override
        public Object getItem(int position) {
            return mChoiceItems[position];
        }

        /**
         * Return a unique ID for each possible choice.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a view for each row.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // When convertView is non-null, we can reuse it (there's no need
            // to reinflate it.)
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.dialpad_chooser_list_item, null);
            }

            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(mChoiceItems[position].text);

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageBitmap(mChoiceItems[position].icon);

            return convertView;
        }
    }

    /**
     * Handle clicks from the dialpad chooser.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        DialpadChooserAdapter.ChoiceItem item =
                (DialpadChooserAdapter.ChoiceItem) parent.getItemAtPosition(position);
        int itemId = item.id;
        switch (itemId) {
            case DialpadChooserAdapter.DIALPAD_CHOICE_USE_DTMF_DIALPAD:
                // Log.i(TAG, "DIALPAD_CHOICE_USE_DTMF_DIALPAD");
                // Fire off an intent to go back to the in-call UI
                // with the dialpad visible.
                returnToInCallScreen(true);
                break;

            case DialpadChooserAdapter.DIALPAD_CHOICE_RETURN_TO_CALL:
                // Log.i(TAG, "DIALPAD_CHOICE_RETURN_TO_CALL");
                // Fire off an intent to go back to the in-call UI
                // (with the dialpad hidden).
                returnToInCallScreen(false);
                break;

            case DialpadChooserAdapter.DIALPAD_CHOICE_ADD_NEW_CALL:
                // Log.i(TAG, "DIALPAD_CHOICE_ADD_NEW_CALL");
                // Ok, guess the user really did want to be here (in the
                // regular Dialer) after all.  Bring back the normal Dialer UI.
                showDialpadChooser(false);
                break;

            default:
                Log.w(TAG, "onItemClick: unexpected itemId: " + itemId);
                break;
        }
    }

    /**
     * Returns to the in-call UI (where there's presumably a call in
     * progress) in response to the user selecting "use touch tone keypad"
     * or "return to call" from the dialpad chooser.
     */
    private void returnToInCallScreen(boolean showDialpad) {
        getTelecomManager().showInCallScreen(showDialpad);

        // Finally, finish() ourselves so that we don't stay on the
        // activity stack.
        // Note that we do this whether or not the showCallScreenWithDialpad()
        // call above had any effect or not!  (That call is a no-op if the
        // phone is idle, which can happen if the current call ends while
        // the dialpad chooser is up.  In this case we can't show the
        // InCallScreen, and there's no point staying here in the Dialer,
        // so we just take the user back where he came from...)
        getActivity().finish();
    }

    /**
     * @return true if the phone is "in use", meaning that at least one line
     *              is active (ie. off hook or ringing or dialing, or on hold).
     */
    public boolean isPhoneInUse() {
        return getTelecomManager().isInCall();
    }

    /**
     * @return true if the phone is a CDMA phone type
     */
    private boolean phoneIsCdma() {
        return getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_contact: {
                final CharSequence digits = mDigits.getText();
                DialerUtils.startActivityWithErrorToast(getActivity(),
                        DialtactsActivity.getAddNumberToContactIntent(digits));
                return true;
            }
            case R.id.menu_2s_pause:
                updateDialString(PAUSE);
                return true;
            case R.id.menu_add_wait:
                updateDialString(WAIT);
                return true;
            case R.id.menu_send_message: {
                final CharSequence digits = mDigits.getText();
                final Intent smsIntent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts(ContactsUtils.SCHEME_SMSTO, digits.toString(), null));
                smsIntent.setComponent(mSmsPackageComponentName);
                DialerUtils.startActivityWithErrorToast(getActivity(), smsIntent);
                return true;
            }
            case R.id.menu_ip_call_by_slot1:
                ipCallBySlot(PhoneConstants.SUB1);
                return true;
            case R.id.menu_ip_call_by_slot2:
                ipCallBySlot(PhoneConstants.SUB2);
                return true;
            case R.id.menu_video_call:
                final String number = mDigits.getText().toString();
                if (CallUtil.isVideoEnabled(mContext)) {
                    //add support for ims video call;
                    final Intent intent = CallUtil.getVideoCallIntent(number,
                                (getActivity() instanceof DialtactsActivity ?
                                    ((DialtactsActivity) getActivity()).getCallOrigin() : null));
                    DialerUtils.startActivityWithErrorToast(getActivity(), intent);
                } else if (CallUtil.isCSVTEnabled()) {
                    getActivity().startActivity(CallUtil.getCSVTCallIntent(number));
                }
                hideAndClearDialpad(false);
                return true;
            case R.id.menu_add_to_4g_conference_call:
                getActivity().startActivity(CallUtil.getConferenceDialerIntent(
                        mDigits.getText().toString()));
                return true;
            default:
                return false;
        }
    }

    /**
     * Updates the dial string (mDigits) after inserting a Pause character (,)
     * or Wait character (;).
     */
    private void updateDialString(char newDigit) {
        if (newDigit != WAIT && newDigit != PAUSE) {
            throw new IllegalArgumentException(
                    "Not expected for anything other than PAUSE & WAIT");
        }

        int selectionStart;
        int selectionEnd;

        // SpannableStringBuilder editable_text = new SpannableStringBuilder(mDigits.getText());
        int anchor = mDigits.getSelectionStart();
        int point = mDigits.getSelectionEnd();

        selectionStart = Math.min(anchor, point);
        selectionEnd = Math.max(anchor, point);

        if (selectionStart == -1) {
            selectionStart = selectionEnd = mDigits.length();
        }

        Editable digits = mDigits.getText();

        if (canAddDigit(digits, selectionStart, selectionEnd, newDigit)) {
            digits.replace(selectionStart, selectionEnd, Character.toString(newDigit));

            if (selectionStart != selectionEnd) {
              // Unselect: back to a regular cursor, just pass the character inserted.
              mDigits.setSelection(selectionStart + 1);
            }
        }
    }

    /**
     * Update the enabledness of the "Dial" and "Backspace" buttons if applicable.
     */
    private void updateDeleteButtonEnabledState() {
        if (getActivity() == null) {
            return;
        }
        final boolean digitsNotEmpty = !isDigitsEmpty();
        mDelete.setEnabled(digitsNotEmpty);
    }

    /**
     * Handle transitions for the menu button depending on the state of the digits edit text.
     * Transition out when going from digits to no digits and transition in when the first digit
     * is pressed.
     * @param transitionIn True if transitioning in, False if transitioning out
     */
    private void updateMenuOverflowButton(boolean transitionIn) {
        mOverflowMenuButton = mDialpadView.getOverflowMenuButton();
        if (transitionIn) {
            AnimUtils.fadeIn(mOverflowMenuButton, AnimUtils.DEFAULT_DURATION);
        } else {
            AnimUtils.fadeOut(mOverflowMenuButton, AnimUtils.DEFAULT_DURATION);
        }
    }

    /**
     * Check if voicemail is enabled/accessible.
     *
     * @return true if voicemail is enabled and accessibly. Note that this can be false
     * "temporarily" after the app boot.
     * @see TelephonyManager#getVoiceMailNumber()
     */
    private boolean isVoicemailAvailable() {
        boolean promptEnabled = SubscriptionManager.isVoicePromptEnabled();
        if (promptEnabled) {
            //return hasVMNumber();
            return true;
        } else {
            int subId = SubscriptionManager.getDefaultVoiceSubId();
            try {
                return !TextUtils.isEmpty(getTelephonyManager().getVoiceMailNumber(subId));
            } catch (SecurityException se) {
                // Possibly no READ_PHONE_STATE privilege.
                Log.w(TAG, "SecurityException is thrown. Maybe privilege isn't sufficient.");
            }
        }
        return false;
    }

    private boolean hasVMNumber() {
        boolean hasVMNum = false;
        int phoneCount = getTelephonyManager().getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            try {
                int[] subId = SubscriptionManager.getSubId(i);
                hasVMNum = getTelephonyManager().getVoiceMailNumber(subId[0]) != null;
            } catch (SecurityException se) {
                // Possibly no READ_PHONE_STATE privilege.
                Log.e(TAG, "hasVMNumber: SecurityException, Maybe privilege isn't sufficient.");
            }
            if (hasVMNum) {
                break;
            }
        }
        return hasVMNum;
    }

    private void showNoVMNumberDialog() {
        String dialogTxt = getString(R.string.is_set_voicemail_number);
        final Activity thisActivity = getActivity();
        new AlertDialog.Builder(thisActivity)
            .setTitle(R.string.dialog_title)
            .setMessage(dialogTxt)
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // go to voicemail setting screen to set its number.
                        Intent intent = new Intent(ACTION_ADD_VOICEMAIL);
                        if (getTelephonyManager().isMultiSimEnabled()) {
                            int subId = SubscriptionManager.getDefaultVoiceSubId();
                            intent.setClassName("com.android.phone",
                                    "com.android.phone.MSimCallFeaturesSubSetting");
                            intent.putExtra(SUBSCRIPTION_KEY, subId);
                        } else {
                            intent.setClassName("com.android.phone",
                                    "com.android.phone.CallFeaturesSetting");
                        }
                        startActivity(intent);
                    }
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    /**
     * Returns true of the newDigit parameter can be added at the current selection
     * point, otherwise returns false.
     * Only prevents input of WAIT and PAUSE digits at an unsupported position.
     * Fails early if start == -1 or start is larger than end.
     */
    @VisibleForTesting
    /* package */ static boolean canAddDigit(CharSequence digits, int start, int end,
                                             char newDigit) {
        if(newDigit != WAIT && newDigit != PAUSE) {
            throw new IllegalArgumentException(
                    "Should not be called for anything other than PAUSE & WAIT");
        }

        // False if no selection, or selection is reversed (end < start)
        if (start == -1 || end < start) {
            return false;
        }

        // unsupported selection-out-of-bounds state
        if (start > digits.length() || end > digits.length()) return false;

        // Special digit cannot be the first digit
        if (start == 0) return false;

        if (newDigit == WAIT) {
            // preceding char is ';' (WAIT)
            if (digits.charAt(start - 1) == WAIT) return false;

            // next char is ';' (WAIT)
            if ((digits.length() > end) && (digits.charAt(end) == WAIT)) return false;
        }

        return true;
    }

    /**
     * @return true if the widget with the phone number digits is empty.
     */
    private boolean isDigitsEmpty() {
        return mDigits.length() == 0;
    }

    /**
     * @return true if the widget with the mRecipients is empty.
     */
    private boolean isRecipientEmpty() {
        return  (mRecipients == null) || (mRecipients.length() == 0);
    }

    /**
     * Starts the asyn query to get the last dialed/outgoing
     * number. When the background query finishes, mLastNumberDialed
     * is set to the last dialed number or an empty string if none
     * exists yet.
     */
    private void queryLastOutgoingCall() {
        mLastNumberDialed = EMPTY_NUMBER;
        CallLogAsync.GetLastOutgoingCallArgs lastCallArgs =
                new CallLogAsync.GetLastOutgoingCallArgs(
                    getActivity(),
                    new CallLogAsync.OnLastOutgoingCallComplete() {
                        @Override
                        public void lastOutgoingCall(String number) {
                            // TODO: Filter out emergency numbers if
                            // the carrier does not want redial for
                            // these.
                            // If the fragment has already been detached since the last time
                            // we called queryLastOutgoingCall in onResume there is no point
                            // doing anything here.
                            if (getActivity() == null) return;
                            mLastNumberDialed = number;
                            updateDeleteButtonEnabledState();
                        }
                    });
        mCallLog.getLastOutgoingCall(lastCallArgs);
    }

    private Intent newFlashIntent() {
        final Intent intent = CallUtil.getCallIntent(EMPTY_NUMBER);
        intent.putExtra(EXTRA_SEND_EMPTY_FLASH, true);
        return intent;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        final DialtactsActivity activity = (DialtactsActivity) getActivity();
        final DialpadView dialpadView = (DialpadView) getView().findViewById(R.id.dialpad_view);
        if (activity == null) return;
        if (!hidden && !isDialpadChooserVisible()) {
            if (mAnimate) {
                dialpadView.animateShow();
            }
            mFloatingActionButtonController.scaleIn(mAnimate ? mDialpadSlideInDuration : 0);
            activity.onDialpadShown();
            mDigits.requestFocus();
        }
        if (hidden && mAnimate) {
            mFloatingActionButtonController.scaleOut();
        }
    }

    public void setAnimate(boolean value) {
        mAnimate = value;
    }

    public boolean getAnimate() {
        return mAnimate;
    }

    public void setYFraction(float yFraction) {
        ((DialpadSlidingRelativeLayout) getView()).setYFraction(yFraction);
    }

    private void callSpeedNumber(int id) {
        int number;

        switch(id) {
            case R.id.two: number = 2; break;
            case R.id.three: number = 3; break;
            case R.id.four: number = 4; break;
            case R.id.five: number = 5; break;
            case R.id.six: number = 6; break;
            case R.id.seven: number = 7; break;
            case R.id.eight: number = 8; break;
            case R.id.nine: number = 9; break;
            default: return;
        }

        String phoneNumber = SpeedDialUtils.getNumber(getActivity(), number);
        if (phoneNumber == null) {
            showNoSpeedNumberDialog(number);
        } else {
            final DialtactsActivity activity = getActivity() instanceof DialtactsActivity
                    ? (DialtactsActivity) getActivity() : null;
            final Intent intent = CallUtil.getCallIntent(phoneNumber,
                    activity != null ? activity.getCallOrigin() : null);
            DialerUtils.startActivityWithErrorToast(getActivity(), intent);
            getActivity().finish();
        }
    }

    private void showNoSpeedNumberDialog(final int number) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.speed_dial_unassigned_dialog_title)
                .setMessage(getString(R.string.speed_dial_unassigned_dialog_message, number))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // go to speed dial setting screen to set speed dial number.
                        Intent intent = new Intent(getActivity(), SpeedDialListActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void listen() {
        for (int i = 0; i < mNumPhones; i++) {
            if (mPhoneStateListener[i] != null) {
                getTelephonyManager().listen(mPhoneStateListener[i],
                        PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    private void stopListen() {
        for (int i = 0; i < mNumPhones; i++) {
            if (mPhoneStateListener[i] != null) {
                getTelephonyManager().listen(mPhoneStateListener[i],
                        PhoneStateListener.LISTEN_NONE);
            }
        }
    }

    private PhoneStateListener getPhoneStateListener(int phoneId) {
        int[] subId = SubscriptionManager.getSubId(phoneId);

        if (subId == null) {
            return null;
        }

        PhoneStateListener phoneStateListener = new PhoneStateListener(subId[0]) {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if ((getActivity() != null) &&
                        (getTelephonyManager().getCallState() == TelephonyManager.CALL_STATE_IDLE)
                        && isDialpadChooserVisible()) {
                    showDialpadChooser(false);
                }
                if ((getActivity() != null)
                        && (getTelephonyManager().getCallState()
                                == TelephonyManager.CALL_STATE_IDLE)) {
                    ((HostInterface) getActivity()).setConferenceDialButtonVisibility(true);
                }
            }
        };
        return phoneStateListener;
    }
}