package com.apps.juan.bluetoothserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class MainActivity extends IOIOActivity implements SensorEventListener {
    SensorManager sensorManager = null;

    //UI
    Button bBluetooth, bCallibration;
    TextView baseDutyT, pwmUpT, pwmDownT, pwmRightT, pwmLeftT;
    TextView orientationXT, orientationYT, orientationZT;
    TextView sBarPT, sBarIT, sBarDT;

    //Objetos
    private BluetoothAdapter bAdapter = null;
    android.app.Activity Activity;
    ConnectedThread connectedThread;
    AcceptThread acceptThread;
    byte[] send;
    ArrayList<Float> pidVerticalList = new ArrayList<>();
    ArrayList<Float> pidHorizontalList = new ArrayList<>();
    public IOIO ioio = null;
    boolean isIoioConnected = false;

    //Constantes
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String MY_UUID_STRING = "a28b41f3-3ba2-4b4b-9696-8c052a5ea406";
    private static final UUID MY_UUID = UUID.fromString(MY_UUID_STRING);
    private static final String NAME = "BluetoothServer";
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;

    public static final int ASK_CONNECTED = 1991;
    public static final int SEND_WIDTH = 1992;
    public static final int SEND_P = 1993;
    public static final int SEND_I = 1994;
    public static final int SEND_D = 1995;
    public static final int STOP = 1996;

    //Variables de control
    float baseDuty,pwmUp, pwmDown, pwmRight, pwmLeft;
    float pidHorizontalDerecha, pidHorizontalIzquierda, pidVerticalArriba, pidVerticalAbajo;
    Float anguloX, anguloY, anguloZ;
    CalcularPidHorizontal calcularPidHorizontal;
    CalcularPidVertical calcularPidVertical;
    float integralH, integralV, derivadaH, derivadaV;
    boolean receivingWidth, receivingP, receivingI, receivingD;

    //Constantes de control
    float KP = 0;
    float KI = 0;
    float KD = 0;
    final static int BASE = 1060;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        setContentView(R.layout.activity_main);

        pidVerticalList.add(0f);
        pidVerticalList.add(0f);
        pidHorizontalList.add(0f);
        pidHorizontalList.add(0f);

        bAdapter = BluetoothAdapter.getDefaultAdapter();
        Activity = MainActivity.this;

        baseDutyT= (TextView) findViewById(R.id.baseDuty);

        pwmUpT = (TextView) findViewById(R.id.pwmUp);
        pwmDownT = (TextView) findViewById(R.id.pwmDown);
        pwmRightT = (TextView) findViewById(R.id.pwmRight);
        pwmLeftT = (TextView) findViewById(R.id.pwmLeft);

        orientationXT = (TextView) findViewById(R.id.orientationX);
        orientationYT = (TextView) findViewById(R.id.orientationY);
        orientationZT = (TextView) findViewById(R.id.orientationZ);

        sBarPT = (TextView) findViewById(R.id.seekBarPText);
        sBarIT = (TextView) findViewById(R.id.seekBarIText);
        sBarDT = (TextView) findViewById(R.id.seekBarDText);

        bBluetooth = (Button) findViewById(R.id.bBluetooth);
        bBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Se comienza a escuchar por conecciones entrantes
                acceptThread = new AcceptThread();
                acceptThread.start();
                Toast.makeText(Activity, "acceptThread started",Toast.LENGTH_SHORT).show();
            }
        });

        bCallibration = (Button) findViewById(R.id.bCallibration);
        bCallibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent startCallibrationIntent = new Intent(MainActivity.this, CallibrationActivity.class);
                startActivity(startCallibrationIntent);
            }
        });


        if (bAdapter == null) {
            //No tenes bluetooth
            Toast.makeText(Activity, "No tenes Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!bAdapter.isEnabled()){
            //Bluetooth desactivado
            Toast.makeText(Activity, "Activa el Bluetooth", Toast.LENGTH_SHORT).show();
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            // Manejamos la respuesta a 'activar bluetooth'"
            if (resultCode == Activity.RESULT_OK) {
                // El bluetooth ah sido activado con exito
                Toast.makeText(Activity, "Presione para conectar",Toast.LENGTH_SHORT).show();
            } else {
                // El bluetooth no ah sido activado con exito
                Toast.makeText(Activity, "Activa el Bluetooth salame",Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onStop() {
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION));
        if(isIoioConnected){
            ioio.disconnect();
        }

        if (connectedThread != null){
            connectedThread.cancel();
        }
        if (acceptThread != null){
            acceptThread.cancel();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                sensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        anguloX = event.values[1];
        anguloY = event.values[2];
        anguloZ = event.values[0];

        calcularPidVertical = new CalcularPidVertical();
        calcularPidHorizontal = new CalcularPidHorizontal();

        calcularPidVertical.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, anguloX);
        calcularPidHorizontal.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, anguloY);

    }

    public float toNearestValue(float fl){
        int i = Math.round(fl*1000);
        fl = i;
        return fl/1000;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // El codigo principal para aceptar coneccion
            // Escuchamos hasta que se establezca la coneccion
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // Si la coneccion fue exitosa..
                if (socket != null) {
                    // Hacemos algo con esa coneccion el el thread principal
                    estamosConectados(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    private void estamosConectados(final BluetoothSocket Socket) {
        runOnUiThread(new Runnable(){
            public void run() {
                Toast.makeText(Activity, "Estamos Conectados",Toast.LENGTH_SHORT).show();
                connectedThread = new ConnectedThread(Socket);
                connectedThread.start();
            }
        });
    }

    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    //no envia nada pero ta
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                case MESSAGE_READ:
                    //Que hacemos con la informacion recibida en forma de Byte[]
                    byte[] readBuf = (byte[]) msg.obj;
                    Integer i = ByteBuffer.wrap(readBuf).getInt();
                    switch (i){
                        case ASK_CONNECTED:
                            //recibimos la pregunta , estamos conectados?
                            ByteBuffer b = ByteBuffer.allocate(4);
                            b.putInt(ASK_CONNECTED);
                            send = b.array();
                            connectedThread.write(send);
                            break;
                        case SEND_WIDTH:
                            receivingWidth=true;receivingP=false;receivingI=false;receivingD=false;
                            toast("Width");
                            break;
                        case SEND_P:
                            receivingP=true;receivingWidth=false;receivingI=false;receivingD=false;
                            toast("P");
                            break;
                        case SEND_I:
                            receivingI=true;receivingWidth=false;receivingP=false;receivingD=false;
                            toast("I");
                            break;
                        case SEND_D:
                            receivingD=true;receivingWidth=false;receivingP=false;receivingI=false;
                            toast("D");
                            break;
                        case STOP:
                            baseDuty = 0;
                            toast("stop");
                            break;
                        default:
                            if(receivingWidth){
                                Float f = i.floatValue();
                                baseDuty = f/1000;
                                baseDutyT.setText(String.valueOf(baseDuty));
                            }
                            else if (receivingP){
                                Float f = i.floatValue();
                                KP = f/1000;
                                sBarPT.setText(String.valueOf(KP));

                            }
                            else if (receivingI){
                                Float f = i.floatValue();
                                KI = f/10000;
                                sBarIT.setText(String.valueOf(KI));
                            }
                            else if (receivingD){
                                Float f = i.floatValue();
                                KD = f/10000;
                                sBarDT.setText(String.valueOf(KD));
                            }


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
            // En esta app no es necesario pero es obligatorio para un BluetoothDevice
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    // La informacion obtenida se manda al handler para que haga algo en el UI
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

    private class CalcularPidVertical extends AsyncTask<Float, Void, ArrayList<Float>>{
        @Override
        protected void onPreExecute() {
            //los imprimo toNearestValue para que no ocupen mucho en pantalla
            orientationXT.setText(String.valueOf(toNearestValue(anguloX)));
            orientationYT.setText(String.valueOf(toNearestValue(anguloY)));
            orientationZT.setText(String.valueOf(toNearestValue(anguloZ)));
        }

        @Override
        protected ArrayList<Float> doInBackground(Float... params) {
            integralV+=params[0];

            float partePvertical = toNearestValue(params[0] * KP);
            float parteIvertical = toNearestValue(integralV * KI);
            float parteDvertical = toNearestValue((params[0] - derivadaV)* KD);

            derivadaV = params[0];

            if(params[0]>=0) {
                //si se inclina hacia la arriba
                pidVerticalArriba = baseDuty + partePvertical + parteDvertical;
                //si la derivada es negativa es que me estoy acercando a la normal, y por eso restarla esta bien
                if(parteIvertical > 0){
                    //La parte integral solo la sumo del lado que aporta
                    pidVerticalArriba += parteIvertical;
                }
                pidVerticalList.set(0, pidVerticalArriba);

                //el otro motor solo es influido por la parte integral si es el caso
                pidVerticalAbajo = baseDuty;
                if(parteIvertical < 0){
                   pidVerticalAbajo -= parteIvertical;
                }
                pidVerticalList.set(1, pidVerticalAbajo);
            }
            else{
                //si se inclina hacia la arriba
                pidVerticalAbajo = baseDuty - partePvertical - parteDvertical;
                //si la derivada es positiva es que me estoy acercando a la normal, por eso la resto
                if(parteIvertical < 0){
                    //La parte integral solo la sumo del lado que aporta
                    pidVerticalAbajo -= parteIvertical;
                }
                pidVerticalList.set(1, pidVerticalAbajo);

                //el otro motor solo es influido por la parte integral si es el caso
                pidVerticalArriba = baseDuty;
                if(parteIvertical > 0){
                    pidVerticalArriba += parteIvertical;
                }
                pidVerticalList.set(0, pidVerticalArriba);
            }
            return pidVerticalList;
            //primero arriba despues abajo
        }

        @Override
        protected void onPostExecute(ArrayList<Float> aFloat) {
            pwmUp = toNearestValue(aFloat.get(0));
            pwmDown = toNearestValue(aFloat.get(1));
            pwmUpT.setText(String.valueOf(pwmUp));
            pwmDownT.setText(String.valueOf(pwmDown));
        }
    }

    private class CalcularPidHorizontal extends AsyncTask<Float, Void, ArrayList<Float>>{

        @Override
        protected ArrayList<Float> doInBackground(Float... params) {
            integralH+=params[0];

            float partePhorizontal = toNearestValue(params[0] * KP);
            float parteIhorizontal = toNearestValue(integralH * KI);
            float parteDhorizontal = toNearestValue((params[0] - derivadaH)* KD);

            derivadaH = params[0];

            if(params[0]>=0) {
                //si se inclina hacia la izquierda
                pidHorizontalIzquierda = baseDuty + partePhorizontal + parteDhorizontal;
                //si la derivada es negativa es que me estoy acercando a la normal, y por eso restarla esta bien
                if(parteIhorizontal > 0){
                    //La parte integral solo la sumo del lado que aporta
                    pidHorizontalIzquierda += parteIhorizontal;
                }
                pidHorizontalList.set(0, pidHorizontalIzquierda);

                //el otro motor solo es influido por la parte integral si es el caso
                pidHorizontalDerecha = baseDuty;
                if(parteIhorizontal < 0){
                    pidHorizontalDerecha -= parteIhorizontal;
                }
                pidHorizontalList.set(1, pidHorizontalDerecha);
            }
            else{
                //si se inclina hacia la derecha
                pidHorizontalDerecha = baseDuty - partePhorizontal - parteDhorizontal;
                //si la derivada es positiva es que me estoy acercando a la normal, por eso la resto
                if(parteIhorizontal < 0){
                    //La parte integral solo la sumo del lado que aporta
                    pidHorizontalDerecha -= parteIhorizontal;
                }
                pidHorizontalList.set(1, pidHorizontalDerecha);

                //el otro motor solo es influido por la parte integral si es el caso
                pidHorizontalIzquierda = baseDuty;
                if(parteIhorizontal > 0){
                    pidHorizontalIzquierda += parteIhorizontal;
                }
                pidHorizontalList.set(0, pidHorizontalIzquierda);
            }
            return pidHorizontalList;
            //primero izquierda despues derecha
        }

        @Override
        protected void onPostExecute(ArrayList<Float> aFloat) {
            pwmLeft = toNearestValue(aFloat.get(0));
            pwmRight = toNearestValue(aFloat.get(1));
            pwmRightT.setText(String.valueOf(pwmRight));
            pwmLeftT.setText(String.valueOf(pwmLeft));
        }
    }
    private class Looper extends BaseIOIOLooper {
        private PwmOutput motorUp, motorDown, motorLeft, motorRight;
        private PwmOutput[] motoresArray;

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {

                if(baseDuty>0){
                    actualizarPwm(motoresArray);
                    Thread.sleep(100);
                }
            else{
                    for (PwmOutput o : motoresArray ){
                        o.setPulseWidth(1060);
                    }
                }




        }

        @Override
        protected void setup() throws ConnectionLostException, InterruptedException {
            ioio = ioio_;
            isIoioConnected = true;
            toast("IOIO connected");

            motorUp = ioio_.openPwmOutput(1, 400);
            motorDown = ioio_.openPwmOutput(2, 400);
            motorRight = ioio_.openPwmOutput(3, 400);
            motorLeft = ioio_.openPwmOutput(4, 400);

            motoresArray = new PwmOutput[] {motorUp, motorDown, motorRight, motorLeft};

        }

        @Override
        public void disconnected() {
            ioio=null;
            isIoioConnected = true;
            toast("IOIO disconnected");
            for (PwmOutput o : motoresArray ){
                o.close();
            }
        }

        public void actualizarPwm (PwmOutput[] motoresArray) throws ConnectionLostException {

            //El array debe contener los motores en el orden Up Down Right Left
            motoresArray[0].setPulseWidth(meterEnEntorno((BASE+(pwmUp*1000))));
            motoresArray[1].setPulseWidth(meterEnEntorno((BASE+(pwmDown*1000))));
            motoresArray[2].setPulseWidth(meterEnEntorno((BASE+(pwmRight*1000))));
            motoresArray[3].setPulseWidth(meterEnEntorno((BASE+(pwmLeft*1000))));
            show(String.valueOf(meterEnEntorno((BASE+(pwmUp*1000)))));


        }

        public float meterEnEntorno(float value){
            if (value <= 1860 && value >= 1060){
                return value;
            }
            else if(value>1860){
                return 1860;
            }
            else {
                return 1060;
            }
        }
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void show(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sBarIT.setText(message);
            }
        });
    }
}
