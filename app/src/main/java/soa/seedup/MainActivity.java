package soa.seedup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import org.json.JSONObject;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothConnectionListener;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.OnDataReceivedListener;
import app.akexorcist.bluetotohspp.library.DeviceList;
import app.akexorcist.bluetotohspp.library.BluetoothSPP.BluetoothStateListener;

public class MainActivity extends AppCompatActivity {

    private TextView txtLuz, txtHumedad, txtTemperatura;
    private BluetoothSPP bt;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hace el binding de los controles a variables
        txtLuz = (TextView)findViewById(R.id.txtLuz);
        txtHumedad = (TextView)findViewById(R.id.txtHumedad);
        txtTemperatura = (TextView)findViewById(R.id.txtTemperatura);

        // Instancia la clase de bluetooth
        bt = new BluetoothSPP(this);

        // Chequea que el dispositivo en el cual esta corriendo la app tenga bluetooth
        if(!bt.isBluetoothAvailable()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Este dispositivo no dispone de bluetooth por lo que no es posible ejecutar esta aplicación.");
            builder.setCancelable(false);
            builder.setPositiveButton(
                "Cerrar aplicacion",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {

            // Listener para cuando se recibe info desde este adaptador bluetooth
            bt.setOnDataReceivedListener(new OnDataReceivedListener() {
                public void onDataReceived(byte[] data, String message) {

                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    try
                    {
                        JSONObject jsonMessage = new JSONObject(message);

                        if( jsonMessage.has("sensor-luz") )
                            txtLuz.setText( jsonMessage.getString("sensor-luz") );

                        if( jsonMessage.has("sensor-humedad") )
                            txtHumedad.setText( jsonMessage.getString("sensor-humedad") );

                        if( jsonMessage.has("sensor-temperatura") )
                            txtTemperatura.setText( jsonMessage.getString("sensor-temperatura") );
                    }
                    catch (Exception e){
                        Toast.makeText(getApplicationContext(), "Problema al parsear JSON", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Listeners para cuando la conexion bluetooth cambia de estado
            bt.setBluetoothConnectionListener(new BluetoothConnectionListener() {
                public void onDeviceConnected(String name, String address) {
                    Toast.makeText(getApplicationContext(), "Device onDeviceConnected()", Toast.LENGTH_SHORT).show();
                }
                public void onDeviceDisconnected() {
                    Toast.makeText(getApplicationContext(), "Device onDeviceDisconnected()", Toast.LENGTH_SHORT).show();
                }
                public void onDeviceConnectionFailed() {
                    Toast.makeText(getApplicationContext(), "Device onDeviceConnectionFailed()", Toast.LENGTH_SHORT).show();
                }
            });

            // Listeners para cuando el dispositivo bluetooth cambia de estado
            bt.setBluetoothStateListener(new BluetoothStateListener() {
                public void onServiceStateChanged(int state) {
                    if(state == BluetoothState.STATE_CONNECTED)
                        Toast.makeText(getApplicationContext(), "State : STATE_CONNECTED", Toast.LENGTH_SHORT).show();
                    else if(state == BluetoothState.STATE_CONNECTING)
                        Toast.makeText(getApplicationContext(), "State : STATE_CONNECTING", Toast.LENGTH_SHORT).show();
                    else if(state == BluetoothState.STATE_LISTEN)
                        Toast.makeText(getApplicationContext(), "State : STATE_LISTEN", Toast.LENGTH_SHORT).show();
                    else if(state == BluetoothState.STATE_NONE)
                        Toast.makeText(getApplicationContext(), "State : STATE_NONE", Toast.LENGTH_SHORT).show();
                }
            });

            // Boton para conectarse/desconectarse
            Button btnConnect = (Button) findViewById(R.id.btnConnect);
            btnConnect.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    if(bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                        bt.disconnect();
                        Toast.makeText(getApplicationContext(),"BT desconectar",Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                        Toast.makeText(getApplicationContext(),"BT conectar a...",Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Binding para el switch de luz
            Switch switchLuz = (Switch) findViewById(R.id.switchLuz);
            switchLuz.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sendToBluetooth( isChecked ? "luz-1" : "luz-0" );
                }
            });

            // Binding para el switch de riego
            Switch switchRiego = (Switch)findViewById(R.id.switchRiego);
            switchRiego.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    sendToBluetooth( isChecked ? "riego-1" : "riego-0" );
                }
            });
        }
    }

    public void onDestroy() {
        super.onDestroy();
        bt.stopService();
        System.exit(0);
    }

    public void onStart() {
        super.onStart();
        if(bt.isBluetoothAvailable()) {
            if (!bt.isBluetoothEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
                Toast.makeText(getApplicationContext(), "BT on request", Toast.LENGTH_SHORT).show();
            } else {
                if (!bt.isServiceAvailable()) {
                    Toast.makeText(getApplicationContext(), "BT setupService()", Toast.LENGTH_SHORT).show();
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
                    bt.autoConnect("HC-06");
                }
            }
        }
    }

    public void sendToBluetooth( String mensaje ) {
        if( bt.isBluetoothAvailable() ) {
            if( bt.isBluetoothEnabled() ) {
                if( bt.getServiceState() == BluetoothState.STATE_CONNECTED )
                    bt.send( mensaje, true );
            }
        }
        Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch( requestCode ) {
            case BluetoothState.REQUEST_CONNECT_DEVICE:
                if(resultCode == Activity.RESULT_OK) {
                    bt.connect(data);
                    Toast.makeText(getApplicationContext(), "BT connect()", Toast.LENGTH_SHORT).show();
                }
                break;

            case BluetoothState.REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(),"BT setupService()",Toast.LENGTH_SHORT).show();
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);
                    bt.autoConnect("HC-06");
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("No es posible utilizar esta aplicación sin el bluetooth activado. Para poder continar debe habilitar el bluetooth.");
                    builder.setCancelable(false);
                    builder.setPositiveButton(
                            "Continuar",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                    startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                break;
        }
    }
}
