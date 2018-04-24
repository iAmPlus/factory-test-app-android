package com.iamplus.twsfactorytest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.csr.gaia.library.GaiaError;
import com.iamplus.earin.application.EarinApplication;
import com.iamplus.earin.communication.Manager;
import com.iamplus.earin.communication.cap.BluetoothGattStatus;
import com.iamplus.earin.communication.cap.CapCommunicator;
import com.iamplus.earin.communication.models.BatteryReading;
import com.iamplus.earin.util.SKUProfile;
import com.iamplus.earin.util.bluetooth.BleBroadcastUtil;
import com.iamplus.earin.util.bluetooth.BluetoothUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.iamplus.twsfactorytest.ButtonsMicrophoneTest.RequestPermissionCode;

/**
 * Created by abhay on 24-01-2018.
 */

public class ButtonsTestActivity extends Activity implements BleBroadcastUtil.BleEventListener, BleBroadcastUtil.OmegaCallEventListener, BleBroadcastUtil.BatteryUpdateListener {

    private static final String TAG = "ButtonsTestActivity";
    private boolean mBound;

    private TextView mStatusView;

    private TextView mGaiaConnectionTextView;
    private Context mContext;
    private ImageButton mGaiaRetryButton;
    private Switch mOmegaTestToggle;
    private AudioManager mAudioManager;
    private ButtonsMediaTest mButtonsMediaTest;
    private ButtonsMicrophoneTest mButtonsMicrophoneTest;
    private TextView mSerialNumberView;
    private TextView mMacAddressView;
    private TextView mUUIDView;
    private boolean mOmegaTestResult;
    private TextView mFWVersionView;
    private TextView mWritetoCSV;
    private ImageButton mShareButton;
    private File mCSVfile;
    private ComponentName mRemoteControlResponder;
    private TextView mBatteryView;
    private View mBatteryIndicator;
    private BluetoothAdapter mBluetoothAdapter;
    private String mVersion;
    private String mSku;
    private String mBatteryLevel = "NA";
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            log("Bound to music service");
            mBound = true;
            MusicService.LocalBinder binder = (MusicService.LocalBinder) iBinder;
            MusicService service = binder.getService();
            mButtonsMediaTest.setMusicController(service.getMusicController());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };
    private ProgressDialog mProgressDialog;

    private static void log(String s) {
        Log.d(TAG, s);
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.buttons_test_layout);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mContext = this;
        EarinApplication.setContext(mContext);

        ((TextView) findViewById(R.id.header)).setText("version: " + BuildConfig.VERSION_NAME);

        mStatusView = (TextView) findViewById(R.id.status);

        mSerialNumberView = ((TextView) findViewById(R.id.serialNo));
        mMacAddressView = ((TextView) findViewById(R.id.mac));
        mFWVersionView = ((TextView) findViewById(R.id.fwversion));
        mUUIDView = ((TextView) findViewById(R.id.uuid));
        mBatteryIndicator = findViewById(R.id.batteryview);
        mBatteryView = ((TextView) findViewById(R.id.battery));

        mOmegaTestToggle = (Switch) findViewById(R.id.testOmega);
        mOmegaTestToggle.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!mOmegaTestResult) {
                    mOmegaTestToggle.setChecked(false);
                }
                if (!b) {
                    mOmegaTestResult = b;
                }
            }
        });
        mButtonsMicrophoneTest = new ButtonsMicrophoneTest();
        mButtonsMediaTest = new ButtonsMediaTest();
        //mMediaTestCallBacks = mButtonsMediaTest;
        if (checkPermission()) {
            getFragmentManager().beginTransaction().add(R.id.mptestcontainer, mButtonsMicrophoneTest).commit();
            getFragmentManager().beginTransaction().add(R.id.mediatestcontainer, mButtonsMediaTest).commit();
        } else {
            requestPermission();
        }

        setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);

        mGaiaConnectionTextView = (TextView) findViewById(R.id.gaia_connection);
        mGaiaRetryButton = (ImageButton) findViewById(R.id.connect_retry);
        mGaiaRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptConnectingEarin();
            }
        });

        mWritetoCSV = (TextView) findViewById(R.id.writetocsv);
        mShareButton = (ImageButton) findViewById(R.id.sharetestfile);

        if (isExternalStorageAvailable() && !isExternalStorageReadOnly()) {
            mWritetoCSV.setEnabled(true);
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).mkdirs();
            mCSVfile = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "results.csv");
        }
        mWritetoCSV.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(mCSVfile, true);
                    if (mCSVfile.length() == 0) {
                        String header = "SKU,MAC Address,Firmware,Omega Test,Mic Test,Playback Test,Battery,Date,Test Status";
                        fileOutputStream.write(header.getBytes());
                        fileOutputStream.write(System.lineSeparator().getBytes());
                    }
                    fileOutputStream.write((SKUProfile.getSKUVersion(mSku) + ",").getBytes());
                    fileOutputStream.write((mMacAddressView.getText() + ",").getBytes());
                    fileOutputStream.write((mVersion + ",").getBytes());
                    fileOutputStream.write(mOmegaTestResult ? "true,".getBytes() : "false,".getBytes());
                    fileOutputStream.write(mButtonsMicrophoneTest.getMicrophoneTestResult() ? "true,".getBytes() : "false,".getBytes());
                    fileOutputStream.write(mButtonsMediaTest.getMediaTestResult() ? "true,".getBytes() : "false,".getBytes());
                    fileOutputStream.write((mBatteryView.getText() + ",").getBytes());

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = df.format(c.getTime());
                    fileOutputStream.write((formattedDate + ",").getBytes());

                    String result = (mOmegaTestResult &&
                            mButtonsMediaTest.getMediaTestResult() &&
                            mButtonsMicrophoneTest.getMicrophoneTestResult()) ? "PASS" : "FAIL";
                    fileOutputStream.write(result.getBytes());

                    fileOutputStream.write(System.lineSeparator().getBytes());

                    Toast.makeText(mContext, "Export Successfull to Documents/results.csv Result: " + result, Toast.LENGTH_SHORT).show();
                    reset();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCSVfile.length() != 0) {
                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                    intentShareFile.setType("application/pdf");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mCSVfile));
                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                            "Sharing Buttons test results file...");
                    intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File...");
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                }
            }
        });
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(),
                RemoteControlReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(
                mRemoteControlResponder);
        if (!mBluetoothAdapter.isEnabled()) {
            BluetoothUtil.getInstance(EarinApplication.getContext());
            //bluetoothAdapter.enable();
        } else {
            Manager.getSharedManager();
            BluetoothUtil.getInstance(EarinApplication.getContext())
                    .getBluetoothHeadset(EarinApplication.getContext());

        }
        BleBroadcastUtil.getInstance(this).setOmegaCallEventListener(this);
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "onConnect: tws");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setMessage("Please wait...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.show();
                mGaiaConnectionTextView.setText("Mini Buttons connected");
            }
        });
        Manager manager = Manager.getSharedManager();
        CapCommunicator capCommunicator = Manager.getSharedManager().getCapCommunicationController().getConnectedCommunicator();
        manager.enqueRequest("getSKU", new Runnable() {
            @Override
            public void run() {
                try {
                    mVersion = capCommunicator.getVersion();
                    mSku = capCommunicator.getSKU("1");
                    Log.d(TAG, "run: sku:" + mSku + " " + mVersion);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        if (mSku == null) {
                            mSerialNumberView.setText("Unable to get SKU");
                            Toast.makeText(mContext, "Unable to get SKU, you might need to update Mini Buttons", Toast.LENGTH_SHORT).show();
                        } else if (!SKUProfile.validateSKU(mSku)) {
                            mSerialNumberView.setText(mSku.substring(2, mSku.length()) + " (" + SKUProfile.earinVersions[0] + ")");
                            Toast.makeText(mContext, "SKU retrived is not valid", Toast.LENGTH_SHORT).show();
                        } else {
                            mSerialNumberView.setText(SKUProfile.getSKUVersion(mSku) + " (" + SKUProfile.getEarinVersion(mSku) + ")");
                        }
                        mMacAddressView.setText(BluetoothUtil.getInstance(EarinApplication.getContext())
                                .getCurrentDevice().getAddress());
                        if(mVersion == null) {
                            mVersion = "Unable to get version";
                        } else {
                            mVersion = mVersion.replaceAll(",", " |");
                        }
                        mFWVersionView.setText(mVersion);
                    }
                });
            }
        });
    }

    @Override
    public void onDisconnect(boolean isCompletingUpgrade, BluetoothGattStatus status) {
        Log.d(TAG, "onDisconnect: tws");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reset();
                mGaiaConnectionTextView.setText("Mini Buttons disconnected");
            }
        });

    }

    private void reset() {
        mSerialNumberView.setText("");
        mUUIDView.setText("");
        mMacAddressView.setText("");
        mFWVersionView.setText("");
        mOmegaTestToggle.setChecked(false);
        mButtonsMediaTest.resetToggle();
        mButtonsMicrophoneTest.resetToggle();
    }

    @Override
    public void omegaCallEvent() {
        Log.d(TAG, "omegaCallEvent: tws");
        mOmegaTestResult = true;
        mOmegaTestToggle.setChecked(true);
    }

    @Override
    public void onBatteryEvent(BatteryReading batteryReading) {
        mBatteryView.setText(batteryReading.getPercentage() + "%");
    }

    private void handleOnError(GaiaError error) {
        switch (error.getType()) {
            case CONNECTION_FAILED:
                //mGaiaConnectionTextView.setText(R.string.disconnected);
                break;
            default:
                // mGaiaConnectionTextView.setText(R.string.disconnected);
                Log.w(TAG, "handleOnError: " + error.getStringException());
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindToMusicService();
        BleBroadcastUtil.getInstance(this).addBleEventListener(this);
        BleBroadcastUtil.getInstance(this).setBatteryUpdateListener(this);
        attemptConnectingEarin();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
        }
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void bindToMusicService() {
        Intent i = new Intent(this, MusicService.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        getFragmentManager().beginTransaction().add(R.id.mptestcontainer, mButtonsMicrophoneTest).commit();
                        getFragmentManager().beginTransaction().add(R.id.mediatestcontainer, mButtonsMediaTest).commit();
                        Toast.makeText(this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent: " + event.getKeyCode());
        if (event.getKeyCode() == KeyEvent.KEYCODE_CALL) {
            Toast.makeText(this, "CALLING!", Toast.LENGTH_LONG).show();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void attemptConnectingEarin() {
        if (BleBroadcastUtil.getInstance(this).isConnected()) {
            onConnect();
        } else {
            Manager.getSharedManager();
            if (mBluetoothAdapter.isEnabled() &&
                    !Manager.getSharedManager().getCapCommunicationController().isConnectingToDevice()) {
                Log.v(TAG, "Try to connect!");
                BluetoothUtil bluetoothUtil = BluetoothUtil.getInstance(EarinApplication.getContext());
                if (bluetoothUtil.getCurrentDevice() != null) {
                    Manager manager = Manager.getSharedManager();
                    Log.d(TAG, "attemptConnectingEarin: bluetoothUtil.getCurrentDevice() != null");
                    manager.getCapCommunicationController().connectToDevice(bluetoothUtil.getCurrentDevice());
                }
            } else {
                Log.v(TAG, "Trying to connect or bluetooth off");
            }

        }

    }

    public class RemoteControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event.getAction() != KeyEvent.ACTION_DOWN) return;

            Log.d(TAG, "onReceive: " + event.getKeyCode());
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    // stop music
                    break;
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    // pause music
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    // next track
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    // previous track
                    break;
            }
        }
    }

    public class MediaButtonIntentReceiver extends BroadcastReceiver {

        public MediaButtonIntentReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            Log.i(TAG, intentAction.toString() + " happended");
            if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                Log.i(TAG, "no media button information");
                return;
            }
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                Log.i(TAG, "no keypress");
                return;
            }
        }
    }

}
