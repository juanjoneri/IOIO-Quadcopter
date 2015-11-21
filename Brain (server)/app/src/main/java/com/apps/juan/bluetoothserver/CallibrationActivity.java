package com.apps.juan.bluetoothserver;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class CallibrationActivity extends IOIOActivity {

    int pulseWidth = 0;
    Button bMax, bMin;
    TextView textView;
    IOIO ioio;
    boolean isIoioConnected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callibration);

        bMax = (Button) findViewById(R.id.button_max);
        bMin = (Button) findViewById(R.id.button_min);

        textView = (TextView) findViewById(R.id.pulseView);

        bMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pulseWidth = 1860;
                textView.setText(String.valueOf(pulseWidth));
            }
        });

        bMin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pulseWidth = 1060;
                textView.setText(String.valueOf(pulseWidth));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        if(isIoioConnected){
            ioio.disconnect();
        }
        pulseWidth = 0;
        super.onStop();

    }

    private class Looper extends BaseIOIOLooper {
        private PwmOutput motorUp, motorDown, motorLeft, motorRight;
        private PwmOutput[] motoresArray;

        @Override
        protected void setup() throws ConnectionLostException, InterruptedException {
            ioio = ioio_;
            isIoioConnected = true;
            toast("Set Up");
            motorUp = ioio_.openPwmOutput(1, 400);
            motorDown = ioio_.openPwmOutput(2, 400);
            motorRight = ioio_.openPwmOutput(3, 400);
            motorLeft = ioio_.openPwmOutput(4, 400);

            motoresArray = new PwmOutput[] {motorUp, motorDown, motorRight, motorLeft};

            super.setup();
        }

        @Override
        public void disconnected() {
            ioio = null;
            isIoioConnected = false;
            toast("exit callibration");
            for (PwmOutput o : motoresArray ){
                o.close();
            }
            super.disconnected();
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {

            motorUp.setPulseWidth(pulseWidth);
            motorDown.setPulseWidth(pulseWidth);
            motorRight.setPulseWidth(pulseWidth);
            motorLeft.setPulseWidth(pulseWidth);
            Thread.sleep(100);
        }

        public Looper() {
            super();
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
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
