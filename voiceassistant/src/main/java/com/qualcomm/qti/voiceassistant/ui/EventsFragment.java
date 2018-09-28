/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qualcomm.qti.voiceassistant.R;

/**
 * <p>This fragment is used to display a list of events about the assistant background process.</p>
 * <p>This fragment inflates the layout {@link R.layout#fragment_events fragment_events}.</p>
 * <p>This view only contains a text view in which the events are displayed.</p>
 */
public class EventsFragment extends Fragment {

    // ====== PRIVATE FIELDS ========================================================================

    /**
     * The text view to display the events.
     */
    private TextView mTVEvents;
    /**
     * To trigger requests from this fragment.
     */
    private EventsFragmentListener mListener;
    /**
     * To keep a copy of the text view content when this fragment is regenerated.
     */
    private String mEvents = "";


    // ====== STATIC METHODS ========================================================================

    /**
     * Returns a new instance of this fragment.
     */
    public static EventsFragment newInstance() {
        return new EventsFragment();
    }


    // ====== CONSTRUCTOR ========================================================================

    // default empty constructor, required for Fragment.
    public EventsFragment() {
    }


    // ====== FRAGMENT METHODS ========================================================================

    @Override // Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof EventsFragmentListener) {
            this.mListener = (EventsFragmentListener) context;
            scrollDown();
        }
    }

    @Override // Fragment
    public void onResume() {
        super.onResume();
        mTVEvents.setText(mEvents);
        scrollDown();
    }

    @Override // Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_events, container, false);
        mTVEvents = rootView.findViewById(R.id.tv_events);
        return rootView;
    }

    // ====== PUBLIC METHODS ========================================================================

    /**
     * <p>To add an event within the list of displayed events.</p>
     *
     * @param event
     *          The event to add.
     */
    public void addEvent(String event) {
        if (mEvents == null || mEvents.length() == 0) {
            setEvents(event);
            return;
        }
        event = "\n" + event;
        mEvents += event;

        if (mTVEvents != null) {
            mTVEvents.append(event);
        }

        scrollDown();
    }


    // ====== PRIVATE METHODS ========================================================================

    /**
     * <p>Sets up the events to the given parameters.</p>
     *
     * @param events
     *          The list of events to display.
     */
    private void setEvents(String events) {
        mEvents = events;
        if (mTVEvents != null) {
            mTVEvents.setText(events);
        }
    }

    /**
     * <p>This method scrolls down to the bottom of the screen by asking to the parent to do so.</p>
     */
    private void scrollDown() {
        if (mListener != null) {
            int height = mTVEvents == null ? 0 : mTVEvents.getMeasuredHeight();
            mListener.scrollParentDown(height);
        }
    }


    // ====== INTERFACE ========================================================================

    /**
     * The interface which listens for request from the fragment.
     */
    public interface EventsFragmentListener {
        /**
         * <p>Asks the parent to scroll down its view in order to display the latest line of the events.</p>
         *
         * @param height
         *          The height to which the parent should scroll down.
         */
        void scrollParentDown(final int height);
    }
}
