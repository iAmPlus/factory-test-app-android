/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.qualcomm.qti.voiceassistant.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 *
 *
 *
 *
 *
 * <p>This view is used to represent a status of an item on a {@link Card}.</p>
 * <p>It inflates the layout {@link R.layout#layout_status layout_status}.</p>
 * <p>This view contains a text, an icon and a progressbar.</p>
 */
public class StatusView extends FrameLayout {

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * <p>The icon of the view: its value depends on the {@link StatusType}.</p>
     */
    private ImageView mIcon;
    /**
     * <p>The status text.</p>
     */
    private TextView mText;
    /**
     * <p>The progressbar of the view: its visibility depends on the {@link StatusType}.</p>
     */
    private View mProgressBar;


    // ====== ENUMS ========================================================================

    /**
     * <p>Depending on the type of status this view adjusts its look. This enumeration lists all the types.</p>
     */
    @IntDef({ StatusType.SUCCESS, StatusType.WARNING, StatusType.ERROR, StatusType.PROGRESS, StatusType.EMPTY })
    @Retention(RetentionPolicy.SOURCE)
    @interface StatusType {
        /**
         * <p>For the SUCCESS type of status, the view adjusts the following:
         * <ul>
         *     <li>Icon: {@link R.drawable#ic_check_16dp ic_check_16dp}.</li>
         *     <li>Icon colour: {@link R.color#grass_52 grass_52}.</li>
         *     <li>Progress bar: hidden.</li>
         * </ul></p>
         */
        int SUCCESS = 0;
        /**
         * <p>For the WARNING type of status, the view adjusts the following:
         * <ul>
         *     <li>Icon: {@link R.drawable#ic_warning_16dp ic_warning_16dp}.</li>
         *     <li>Icon colour: {@link R.color#orange_54 orange_54}.</li>
         *     <li>Progress bar: hidden.</li>
         * </ul></p>
         */
        int WARNING = 1;
        /**
         * <p>For the ERROR type of status, the view adjusts the following:
         * <ul>
         *     <li>Icon: {@link R.drawable#ic_error_16dp ic_error_16dp}.</li>
         *     <li>Icon colour: {@link R.color#red_54 red_54}.</li>
         *     <li>Progress bar: hidden.</li>
         * </ul></p>
         */
        int ERROR = 2;
        /**
         * <p>For the PROGRESS type of status, the view adjusts the following:
         * <ul>
         *     <li>Icon: hidden.</li>
         *     <li>Progress bar: visible.</li>
         * </ul></p>
         */
        int PROGRESS = 3;
        /**
         * <p>For the EMPTY type of status, the view adjusts the following:
         * <ul>
         *     <li>Icon: hidden.</li>
         *     <li>Progress bar: hidden.</li>
         * </ul></p>
         */
        int EMPTY = 4;
    }


    // ====== CONSTRUCTORS ========================================================================

    /* All mandatory constructors when implementing a View */

    public StatusView(@NonNull Context context) {
        super(context);
        init();
    }

    public StatusView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    public StatusView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr,
                      @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    // ====== PUBLIC METHODS ========================================================================


    /**
     * <p>This method refreshes the content of this view with the given parameters.</p>
     *
     * @param type
     *          The type of status as one of {@link StatusType}.
     * @param text
     *          The text to display.
     */
    public void refreshValue(@StatusType int type, int text) {
        int colour = R.color.textColorPrimary;
        int drawable = R.drawable.ic_check_16dp;

        switch (type) {
            case StatusType.EMPTY:
                mProgressBar.setVisibility(GONE);
                mIcon.setVisibility(INVISIBLE);
                break;
            case StatusType.ERROR:
                mProgressBar.setVisibility(GONE);
                mIcon.setVisibility(VISIBLE);
                colour = R.color.red_54;
                drawable = R.drawable.ic_error_16dp;
                break;
            case StatusType.PROGRESS:
                mProgressBar.setVisibility(VISIBLE);
                mIcon.setVisibility(GONE);
                break;
            case StatusType.SUCCESS:
                mProgressBar.setVisibility(GONE);
                mIcon.setVisibility(VISIBLE);
                colour = R.color.grass_52;
                drawable = R.drawable.ic_check_16dp;
                break;
            case StatusType.WARNING:
                mProgressBar.setVisibility(GONE);
                mIcon.setVisibility(VISIBLE);
                colour = R.color.orange_54;
                drawable = R.drawable.ic_warning_16dp;
                break;
        }

        mText.setText(text);
        //noinspection deprecation
        mText.setTextColor(getResources().getColor(R.color.textColorPrimary));

        if (type != StatusType.PROGRESS && type != StatusType.EMPTY) {
            mIcon.setImageDrawable(getContext().getDrawable(drawable));
            //noinspection deprecation
            mIcon.setColorFilter(getResources().getColor(colour), android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }


    // ====== PRIVATE METHODS ========================================================================

    /**
     * <p>Inflate the layout used for the {@link Card} and initialises all the view components.</p>
     */
    private void init() {
        // inflate the layout
        inflate(getContext(), R.layout.layout_status, this);

        // get views
        mIcon = findViewById(R.id.iv_status_image);
        mText = findViewById(R.id.tv_status_text);
        mProgressBar = findViewById(R.id.progress_bar_status);
    }
}
