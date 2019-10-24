package za.co.cradle.PackhouseLabeling;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    Button buttonScan, buttonReset;

    TextView tvSolution;

    ZebraPrinter zebraPrinter;

    com.zebra.sdk.comm.Connection connection;

    BluetoothDevice bluetoothDevice;

    EditText etBarcode, etLabelQuantity;

    String solutionCode, uniqueID, name;

    LinearLayout loadScreen;

    ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buttonScan = findViewById(R.id.buttonPrint);
        buttonScan.setOnClickListener(this);
        buttonReset = findViewById(R.id.buttonReset);
        buttonReset.setOnClickListener(this);

        tvSolution = findViewById(R.id.textViewSolution);
        tvSolution.setOnClickListener(this);
        tvSolution.setText("");

        etBarcode = findViewById(R.id.editTextBarcode);
        etBarcode.setInputType(InputType.TYPE_NULL);
        etBarcode.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        etBarcode.setTextIsSelectable(true);
        etBarcode.requestFocus();

        etLabelQuantity = findViewById(R.id.editTextQuantity);
        etLabelQuantity.clearFocus();

        loadScreen = findViewById(R.id.loadScreenMain);
        loadScreen.setVisibility(View.GONE);

        imageView = findViewById(R.id.imageView);
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(getApplicationContext(), "Hello World!", Toast.LENGTH_SHORT).show();
                return false;
            }
        });


        verifyStoragePermissions(this);
        getBluetoothDevices();
        createDirectory();
        getDefaultQty();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("Debug", "onResume");
        getPackingSolution();
        getDefaultQty();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.buttonPrint:
                if (fieldCheck()) {
                    loadScreen.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    print();
                                    loadScreen.setVisibility(View.GONE);
                                    resetFields();
                                }
                            });
                        }
                    }).start();
                }
                break;

            case R.id.textViewSolution:
                Intent intent1 = new Intent(MainActivity.this, ListActivity.class);
                startActivity(intent1);
                break;

            case R.id.buttonReset:
                clear();
                break;
        }
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int action = keyEvent.getAction();
        int code = keyEvent.getKeyCode();

        if (etBarcode.isFocused()) {
            if (etBarcode.getText().toString().matches(".*\\d+.*")) {
                switch (code) {
                    case 285:
                        switch (action) {
                            case 1:
                                etBarcode.setFocusableInTouchMode(false);
                                etBarcode.setFocusable(false);
                                etBarcode.setTextColor(Color.parseColor("#6e6e6e"));
                                break;
                        }
                        break;

                    case 286:
                        switch (action) {
                            case 1:
                                etBarcode.setFocusableInTouchMode(false);
                                etBarcode.setFocusable(false);
                                etBarcode.setTextColor(Color.parseColor("#6e6e6e"));
                                break;
                        }
                        break;
                }
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    public void createDirectory() {
        File root = new File(Environment.getExternalStorageDirectory(), "CradleTechnologyServices"+File.separator+"CSV"+File.separator+"packing");

        if (!root.exists()) {
            root.mkdirs();
            Log.d("Directory", "created");
        }
    }

    public void getPackingSolution() {
        File root = new File(Environment.getExternalStorageDirectory(), "CradleTechnologyServices"+File.separator+"CSV"+File.separator+"packing");
        File file = new File(root, "solutions.csv");

        SharedPreferences preferences = getSharedPreferences("ListPrefs", MODE_PRIVATE);
        String solution = preferences.getString("solution", "");

        try {
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file)));
            reader.readNext();

            List<String> data = new ArrayList<>();
            String[] line;

            while ((line = reader.readNext()) != null) {
                if (line[0].equals(solution)) {
                    solutionCode = line[1];
                    name = line[0];
                }
            }

            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (solution != "") {
            tvSolution.setText(solution);
            etLabelQuantity.requestFocus();
        }
    }

    public void clear() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Clear Fields");
        builder.setMessage("Are You Sure?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                tvSolution.setText("");
                etBarcode.setText("");
                etLabelQuantity.setText("");

                etBarcode.setFocusableInTouchMode(true);
                etBarcode.setFocusable(true);
                etBarcode.requestFocus();

                SharedPreferences.Editor editor = getSharedPreferences("ListPrefs", MODE_PRIVATE).edit();
                editor.remove("solution");
                editor.apply();
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

    public void getBluetoothDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is unsupported for this device!", Toast.LENGTH_LONG).show();

        } else {
            if (adapter.isEnabled()) {
                Set<BluetoothDevice> devices = adapter.getBondedDevices();

                if (devices.size() > 0) {
                    for (BluetoothDevice pairedDevice : devices) {

                        if (pairedDevice.getName().contains("XXRAJ")) {
                            bluetoothDevice = pairedDevice;
                        }
                    }
                }
            }
        }
    }

    public void print() {
        File root = new File(Environment.getExternalStorageDirectory(), "CradleTechnologyServices"+File.separator+"Labels");
        File labelFile = new File(root, "label.LBL");

        openConnection();
        createLabel(labelFile, root);
        sendLabel(labelFile);
    }

    public void openConnection() {
        try {
            connection = new BluetoothConnection(bluetoothDevice.getAddress());

            if (!connection.isConnected()) {
                connection.open();
                setPrinterControlLanguage();
            }

            zebraPrinter = ZebraPrinterFactory.getInstance(connection);

        } catch (ConnectionException e) {
            Toast.makeText(getApplicationContext(), "Connection failed, please retry", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Toast.makeText(getApplicationContext(), "Not paired to printer", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void createLabel(File labelFile, File root) {

        try {

            if (!labelFile.exists()) {
                root.mkdirs();
                labelFile.createNewFile();
            }

            FileOutputStream outputStream = new FileOutputStream(labelFile);

            byte[] label = null;

            String barcode = etBarcode.getText().toString();
            String qty = etLabelQuantity.getText().toString();

            getUniqueID();

            PrinterLanguage printerLanguage = zebraPrinter.getPrinterControlLanguage();
            if (printerLanguage == printerLanguage.ZPL) {

                String labelContents = "\u0010CT~~CD,~CC^~CT~\n" +
                        "^XA~TA000~JSN^LT40^MNW^MTD^PON^PMN^LH35,0^JMA^PR5,5~SD10^JUS^LRN^CI0^XZ\n" +
                        "^XA\n" +
                        "^MMT\n" +
                        "^PW400\n" +
                        "^LL0200\n" +
                        "^LS-30\n" +
                        "^FT310,110^A0I,32,32^FH\\^FD"+name+"^FS\n" +
                        "^FT10,110^A0I,32,32^FH\\^FD"+barcode+"^FS\n" +
                        "^BY2,1,100^FT285,0^BCI,100,N,N,N\n" +
                        "^FD"+barcode+solutionCode+uniqueID+"^SF%%%%%ddddd,1^FS\n" +
                        "^PQ"+qty+",0,0,Y^XZ";

                label = labelContents.getBytes();
            }

            outputStream.write(label);
            outputStream.flush();
            outputStream.close();

            setUniqueID();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void sendLabel(File labelFile) {
        try {
            zebraPrinter.sendFileContents(labelFile.getAbsolutePath());
            connection.close();
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void getUniqueID() throws IOException {
        File root = getApplicationContext().getFilesDir();
        File file = new File(root, "id.txt");

        if (!file.exists()) {
            file.createNewFile();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            writer.write("1");
            writer.flush();
            writer.close();
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        String line;
        String value = null;

        while ((line = reader.readLine()) != null) {
            value = line;
            Log.d("getId", ""+value);
        }

        reader.close();

        if (Integer.parseInt(value) + Integer.parseInt(etLabelQuantity.getText().toString()) > 99999) {
            value = "1";

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            writer.write("1");
            writer.flush();
            writer.close();
        }

        uniqueID = String.format("%05d", Integer.parseInt(value));
    }

    public void setUniqueID() throws IOException {
        File root = getApplicationContext().getFilesDir();
        File file = new File(root, "id.txt");

        String increment = etLabelQuantity.getText().toString();
        int value = Integer.parseInt(uniqueID) + Integer.parseInt(increment);
        String strValue = Integer.toString(value);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        writer.write(strValue);
        writer.flush();
        writer.close();
    }

    public void getDefaultQty() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String qty = prefs.getString("default_label_qty", "");
        etLabelQuantity.setText(qty);
    }

    public boolean fieldCheck() {
        if (!etBarcode.getText().toString().equals("") && !tvSolution.getText().toString().equals("") && !etLabelQuantity.getText().toString().equals("")) {
            int qty = Integer.parseInt(etLabelQuantity.getText().toString());

            if (qty < 100 && qty >= 1) {
                return true;
            } else {
                Toast.makeText(getApplicationContext(), "Incorrect label quantity", Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            Toast.makeText(getApplicationContext(), "Ensure fields are correct", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public void resetFields() {
        etBarcode.setText("");
        etBarcode.setFocusable(true);
        etBarcode.setFocusableInTouchMode(true);
        etLabelQuantity.setFocusable(true);
        etLabelQuantity.setFocusableInTouchMode(true);

        etBarcode.requestFocus();

        SharedPreferences.Editor editor = getSharedPreferences("ListPrefs", MODE_PRIVATE).edit();
        editor.remove("solution");
        editor.apply();
    }

    public void setPrinterControlLanguage() throws ConnectionException {
        SGD.SET("device.languages", "hybrid_xml_zpl", connection);
    }

    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, permissions, 1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
