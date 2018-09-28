/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.qualcomm.qti.voiceassistant.R;

/**
 * <p>This view is used to represent the session state of the assistant background process.</p>
 * <p>It looks like an information bar within the user interface.</p>
 * <p>It inflates the layout {@link R.layout#layout_session_status layout_session_status}.</p>
 * <p>This view contains a text, a progressbar and has a background.</p>
 */
public class SessionView extends FrameLayout {

    // ====== PRIVATE FIELDS ========================================================================
    /**
     * The text to give the current state of the assistant session.
     */
    private TextView mText;
    /**
     * The progress bar of the view.
     */
    private View mProgressBar;
    /**
     * The background view component of this view.
     */
    private View mBackground;


    // ====== CONSTRUCTORS ========================================================================

    /* All mandatory constructors when implementing a View */

    public SessionView(@NonNull Context context) {
        super(context);
        init();
    }

    public SessionView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SessionView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    public SessionView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr,
                       @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    // ====== PUBLIC METHODS ========================================================================

    /**
     * <p>This method refreshes the content of this view with the given parameters.</p>
     *
     * @param colour
     *          The colour to set up the background with.
     * @param progress
     *          True to show the progress bar, false to hide it.
     * @param text
     *          The text to display.
     */
    public void refreshValue(int colour, boolean progress, int text) {
        //noinspection deprecation
        mBackground.setBackgroundColor(getResources().getColor(colour));
        mProgressBar.setVisibility(progress ? VISIBLE : INVISIBLE);
        mText.setText(text);
    }


    // ====== PRIVATE METHODS ========================================================================

    /**
     * <p>Inflate the layout used for the {@link Card} and initialises all the view components.</p>
     */
    private void init() {
        // inflate the layout
        inflate(getContext(), R.layout.layout_session_status, this);

        // get views
        mText = findViewById(R.id.tv_session_status);
        mProgressBar = findViewById(R.id.pb_session_status);
        mBackground = findViewById(R.id.ll_session_status);
    }

}
