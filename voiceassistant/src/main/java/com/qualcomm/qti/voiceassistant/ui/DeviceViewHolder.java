/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.qualcomm.qti.voiceassistant.R;

/**
 * <p>This view holder represents a device item display. It is used in a devices list to display and update the
 * information of a device for the layout {@link R.layout#list_devices_item list_devices_item}.</p>
 */
class DeviceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    /**
     * The text view to display the device name.
     */
    private final TextView textViewDeviceName;
    /**
     * The text view to display the device Bluetooth address.
     */
    private final TextView textViewDeviceAddress;
    /**
     * The radio button to select the item.
     */
    private final RadioButton radioButton;
    /**
     * The instance of the parent to interact with it as a listener.
     */
    private final DeviceViewHolderListener mListener;

    /**
     * <p>The constructor which instantiates the views to use for this holder.</p>
     *
     * @param rowView
     *              The main view which contains all the views this holder should use.
     * @param listener
     *          The instance of the parent to interact with it as a listener.
     */
    DeviceViewHolder(View rowView, DeviceViewHolderListener listener) {
        super(rowView);
        textViewDeviceName = rowView.findViewById(R.id.tv_device_name);
        textViewDeviceAddress = rowView.findViewById(R.id.tv_device_address);
        radioButton = rowView.findViewById(R.id.rb_button);
        radioButton.setOnClickListener(this);
        mListener = listener;
        itemView.setOnClickListener(this);
    }

    @Override // View.OnClickListener
    public void onClick(View v) {
        mListener.onClickItem(this.getAdapterPosition());
    }

    /**
     * <p>This method refreshes all the values displayed in the corresponding view which shows all information
     * related to a Bluetooth device.</p>
     *
     * @param name
     *          The name which has to be displayed.
     * @param address
     *          The Bluetooth address which has to be displayed.
     * @param isSelected
     *          To know if the device should be displayed as selected.
     */
    void refreshValues(String name, String address,  boolean isSelected) {
        // display name
        textViewDeviceName.setText(name);
        // display bluetooth address
        textViewDeviceAddress.setText(address);

        // checked the radio button if device is selected
        radioButton.setChecked(isSelected);
    }

    /**
     * The interface to dispatch events happening within the view managed by this holder.
     */
    interface DeviceViewHolderListener {
        /**
         * This method is called when the user clicks on an item to select or deselect it.
         *
         * @param position
         *              The position of the item in the list.
         */
        void onClickItem(int position);
    }
}
