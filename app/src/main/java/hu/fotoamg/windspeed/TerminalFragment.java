package hu.fotoamg.windspeed;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.util.Calendar;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import hu.fotoamg.windspeed.nmea.INmeaHandler;
import hu.fotoamg.windspeed.nmea.NmeaReceiver;
import hu.fotoamg.windspeed.nmea.Sentences.GGA;
import hu.fotoamg.windspeed.nmea.Sentences.GSA;
import hu.fotoamg.windspeed.nmea.Sentences.GST;
import hu.fotoamg.windspeed.nmea.Sentences.GSV;
import hu.fotoamg.windspeed.nmea.Sentences.HDT;
import hu.fotoamg.windspeed.nmea.Sentences.RMC;
import hu.fotoamg.windspeed.nmea.Sentences.VTG;
import java.nio.charset.StandardCharsets;


public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener, INmeaHandler {
    //Permision code that will be checked in the method onRequestPermissionsResult
    private int STORAGE_PERMISSION_CODE = 23;

    private enum Connected { False, Pending, True }

    private StringBuffer debugMsg = new StringBuffer();
    private String deviceAddress;
    private String newline = "\r\n";

    private TextView receiveText;
    private TextView debugText;
    private TextView altSatText;

    private CheckBox debugCheckBox;
    private CheckBox parseCheckBox;
    private CheckBox altCheckBox;

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    // ... Create the NMEA receiver
    private NmeaReceiver nmeaReceiver = new NmeaReceiver( this ) ;

    private float baseAlt = 0f;
    private float currAlt = 0f;


    public TerminalFragment() {
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");


        // ... Attach handler for NMEA messages that fail NMEA checksum verification
        nmeaReceiver.setMessageHandlers( new NmeaReceiver.MessageHandlers() {
            @Override
            public void OnNmeaMessageFailedChecksum( byte[] bytes, int index, int count, byte expected, byte actual ) {
                String sentence = new String(bytes, index, count, StandardCharsets.US_ASCII ) ;
                appendDebug("Failed Checksum: " + sentence + "; expected "+expected+" but got " + actual );

            }

            @Override
            public void OnNmeaMessageDropped( byte[] bytes, int index, int count, String reason ) {
                String sentence = new String(bytes, index, count, StandardCharsets.US_ASCII ) ;
                appendDebug("Bad Syntax: "+sentence+"; reason: "+reason );
            }

            @Override
            public void OnNmeaMessageIgnored( byte[] bytes, int index, int count ) {
                String sentence = new String(bytes, index, count, StandardCharsets.US_ASCII ) ;
                appendDebug("Ignored: " + sentence ) ;
            }
        } ) ;
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service !=null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        //receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        debugText = view.findViewById(R.id.debugText);
        debugCheckBox = view.findViewById(R.id.debugOn);
        parseCheckBox = view.findViewById(R.id.parseOn);
        altCheckBox =  view.findViewById(R.id.altFixCheck);

        TextView sendText = view.findViewById(R.id.send_text);
        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));
        View resetBtn = view.findViewById(R.id.reset_btn);
        resetBtn.setOnClickListener(v -> resetClick(view));

        altCheckBox.setOnClickListener(v -> altFixClick(view));
        altSatText = view.findViewById(R.id.altSatCount);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            /*Toast.makeText(getContext(), "CLEAR",
                    Toast.LENGTH_LONG).show();*/
            appendDebug("CLEAR clicked");
            receiveText.setText("");
            return true;
        } else if (id ==R.id.newline) {
            /*Toast.makeText(getContext(), "NEWLINE",
                    Toast.LENGTH_LONG).show();*/
            appendDebug("NEWLINE clicked");
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id ==R.id.savelog) {
            /*Toast.makeText(getContext(), "SAVELOGGGG",
                    Toast.LENGTH_LONG).show();*/
            appendDebug("saveLog clicked");
            saveLog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status("connecting...");
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connected to " + deviceName);
            socket.connect(getContext(), service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        receiveText.append(new String(data));
        if(parseCheckBox.isChecked()) nmeaReceiver.Receive( data ) ;
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str+'\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private void appendDebug(String str) {
        if (debugText != null && debugCheckBox.isChecked()) {
            debugText.append("\n");
            debugText.append(str);
        }
    }

    public void resetClick(View view) {
        debugText.setText("Debug msg:");
        appendDebug(" RESET pushed!\n");
    }


    public void altFixClick(View view) {
        appendDebug(" Alt FIX clicked! " + (altCheckBox.isChecked() ? "ON" :  "OFF") + "\n");
        if (altCheckBox.isChecked()) {
            baseAlt = currAlt;
        }

    }


    public void saveLog() {
        String text = receiveText.getText().toString();
        FileOutputStream fos = null;
        String state = Environment.getExternalStorageState();

        if(Environment.MEDIA_MOUNTED.equals(state)) {
            java.util.Date currDate = Calendar.getInstance().getTime();
            String pattern = "yyyyMMdd_HH-mm";
            SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat(pattern, new Locale("hu", "HU"));
            String date = simpleDateFormat.format(currDate);
            String Filename = "GPSlog" + date + ".txt";

            /*Toast.makeText(getContext(), "FILENAME: /BtGpsWindLog/" + Filename,
                    Toast.LENGTH_LONG).show();*/
            appendDebug("FILENAME: /BtGpsWindLog/" + Filename);

            File Root = Environment.getExternalStorageDirectory();

            /*Toast.makeText(getContext(), "Root path: " + Root.getPath() ,
                    Toast.LENGTH_LONG).show();*/
            appendDebug("Root path: " + Root.getPath() );
            File Dir = new File(((File) Root).getAbsolutePath()+"/BtGpsWindLog");

            if(!isReadStorageAllowed() || !isWriteStorageAllowed()){
                appendDebug("Missing permissions permission READ: " + isReadStorageAllowed() + " WRITE: " + isWriteStorageAllowed() );
                requestStoragePermission();
                appendDebug("after REQUESTING permissions! ");
            }  else {
               /* Toast.makeText(getContext(), "You already have the permission", Toast.LENGTH_LONG).show();*/
                appendDebug("You already have the permission");
            }


            try {
               /* Toast.makeText(getContext(), "Dir path: " + Dir.getPath() ,
                        Toast.LENGTH_LONG).show();*/
                appendDebug("Dir path: " + Dir.getPath());
                if (!Dir.exists() || !Dir.isDirectory()) {

                    /*Toast.makeText(getContext(), "No Dir... creating" ,
                            Toast.LENGTH_LONG).show();*/
                    appendDebug("No Dir... creating");
                    Dir.mkdirs();

                    /*Toast.makeText(getContext(), "Dir created!" ,
                            Toast.LENGTH_LONG).show();
                    Toast.makeText(getContext(), "Dir exists: " + Dir.exists(),
                            Toast.LENGTH_LONG).show();*/
                    appendDebug("Dir created!");
                    appendDebug("Dir exists: " + Dir.exists());
                }

                File file = new File(Dir, Filename);
                fos = new FileOutputStream(file);
                fos.write(text.getBytes());
                Toast.makeText(getContext(), "Saved to /BtGpsWindLog/" + Filename,
                        Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Exception: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Toast.makeText(getContext(), "SD card not found!",
                    Toast.LENGTH_LONG).show();
        }

    }

    //We are calling this method to check the permission status
    private boolean isReadStorageAllowed() {
        //Getting the permission status
        int result = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);

        //If permission is granted returning true
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;

        //If permission is not granted returning false
        return false;
    }

    //We are calling this method to check the permission status
    private boolean isWriteStorageAllowed() {
        //Getting the permission status
        int result = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        //If permission is granted returning true
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;

        //If permission is not granted returning false
        return false;
    }

    //Requesting permission
    private void requestStoragePermission(){
        appendDebug("REQUESTING permissions! ");
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity() ,Manifest.permission.READ_EXTERNAL_STORAGE)){
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }

        //And finally ask for the permission
        ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
        ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
    }

    @Override
    public void HandleGGA(GGA msg) {
        currAlt =  msg.get_altitude();
        appendDebug(" GGA received, alt: " + currAlt + " sats: " + msg.get_satelliteCount());
        altSatText.setText(" Alt:" + (currAlt-baseAlt) + " NrSats: " + msg.get_satelliteCount());
    }

    @Override
    public void HandleGSA(GSA msg) {

    }

    @Override
    public void HandleGST(GST msg) {

    }

    @Override
    public void HandleGSV(GSV msg) {

    }

    @Override
    public void HandleHDT(HDT msg) {

    }

    @Override
    public void HandleRMC(RMC msg) {
        appendDebug(" RMC received!");
    }

    @Override
    public void HandleVTG(VTG msg) {
        appendDebug(" VTG speed: " + msg.get_groundSpeedKph() + "kph true dir: " + msg.get_trueTrackMadeGoodDegrees());
    }

}
