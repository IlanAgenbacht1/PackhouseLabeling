package za.co.cradle.PackhouseLabeling;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener {

    File file;

    BluetoothDevice bluetoothDevice;

    Connection connection;

    ZebraPrinter zebraPrinter;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };


    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        setupActionBar();

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("default_label_qty"));

        Preference calibrate = findPreference("calibrate");
        calibrate.setOnPreferenceClickListener(this);
        Preference reset = findPreference("reset");
        reset.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "calibrate":
                buildCalibrateDialog();
                break;

            case "reset":
                buildResetDialog();
                break;
        }
        return false;
    }

    public void buildCalibrateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle("Calibrate Printer");
        builder.setMessage("Are you sure?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                createCalibrationLabel();
                sendCalibrationLabel();
                Toast.makeText(getApplicationContext(), "Calibration Complete", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.create();
        builder.show();
    }

    public void buildResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle("Reset Printer");
        builder.setMessage("Are you sure?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                resetPrinter();
                Toast.makeText(getApplicationContext(), "Reset Complete", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.create();
        builder.show();
    }

    public void createCalibrationLabel() {
        getPairedDevices();

        try {
            connection = new BluetoothConnection(bluetoothDevice.getAddress());
            connection.open();
            zebraPrinter = ZebraPrinterFactory.getInstance(connection);

            File root = new File(Environment.getExternalStorageDirectory(), "CradleTechnologyServices"+File.separator+"Labels");
            file = new File(root, "callabel.LBL");

            try {
                if (!file.exists()) {
                    root.mkdirs();
                    file.createNewFile();
                }

                FileOutputStream fileOutputStream = new FileOutputStream(file);

                byte[] label = null;

                String labelContents = "\u0010CT~~CD,~CC^~CT~\n" +
                        "^XA~TA000~JSN^LT0^MNW^MTD^PON^PMN^LH50,0^JMA^PR5,5~SD10^JUS^LRN^CI0^XZ\n" +
                        "~JC^XA^JUS^XZ\n" +
                        "^XA\n" +
                        "^MMT\n" +
                        "^PW400\n" +
                        "^LL0200\n" +
                        "^LS0\n" +
                        "^FT332,123^A0I,28,28^FH\\^FDPrinter Calibrated^FS\n" +
                        "^PQ1,0,1,Y^XZ";

                label = labelContents.getBytes();

                fileOutputStream.write(label);
                fileOutputStream.flush();
                fileOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (ConnectionException e) {
            Toast.makeText(getApplicationContext(), "Connection failed, please retry", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Toast.makeText(getApplicationContext(), "Not paired to printer", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void sendCalibrationLabel() {
        try {
            zebraPrinter.sendFileContents(file.getAbsolutePath());
            connection.close();

        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void resetPrinter() {
        try {
            connection = new BluetoothConnection(bluetoothDevice.getAddress());
            connection.open();

            zebraPrinter = ZebraPrinterFactory.getInstance(connection);

            zebraPrinter.sendCommand("^XA^JUF^XZ^XA^JUN^XZ^XA^JUS^XZ");
            zebraPrinter.restoreDefaults();

            connection.close();

        } catch (ConnectionException e) {
            Toast.makeText(getApplicationContext(), "Connection failed, please retry", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
            Toast.makeText(this, "Not paired to printer", Toast.LENGTH_SHORT).show();
        }
    }

    public void getPairedDevices() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is unsupported for this device!", Toast.LENGTH_LONG).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

                if (devices.size() > 0) {
                    for (BluetoothDevice pairedDev:devices) {

                        if (pairedDev.getName().contains("XXRAJ")) {
                            bluetoothDevice = pairedDev;
                        }
                    }
                }
            }
        }
    }
}
