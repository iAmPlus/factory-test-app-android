/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

package com.qualcomm.qti.voiceassistant.ui;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qualcomm.qti.voiceassistant.R;

import java.util.List;

/**
 * <p>This class manages the data set for a devices list.</p>
 */
public class DevicesListAdapter extends RecyclerView.Adapter<DeviceViewHolder>
        implements DeviceViewHolder.DeviceViewHolderListener {

    /**
     * The data managed by this adapter.
     */
    private final List<BluetoothDevice> mDevices;
    /**
     * To know which device is selected.
     */
    private int mSelectedItem = 0; // always has a default item selected

    /**
     * Default constructor to build a new instance of this adapter.
     */
    public DevicesListAdapter(List<BluetoothDevice> devices, int selected) {
        mDevices = devices;
        mSelectedItem = (selected > 0 && selected < mDevices.size()) ? selected : 0;
    }

    @Override // RecyclerView.Adapter<DeviceViewHolder>
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_devices_item, parent, false);
        return new DeviceViewHolder(view, this);
    }

    @Override // RecyclerView.Adapter<DeviceViewHolder>
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        // we define the content of this view depending on the data set of this adapter.
        BluetoothDevice device = mDevices.get(position);
        String deviceName = device.getName();
        deviceName = (deviceName == null || deviceName.length() < 1) ? "Unknown" : deviceName;
        boolean isSelected = mSelectedItem == position;

        // fill data
        holder.refreshValues(deviceName, device.getAddress(), isSelected);
    }

    @Override // RecyclerView.Adapter<DeviceViewHolder>
    public int getItemCount() {
        return mDevices.size();
    }

    @Override // DeviceViewHolder.IDeviceViewHolder
    public void onClickItem(int position) {
        if (mSelectedItem != position) {
            notifyItemChanged(mSelectedItem);
        }
        mSelectedItem = position;
        notifyItemChanged(position);
    }

    /**
     * <p>To get the {@link BluetoothDevice BluetoothDevice} which is selected in the list.</p>
     *
     * @return the device currently selected within the list.
     */
    public BluetoothDevice getSelectedDevice() {
        if (hasSelection()) {
            return mDevices.get(mSelectedItem);
        }
        else {
            return null;
        }
    }

    /**
     * This method allows to know if the view has a selected item.
     *
     * @return true if the view has a selected item and false if none of the items is selected.
     */
    private boolean hasSelection() {
        return mSelectedItem >= 0 && mSelectedItem < mDevices.size();
    }
}