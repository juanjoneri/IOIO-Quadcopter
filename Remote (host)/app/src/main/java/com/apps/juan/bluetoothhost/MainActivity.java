package com.apps.juan.bluetoothhost;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.software.shell.fab.ActionButton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //check for connection
    Handler jHandler = new Handler();
    private Timer mTimer = null;
    int bState;
    int bStatePast;
    boolean barIsActive, estamosConectados, isFlying;

    //Control
    Integer minPulseWidth,sBarValue, PK, IK, DK;
    boolean sendingWidth, sendingP, sendingI, sendingD;
    int selectedOptionToSend;
    //Las constantes se mandan multiplicadas por 100

    //UI
    SeekBar sBar, detailedPwm, pBar, iBar, dBar;
    TextView textPwm, textdetailedPwm, textP, textI, textD;
    ActionButton bTakeOffLand;
    Button bPlus, bMinus, bStop;
    ValueAnimator anim;

    //Objetos
    private BluetoothAdapter bAdapter = null;
    Activity Activity;
    Set<BluetoothDevice> dispocitivosApareados;
    ArrayAdapter<String> arrayAdapter;
    ConnectedThread connectedThread;
    ConnectThread connectThread;
    BluetoothDevice[] devicesArray = new BluetoothDevice[2];
    byte[] send;

    //Constantes
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String MY_UUID_STRING = "a28b41f3-3ba2-4b4b-9696-8c052a5ea406";
    private static final UUID MY_UUID = UUID.fromString(MY_UUID_STRING);
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MIN_PARA_TAKEOFF = 350;
    public static final int TIME_TO_TAKEOFF = 3000;

    public static final int ASK_CONNECTED = 1991;
    public static final int SEND_WIDTH = 1992;
    public static final int SEND_P = 1993;
    public static final int SEND_I = 1994;
    public static final int SEND_D = 1995;
    public static final int STOP = 1996;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bAdapter = BluetoothAdapter.getDefaultAdapter();
        Activity = MainActivity.this;

        sendingWidth = false;
        sendingP = false;
        sendingI = false;
        sendingD = false;

        sBar = (SeekBar) findViewById(R.id.progressBar);
        detailedPwm = (SeekBar) findViewById(R.id.DetailedPwmBar);
        pBar = (SeekBar) findViewById(R.id.PconstantBar);
        iBar = (SeekBar) findViewById(R.id.IconstantBar);
        dBar = (SeekBar) findViewById(R.id.DconstantBar);

        sBar.setMax(800);
        detailedPwm.setMax(100);
        pBar.setMax(1000);
        iBar.setMax(1000);
        dBar.setMax(1000);

        detailedPwm.setProgress(50);
        textPwm = (TextView) findViewById(R.id.pwm);
        textdetailedPwm = (TextView) findViewById(R.id.DetailedPwm);
        textP = (TextView) findViewById(R.id.PconstantText);
        textI = (TextView) findViewById(R.id.IconstantText);
        textD = (TextView) findViewById(R.id.DconstantText);

        bPlus = (Button) findViewById(R.id.plus_one);
        bMinus = (Button) findViewById(R.id.minus_one);
        bStop = (Button) findViewById(R.id.buttonStop);

        bTakeOffLand = (ActionButton) findViewById(R.id.action_button);
        bTakeOffLand.hide();

        estamosConectados = false;
        isFlying = false;

        mTimer = new Timer();
        bState = 1;
        bStatePast = 0;
        barIsActive =false;

        sBarValue = 0;
        anim = ValueAnimator.ofInt(sBar.getProgress(),MIN_PARA_TAKEOFF);

        if (bAdapter == null) {
            //No tenes bluetooth
            Toast.makeText(Activity, "No tenes Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!bAdapter.isEnabled()){
            //Bluetooth desactivado
            //Android pide al usuario que lo active
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            // Manejamos la respuesta a 'activar bluetooth'"
            if (resultCode == Activity.RESULT_OK) {
                // El bluetooth ah sido activado con exito
                Toast.makeText(Activity, "Click to begin",Toast.LENGTH_SHORT).show();
            } else {
                // El bluetooth no ah sido activado con exito
                Toast.makeText(Activity, "Activa el Bluetooth banana",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        if (connectedThread != null){
            connectedThread.cancel();
        }
        if (connectThread != null){
            connectThread.cancel();
        }
        mTimer.cancel();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_pair) {
            //Buscar telefonos con los que ya nos hemos apareado
            //Mostramos los dispositivos apareados en pantalla
            dispocitivosApareados = bAdapter.getBondedDevices();
            arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1);
            for (BluetoothDevice device : dispocitivosApareados) {
                arrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }

            //construimos un dialogo que pregunte a que dispositivo hay que conectarse
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Choose Device");
            builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Mandemos una solicitud de coneccion a ese dispositivo
                    dispocitivosApareados.toArray(devicesArray);
                    devicesArray = dispocitivosApareados.toArray(new BluetoothDevice[0]);
                    connectThread = new ConnectThread(devicesArray[which]);
                    connectThread.start();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        if (id == R.id.action_conection) {
            //avisamos al usuario acerca del estado de la conexion
            if(estamosConectados){
                Toast.makeText(Activity, "Conectado",Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(Activity, "Desonectado",Toast.LENGTH_SHORT).show();
            }

        }

        if (id==R.id.action_choose){
            if(estamosConectados){
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("What to send");
                CharSequence[] options = new CharSequence[]{
                        "Pulse Width",
                        "P constant",
                        "I constant",
                        "D constant"};
                builder.setSingleChoiceItems(options, selectedOptionToSend, null);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                        selectedOptionToSend = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        switch (selectedOptionToSend) {
                            case 0:
                                sendToServer(SEND_WIDTH);
                                sendingWidth = true;sendingP = false;sendingI = false;sendingD = false;
                                break;
                            case 1:
                                sendToServer(SEND_P);
                                sendingP = true;sendingWidth = false;sendingI = false;sendingD = false;
                                break;
                            case 2:
                                sendToServer(SEND_I);
                                sendingI = true;sendingWidth = false;sendingP = false;sendingD = false;
                                break;
                            case 3:
                                sendToServer(SEND_D);
                                sendingD = true;sendingWidth = false;sendingP = false;sendingI = false;
                                break;
                        }
                    }
                });
                builder.setCancelable(true);
                AlertDialog alert = builder.create();
                alert.show();
            }
        }

        if (id==R.id.action_instructions){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("How to use");
            CharSequence[] instructions = new CharSequence[]{
                    "Pair Server and Host devices",
                    "Open connection on Server by pressing the bluetooth icon",
                    "Request connection on Host by pressing bluetooth icon",
                    "Choose Server device from the list",
                    "Wait until connected",
                    "Set the pulse with using the SeekBar"};
            builder.setItems(instructions,null);
            builder.setCancelable(true);
            AlertDialog alert = builder.create();
            alert.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private class ConnectThread extends Thread {
        //En paralelo se busca conectarse al otro dispositivo
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancelar el modo discovery
            bAdapter.cancelDiscovery();

            try {
                //tratar de conectar los dispositivos
                mmSocket.connect();
            } catch (IOException connectException) {
                // No se pudo establecer la coneccion
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Finalmente tenemos la coneccion en la forma de un socket
            estamosConectados = true;
            estamosConectados(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }


    }

    private void estamosConectados(final BluetoothSocket Socket) {
        runOnUiThread(new Runnable() {
            public void run() {
                connectedThread = new ConnectedThread(Socket);
                connectedThread.start();
                enableUi();
                //comenzar a verificar estado de la coneccion
                mTimer.scheduleAtFixedRate(new CheckConnecion(), 0, 1000);
                bTakeOffLand.show();

            }
        });
    }

    private void enableUi() {
        sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                barIsActive = false;
                sBarValue = sBar.getProgress();
                detailedPwm.setProgress(50);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                barIsActive = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Cuando el usuario cambia la barra de estado queremos que
                //el dato del progreso sea eniado al cliente
                //guardar en variable
                minPulseWidth = progress;
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(minPulseWidth);
                send = b.array();
                if(sendingWidth){
                    connectedThread.write(send);
                }

                //mostrar en pantalla
                textPwm.setText(String.valueOf(minPulseWidth));

                if(progress < MIN_PARA_TAKEOFF){
                    isFlying=false;
                    bTakeOffLand.setImageResource(R.drawable.ic_flight_takeoff_black_48dp);
                }
                else{
                    isFlying=true;
                    bTakeOffLand.setImageResource(R.drawable.ic_flight_land_black_48dp);
                }
            }
        });
        detailedPwm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textdetailedPwm.setText(String.valueOf(progress - 50));
                sBar.setProgress(sBarValue + progress - 50);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        pBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                PK = progress;
                textP.setText(String.valueOf(PK)+"/100");
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(PK);
                send = b.array();
                if(sendingP) {
                    connectedThread.write(send);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        iBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                IK = progress;
                textI.setText(String.valueOf(IK)+"/100");
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(IK);
                send = b.array();
                if(sendingI) {
                    connectedThread.write(send);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        dBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DK = progress;
                textD.setText(String.valueOf(DK)+"/100");
                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(DK);
                send = b.array();
                if(sendingD) {
                    connectedThread.write(send);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        bTakeOffLand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFlying){
                    anim = ValueAnimator.ofInt(sBar.getProgress(),0);
                }
                else {
                    anim = ValueAnimator.ofInt(sBar.getProgress(),MIN_PARA_TAKEOFF);
                }
                anim.setDuration(TIME_TO_TAKEOFF);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int animProgress = (Integer) animation.getAnimatedValue();
                        sBar.setProgress(animProgress);
                    }
                });
                anim.start();
            }
        });
        bMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sBar.setProgress(sBar.getProgress()-1);
            }
        });
        bPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sBar.setProgress(sBar.getProgress()+1);
            }
        });
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToServer(STOP);
            }
        });

    }


    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    //Envia los datos del Pwm
                    //byte[] writeBuf = (byte[]) msg.obj;
                    //String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    //Recibe datos acerca de la coneccion
                    byte[] readBuf = (byte[]) msg.obj;
                    Integer i = ByteBuffer.wrap(readBuf).getInt();
                    if (i==ASK_CONNECTED){
                        bState ++;
                    }
                    break;
            }
        }
    };

    private class ConnectedThread extends Thread {
        //Finalmente, este es el thread que maneja la comunicacion
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // En run se pasa escuchando por si le llega un mensaje
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    // Enviar la informacion resultante al UI thread mediante un handler
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //Manda la informacion al otro celular
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        //Termina la coneccion
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    //verifica si la connexion sigue
    class CheckConnecion extends TimerTask {

        @Override
        public void run() {
            // run on another thread
            jHandler.post(new Runnable() {

                @Override
                public void run() {
                    // check if the count has continued
                    if (bState >= bStatePast){
                        //estamos conectados
                        bStatePast++;
                        estamosConectados = true;
                    }
                    else{
                        //se perdio la coneccion
                        if(!barIsActive && !anim.isRunning()){
                            if(!estamosConectados){
                                Toast.makeText(Activity, "Desonectados",Toast.LENGTH_SHORT).show();
                            }
                            estamosConectados = false;
                        }
                    }
                    //request if it still on
                    sendToServer(ASK_CONNECTED);

                }

            });
        }
    }

    public void sendToServer (int value){
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(value);
        send = b.array();
        connectedThread.write(send);
    }
}
