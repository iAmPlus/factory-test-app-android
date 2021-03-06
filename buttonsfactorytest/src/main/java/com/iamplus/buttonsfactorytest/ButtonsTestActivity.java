package com.iamplus.buttonsfactorytest;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.Notification;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.csr.gaia.library.Gaia;
import com.csr.gaia.library.GaiaError;
import com.csr.gaia.library.GaiaPacket;
import com.csr.gaiacontrol.Callback;
import com.csr.gaiacontrol.Controller;
import com.csr.gaiacontrol.Events;
import com.csr.gaiacontrol.SimpleCallback;
import com.iamplus.systemupdater.Config;
import com.qualcomm.qti.voiceassistant.activities.MainActivity;
import com.qualcomm.qti.voiceassistant.ui.StatusFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.iamplus.buttonsfactorytest.ButtonsMicrophoneTest.RequestPermissionCode;

/**
 * Created by abhay on 24-01-2018.
 */

public class ButtonsTestActivity extends Activity implements View.OnClickListener, Application.OnProvideAssistDataListener {

    private static final String TAG = "ButtonsTestActivity";
    private boolean mBound;

    private Controller mController;
    private TextView mGaiaConnectionTextView;
    private Context mContext;
    private ImageButton mGaiaRetryButton;
    private Switch mOmegaTestToggle;
    private Switch mVATestToggle;
    private AudioManager mAudioManager;
    private ButtonsMediaTest mButtonsMediaTest;
    private StatusFragment mStatusFragment;
    private ButtonsMicrophoneTest mButtonsMicrophoneTest;
    private TextView mSerialNumberView;
    private TextView mMacAddressView;
    private TextView mUUIDView;
    private Button mEQButton;
    private boolean mOmegaTestResult;
    private boolean mVATestResult;
    private TextView mFWVersionView;
    private Button mWritetoCSV;
    private ImageButton mShareButton;
    private File mCSVfile;
    private ComponentName mRemoteControlResponder;
    private TextView mBatteryView;
    private View mOmegaView;
    private View mVAView;
    private View mVoiceAView;
    private TextView mHeaderView;
    private View mBatteryIndicator;
    private boolean mGaiaConnected;
    private Button mUpdateButton;
    private String mBatteryLevel = "NA";
    private View mNonOmegaView;
    private Callback mCallback = new SimpleCallback() {
        @Override
        public void onConnected() {
            Log.v(TAG, " onConnected ");
            if (mGaiaConnected) {
                return;
            }
            mGaiaConnected = true;
            mGaiaConnectionTextView.setText("Connected");
            mController.getSensoryState();
            mController.getVoiceAssistanntState();
            if (mController.getBluetoothDevice() != null) {
                mMacAddressView.setText(mController.getBluetoothDevice().getAddress());
            }
            Log.d(TAG, "onConnected: getSerialNumber");
            mController.getSerialNumber();
            mController.getUUID();
            mController.getAppVersion();
            mController.getBatteryLevel();
            mController.registerNotification(Gaia.EventId.USER_ACTION);
        }

        @Override
        public void onDisconnected() {
            mGaiaConnected = false;
            mBatteryIndicator.setVisibility(View.GONE);
            mGaiaConnectionTextView.setText("Disconnected");
            mSerialNumberView.setText("");
            mUUIDView.setText("");
            mMacAddressView.setText("");
            mFWVersionView.setText("");
            mNonOmegaView.setVisibility(View.GONE);
            reset();
            mController.cancelNotification(Gaia.EventId.USER_ACTION);
        }

        @Override
        public void handleNotification(GaiaPacket packet) {
            Gaia.EventId event = packet.getEvent();
            Log.d(TAG, "handleNotification: " + Integer.toHexString(packet.getPayload()[2] & 0xff).toUpperCase());
            switch (event) {
                case USER_ACTION:
                    if (Integer.toHexString(packet.getPayload()[2] & 0xff).toUpperCase().equals(Events.GAIA_USER1)||
                            Integer.toHexString(packet.getPayload()[2] & 0xff).toUpperCase().equals(Events.GAIA_USER31)) {
                        mOmegaTestResult = true;
                        mVATestResult = true;
                        mOmegaTestToggle.setChecked(true);
                        mVATestToggle.setChecked(true);
                    } else if(Integer.toHexString(packet.getPayload()[2] & 0xff).toUpperCase().equals(Events.GAIA_USER2)) {
                        mVATestToggle.setChecked(false);
                        mStatusFragment.voiceEnd();
                    }
                    break;

            }
        }


        @Override
        public void onGetSerialNumber(String sn) {
            mSerialNumberView.setText(sn);
        }

        @Override
        public void onGetUUID(String uuid) {
            mUUIDView.setText(uuid);
        }

        @Override
        public void onGetAppVersion(String version) {
            mFWVersionView.setText(version);

            if(BuildConfig.isInternal) {
                mOmegaView.setVisibility(View.GONE);
                mVAView.setVisibility(View.GONE);
                mVoiceAView.setVisibility(View.GONE);
                if(version.contains("4.")) {
                    mVAView.setVisibility(View.VISIBLE);
                } else if(version.contains("3.")) {
                    mOmegaView.setVisibility(View.VISIBLE);
                } else if(version.contains("5.")) {
                    mVoiceAView.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onGetBatteryLevel(int level) {
            mBatteryLevel = String.valueOf(level);
            mBatteryView.setText(com.csr.gaiacontrol.utils.Utils.getBatteryPercentage(level));
            mBatteryIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(GaiaError error) {
            handleOnError(error);
        }
    };

    private void nonOmegaButtons() {
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            log("Bound to music service");
            mBound = true;
            MusicService.LocalBinder binder = (MusicService.LocalBinder) iBinder;
            MusicService service = binder.getService();
            if(!mButtonsMediaTest.isDetached() && mButtonsMediaTest.isAdded())
                mButtonsMediaTest.setMusicController(service.getMusicController());
            if(!mStatusFragment.isDetached() && mStatusFragment.isAdded())
                mStatusFragment.setMusicController(service.getMusicController());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };

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

        mContext = this;
        getApplication().registerOnProvideAssistDataListener(this);
        mSerialNumberView = ((TextView) findViewById(R.id.serialNo));
        mMacAddressView = ((TextView) findViewById(R.id.mac));
        mFWVersionView = ((TextView) findViewById(R.id.fwversion));
        mUUIDView = ((TextView) findViewById(R.id.uuid));
        mBatteryIndicator = findViewById(R.id.batteryview);
        mBatteryView = ((TextView) findViewById(R.id.battery));

        mOmegaView = findViewById(R.id.omegaView);
        mOmegaView.setVisibility(View.VISIBLE);
        mVAView = findViewById(R.id.vaview);
        mVoiceAView = findViewById(R.id.voiceassistant);
        mHeaderView = findViewById(R.id.header);
        mNonOmegaView = findViewById(R.id.nonOmegaView);


        findViewById(R.id.radioProductionFota).setOnClickListener(this);
        RadioButton qaButton = findViewById(R.id.radioQaFota);
        qaButton.setOnClickListener(this);
        qaButton.toggle();
        Config.setChannel(mContext, Config.QA);
        findViewById(R.id.radioDevFota).setOnClickListener(this);

        if(BuildConfig.isInternal) {
            mHeaderView.setText("Buttons Internal Test(Internal)");
            mNonOmegaView.setVisibility(View.VISIBLE);
        } else {
            mNonOmegaView.setVisibility(View.GONE);
            mHeaderView.setText("Buttons Factory Test");
        }
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

        mVATestToggle = (Switch) findViewById(R.id.testva);
        mVATestToggle.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!mVATestResult) {
                    mOmegaTestToggle.setChecked(false);
                }
                if (!b) {
                    mVATestResult = b;
                }
            }
        });

        mButtonsMicrophoneTest = new ButtonsMicrophoneTest();
        mButtonsMediaTest = new ButtonsMediaTest();
        mStatusFragment = new StatusFragment();
        mStatusFragment.init(this);
        //mMediaTestCallBacks = mButtonsMediaTest;
        if (checkPermission()) {
            getFragmentManager().beginTransaction().add(R.id.mptestcontainer, mButtonsMicrophoneTest).commit();
            getFragmentManager().beginTransaction().add(R.id.mediatestcontainer, mButtonsMediaTest).commit();
            getFragmentManager().beginTransaction().add(R.id.voiceassistant, mStatusFragment).commit();

        } else {
            requestPermission();
        }

        setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);

        mController = Controller.getInstance();
        mGaiaConnectionTextView = (TextView) findViewById(R.id.gaia_connection);
        mGaiaRetryButton = (ImageButton) findViewById(R.id.connect_retry);
        mGaiaRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.establishGAIAConnection();
                if(mStatusFragment != null) {
                    mStatusFragment.forceReset();
                }
            }
        });


        mEQButton = ((Button) findViewById(R.id.eq));
        mEQButton.setVisibility(View.GONE);
        mEQButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                Intent intentEqualizer = new Intent(mContext, MainActivity.class);
                startActivity(intentEqualizer);
            }
        });
        mWritetoCSV = (Button) findViewById(R.id.writetocsv);
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
                        String header = "Serial No,MAC No,UUID,FW version,Voice Assistant Test,Mic Test,Playback Test,Battery level(mv),Date,Test Status";
                        fileOutputStream.write(header.getBytes());
                        fileOutputStream.write(System.lineSeparator().getBytes());
                    }
                    fileOutputStream.write((mSerialNumberView.getText().toString() + ",").getBytes());
                    fileOutputStream.write((mMacAddressView.getText().toString() + ",").getBytes());
                    fileOutputStream.write((mUUIDView.getText().toString() + ",").getBytes());
                    fileOutputStream.write((mFWVersionView.getText().toString() + ",").getBytes());
                    if (mSerialNumberView.getText().toString().contains("3.")) {
                        fileOutputStream.write(mOmegaTestResult ? "true,".getBytes() : "false,".getBytes());
                    } else {
                        fileOutputStream.write(mVATestResult ? "true,".getBytes() : "false,".getBytes());
                    }
                    fileOutputStream.write(mButtonsMicrophoneTest.getMicrophoneTestResult() ? "true,".getBytes() : "false,".getBytes());
                    fileOutputStream.write(mButtonsMediaTest.getMediaTestResult() ? "true,".getBytes() : "false,".getBytes());
                    fileOutputStream.write((mBatteryLevel + "mv,").getBytes());

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = df.format(c.getTime());
                    fileOutputStream.write((formattedDate + ",").getBytes());

                    String result = (mOmegaTestResult && mVATestResult &&
                            mButtonsMediaTest.getMediaTestResult() &&
                            mButtonsMicrophoneTest.getMicrophoneTestResult()) ? "PASS" : "FAIL";
                    fileOutputStream.write(result.getBytes());

                    fileOutputStream.write(System.lineSeparator().getBytes());

                    Toast.makeText(mContext, "Export Successful to Documents/results.csv Result: " + result, Toast.LENGTH_SHORT).show();
                    fileOutputStream.close();
                    reset();
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

        mUpdateButton = (Button) findViewById(R.id.update);
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                Intent intent = new Intent();
                intent.setAction(HandsFreeService.ACTION_CHECK_FOR_UPDATES);
                intent.setPackage(getPackageName());
                startService(intent);
            }
        });
        mController.registerListener(mCallback);
    }

    private void reset() {
        mOmegaTestToggle.setChecked(false);
        mVATestToggle.setChecked(false);
        mButtonsMediaTest.resetToggle();
        mButtonsMicrophoneTest.resetToggle();
    }


    private void handleOnError(GaiaError error) {
        switch (error.getType()) {
            case CONNECTION_FAILED:
                mGaiaConnectionTextView.setText(R.string.retry_to_connect);
                break;
            default:
                mGaiaConnectionTextView.setText(R.string.retry_to_connect);
                Log.w(TAG, "handleOnError: " + error.getStringException());
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindToMusicService();
        mController.getUUID();
        mController.getSerialNumber();
        mController.getAppVersion();
        mController.getBatteryLevel();
        if(getIntent().getAction().contains("VOICE") ) {
           mVATestToggle.setChecked(true);
        }
        if (mController.getBluetoothDevice() != null) {
            ((TextView) findViewById(R.id.mac)).setText(mController.getBluetoothDevice().getAddress());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBound) {
            unbindService(mConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mController.unRegisterListener(mCallback);
        getApplication().unregisterOnProvideAssistDataListener(this);
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
                        getFragmentManager().beginTransaction().add(R.id.voiceassistant, mStatusFragment).commit();
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.radioProductionFota:
                Config.setChannel(mContext, Config.PRODUCTION);
                break;
            case R.id.radioDevFota:
                Config.setChannel(mContext, Config.DEV);
                break;
            case R.id.radioQaFota:
                Config.setChannel(mContext, Config.QA);
                break;
        }
    }

    @Override
    public void onProvideAssistData(Activity activity, Bundle data) {
        Log.d(TAG, "onProvideAssistData: ");
    }

    @Override
    public void onProvideAssistData(Bundle data) {
        super.onProvideAssistData(data);
        Log.d(TAG, "onProvideAssistData: ");
    }

    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        super.onProvideAssistContent(outContent);
        Log.d(TAG, "onProvideAssistContent: ");
    }

    public class MyAssistantSessionService
            extends VoiceInteractionSessionService {
        @Override
        public VoiceInteractionSession onNewSession(Bundle bundle) {
            return new MyAssistantSession(this);
        }
    }


    public class MyAssistantSession extends VoiceInteractionSession {
        @Override
        public void onHandleAssist(@Nullable Bundle data, @Nullable AssistStructure structure, @Nullable AssistContent content) {
            super.onHandleAssist(data, structure, content);

        }

        public MyAssistantSession(Context context) {
            super(context);
        }
    }
    public class MyAssistantService extends VoiceInteractionService {

    }
}
