/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qualcomm.qti.voiceassistant.R;

/**
 * <p>This view is a card display to represent an item, it inflates the layout
 * {@link R.layout#layout_card layout_card}.</p>
 * <p>This view contains a title, some subtext, an image, an optional action button and some statuses
 * represented by a {@link StatusView}.</p>
 * <p>This application has two types of card which determines the content and images of a card:
 * {@link CardType#DEVICE DEVICE} or {@link CardType#ASSISTANT ASSISTANT}. When declaring a {@link Card} in the xml
 * file, the type is declared using the attribute <code>cardType</code> as defined in the attrs.xml file of the app
 * module.</p>
 * <p>The default type of a {@link Card} is {@link CardType#DEVICE DEVICE}.</p>
 */
public class Card extends FrameLayout implements View.OnClickListener {

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * A text view to display some complementary text about the item represented by this card.
     */
    private TextView mItemSubText;
    /**
     * A text view to display the item title.
     */
    private TextView mItemName;
    /**
     * An array which contains all the {@link StatusView} of the {@link Card}.
     */
    private StatusView[] mStatus;
    /**
     * An image to represent the item within the {@link Card}.
     */
    private ImageView mCardImage;
    /**
     * The optional action button to act on the item.
     */
    private View mAction;
    /**
     * The layout which displays all the information related to the item represented by this {@link Card}.
     */
    private View mLayoutInformation;
    /**
     * A button to select an item when the card has no item to represent.
     */
    private Button mButtonSelect;
    /**
     * The type of the {@link Card}.
     */
    private int mType = CardType.DEVICE;
    /**
     * The instance of the parent to interact with it as a listener.
     */
    private CardListener mListener;


    // ====== CONSTS CLASSES AND FIELDS ========================================================================

    /**
     * The list of status which can be displayed for each type of card.
     */
    /*package*/ class Status {
        /**
         * The number of status for the type {@link CardType#DEVICE DEVICE}.
         */
        private static final int DEVICE_STATUS_COUNT = 2;
        /**
         * The type {@link CardType#DEVICE DEVICE} has a connection status.
         */
        /*package*/ static final int DEVICE_CONNECTION_STATE = 0;
        /**
         * The type {@link CardType#DEVICE DEVICE} has an IVOR status.
         */
        /*package*/ static final int DEVICE_IVOR_STATUS = 1;

        /**
         * The number of status for the type {@link CardType#ASSISTANT ASSISTANT}.
         */
        private static final int ASSISTANT_STATUS_COUNT = 1;
        /**
         * The type {@link CardType#ASSISTANT ASSISTANT} has a state.
         */
        /*package*/ static final int ASSISTANT_STATE = 0;
    }

    /**
     * <p>All card types as defined in the attrs.xml file.</p>
     */
    private class CardType {
        static final int DEVICE = 0;
        static final int ASSISTANT = 1;
    }


    // ====== CONSTRUCTORS ========================================================================

    /* All mandatory constructors when implementing a View */

    public Card(@NonNull Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public Card(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public Card(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("unused")
    public Card(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr,
                @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    // ====== PUBLIC METHODS ========================================================================

    @Override //View.OnClickListener
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.bt_card_action || i == R.id.bt_card_select) {
            if (mType == CardType.DEVICE) {
                mListener.onSelectDevice();
            } else if (mType == CardType.ASSISTANT) {
                mListener.onSelectAssistant();
            }

        }
    }

    /**
     * <p>To refresh the card information with an item name and some sub information.</p>
     *
     * @param name
     *          The name to display for the item.
     * @param information
     *          The sub information to display for the item.
     */
    public void refreshCard(String name, String information) {
        mItemSubText.setText(information);
        mItemName.setText(name);
    }

    /**
     * <p>To refresh the card information of an item with an item name, some sub information, an image to represent
     * the item and if the image should keep its original colour.</p>
     * <p>The default image use in a card corresponds to the {@link CardType#DEVICE DEVICE} type and is
     * {@link R.drawable#ic_headphones_96dp ic_headphones_96dp}.</p>
     * <p>By default items are tinted with {@link R.color#blue_light_tone_65 blue_light_tone_65}.</p>
     *
     * @param name
     *          The name to display for the item.
     * @param information
     *          The sub information to display for the item.
     * @param drawable
     *          The drawable
     * @param coloured
     *          True to tint the image, false to keep the original colouring.
     */
    public void refreshCard(String name, String information, int drawable, boolean coloured) {
        mItemSubText.setText(information);
        mItemName.setText(name);
        mCardImage.setImageDrawable(getContext().getDrawable(drawable));
        if (coloured) {
            //noinspection deprecation
            mCardImage.setColorFilter(getResources().getColor(R.color.blue_light_tone_65),
                    android.graphics.PorterDuff.Mode.SRC_IN);
        }
        else {
            mCardImage.clearColorFilter();
        }

        mCardImage.setVisibility(GONE);
    }

    /**
     * <p>To update the view of a StatusView with the given {@link StatusView.StatusType StatusType} and text to
     * display.</p>
     *
     * @param status
     *          The status to update. If the {@link CardType} is:
     *          <ul>
     *              <li>{@link CardType#DEVICE DEVICE}: the status is one of
     *              {@link Status#DEVICE_CONNECTION_STATE DEVICE_CONNECTION_STATE} or
     *              {@link Status#DEVICE_IVOR_STATUS DEVICE_IVOR_STATUS}.</li>
     *              <li>{@link CardType#ASSISTANT ASSISTANT}: the status is
     *              {@link Status#ASSISTANT_STATE ASSISTANT_STATE}.</li>
     *          </ul>
     * @param type
     *          the {@link com.qualcomm.qti.voiceassistant.ui.StatusView.StatusType StatusType} to apply.
     * @param text
     *          the text to display.
     */
    public void refreshStatus(int status, @StatusView.StatusType int type, int text) {
        if (status >= 0 && status < mStatus.length) {
            mStatus[status].refreshValue(type, text);
        }
    }

    /**
     * <p>To show or hide the item information.</p>
     * <p>The item information should be displayed when an item is defined for the {@link Card}.</p>
     *
     * @param display
     *          True to display the item information, false to hide them and display the SELECT button.
     */
    public void displayInformation(boolean display) {
        mButtonSelect.setVisibility(display ? View.GONE : View.VISIBLE);
        mLayoutInformation.setVisibility(display ? View.VISIBLE : View.INVISIBLE);
        mAction.setVisibility(display && mType == CardType.ASSISTANT ? View.VISIBLE : View.INVISIBLE);
        mAction.setVisibility(GONE);
    }

    /**
     * <p>To set up a listener to be updated about events from the {@link Card}.</p>
     */
    public void setListener(CardListener listener) {
        mListener = listener;
    }


    // ====== PRIVATE METHODS ========================================================================

    /**
     * <p>Inflate the layout used for the {@link Card} and initialises all the view components.</p>
     *
     * @param context
     *          Context fo the application, used to inflate the layout.
     * @param attrs
     *          The attributes set up within the declaration of {@link Card} in an xml file.
     * @param defStyleAttr
     *          An attribute in the current theme that contains a reference to a style resource that supplies defaults
     *          values for the TypedArray. Can be 0 to not look for defaults.
     * @param defStyleRes
     *          A resource identifier of a style resource that supplies default values for the TypedArray, used only
     *          if defStyleAttr is 0 or can not be found in the theme. Can be 0 to not look for defaults.
     */
    private void init(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        // get the type defined in the XML file
        if (attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Card, defStyleAttr,
                    defStyleRes);
            try {
                mType = array.getInteger(R.styleable.Card_cardType, 0);
            } finally {
                array.recycle();
            }
        }

        // inflate the layout
        inflate(getContext(), R.layout.layout_card, this);

        // get views
        mItemSubText = findViewById(R.id.tv_card_item_subtext);
        mItemName = findViewById(R.id.tv_card_item_name);
        mAction = findViewById(R.id.bt_card_action);
        mAction.setOnClickListener(this);
        mButtonSelect = findViewById(R.id.bt_card_select);
        mButtonSelect.setOnClickListener(this);
        mButtonSelect.setVisibility(GONE);
        mLayoutInformation = findViewById(R.id.cl_card_information);
        mCardImage = findViewById(R.id.iv_card_image);

        TextView title = findViewById(R.id.tv_card_title);

        switch (mType) {
            case CardType.DEVICE:
                title.setText(R.string.device_card_title);
                createStatusViews(Status.DEVICE_STATUS_COUNT);
                mCardImage.setImageDrawable(context.getDrawable(R.drawable.ic_headphones_96dp));
                //noinspection deprecation
                mCardImage.setColorFilter(getResources().getColor(R.color.blue_light_tone_65),
                        android.graphics.PorterDuff.Mode.SRC_IN);
                mButtonSelect.setText(R.string.button_connect_device);
                mButtonSelect.setEnabled(false);
                break;

            case CardType.ASSISTANT:
                title.setText(R.string.assistant_card_title);
                createStatusViews(Status.ASSISTANT_STATUS_COUNT);
                mButtonSelect.setText(R.string.button_select_assistant);
                mButtonSelect.setEnabled(true);
                break;
        }
    }

    /**
     * <p>Initialises the {@link StatusView} array by creating as many {@link StatusView} for the {@link Card} as
     * specified within the parameters of the method.</p>
     *
     * @param count
     *          The number of StatusView to create.
     */
    private void createStatusViews(int count) {
        mStatus = new StatusView[count];
        LinearLayout layout = findViewById(R.id.ll_card_status_list);
        for (int i=0; i<count; i++) {
            StatusView statusView = new StatusView(getContext());
            layout.addView(statusView);
            mStatus[i] = statusView;
        }
    }


    // ====== INNER INTERFACES ========================================================================

    /**
     * The interface to implement to be updated of events from a {@link Card} item.
     */
    /*package*/ interface CardListener {
        /**
         * <p>This method is called when the user has pressed the "SELECT" button of the {@link Card} view when the
         * card type is {@link CardType#ASSISTANT ASSISTANT}.</p>
         */
        void onSelectAssistant();
        /**
         * <p>This method is called when the user has pressed the "SELECT" button of the {@link Card} view when the
         * card type is {@link CardType#DEVICE DEVICE}.</p>
         */
        void onSelectDevice();
    }

}