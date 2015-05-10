/**
*
*Creado por @mpijierro
*/
package stick.eyes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;

import stick.eyes.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import java.text.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
 
public class MainActivity extends Activity {
	
  private static final String TAG = "StickEyes";
  private static final float defaultSpeed = 0.55F;		//Velocidad de la persona en metros por segundo, por defecto, 0.55 m/s
  private static final int defaultMinDistance = 100;	//Distancia mínima en centímetrospara el primer aviso, por defecto, 100 cms
   
  Button btnSettings;
  Button btnFinish;
  Button btnReconnect;
  TextView txtArduino;
  Handler h;
  
  private EditText textSpeed;		//Contenido del campo speed del formulario
  private EditText textMinDistance;	//Contenido del campo minDistance del formulario
  private float speed = 0.55F;		
  private int minDistance = 100;  	
   
  final int RECIEVE_MESSAGE = 1;	// Status  for Handler
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  
  private StringBuilder sb = new StringBuilder();
  
  private ConnectedThread mConnectedThread;
   
  // SPP UUID service
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
  // Dirección MAC del módulo bluetooth del Arduino
  private static String address = "98:D3:31:B2:19:22";
   
  /** LLamada cuando la actividad es creada. */
  @Override  
  public void onCreate(Bundle savedInstanceState) {
	  
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // Inicialiamos los elementos de la vista
    btnSettings = (Button) findViewById(R.id.btnSettingSend);
    btnFinish = (Button) findViewById(R.id.btnFinish);
    btnReconnect = (Button) findViewById(R.id.btnReconnect);
    
    textSpeed = (EditText) findViewById(R.id.speed);
    textMinDistance = (EditText) findViewById(R.id.minDistance);
    txtArduino = (TextView) findViewById(R.id.txtArduino);

    // Listener click en el botón de guardar la configuración
    btnSettings.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
        	
        	String speedString = "";
        	String distanceString = "";
        	
    		// Configurar el valor de la velocidad
        	if (textSpeed.getText().toString().trim().length() == 0){
        		speedString = Float.toString(defaultSpeed);        		
        	}
        	else{
        		speedString = textSpeed.getText().toString().trim();
        	}
        	
        	// Configurar el valor de la distancia
        	if (textMinDistance.getText().toString().trim().length() == 0){
        		distanceString = Float.toString(defaultMinDistance);        		
        	}
        	else{
        		distanceString = textMinDistance.getText().toString().trim();
        	}
        
        	int timeSleep = (int) Math.ceil(Float.parseFloat(distanceString) /  (Float.parseFloat(speedString)*100));
        	
        	// Enviamos los datos a Arduino
        	// El caracter '#' detecta en Arduino el final de la cadena enviada
        	
        	// Enviamos el tiempo de 'dormir' del arduino para ahorrar energía (en segundos)
        	String stringToSend = "t"+Integer.toString(timeSleep)+"#";
      	  	mConnectedThread.write(stringToSend);
      	  	
      	  	// Enviamos la distancia mínima para detectar obstáculos a Arduino
      	  	mConnectedThread.write("d"+distanceString+"#");
      	  	
      	  	showMessage ("t"+stringToSend+"#"+" "+"d"+distanceString+"#");
      	  	showMessage ("Configuración modificada");
        	
        }
    });
    
    //Implementación del botón "Salir"
    btnFinish.setOnClickListener(new OnClickListener() {
           
    	public void onClick(View v) {
              setResult(RESULT_OK);
              mConnectedThread.cancel(btSocket);
              finish();
    	}
    });
    
    //Implementación del botón "reconectar" la conexión bluetooth
    btnReconnect.setOnClickListener(new OnClickListener() {
           
    	public void onClick(View v) {
              showMessage ("Por implementar...");
    	}
    });
    
    //createHandler();
    h = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		
    		Vibrator mVibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    		
    		switch (msg.what) {
            	case RECIEVE_MESSAGE:													// if receive massage
            	
	            	byte[] readBuf = (byte[]) msg.obj;
	            	String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
	            	sb.append(strIncom);												// append string
	            	
	            	int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
	            	
	            	if (endOfLineIndex > 0) { 											// if end-of-line,

	            		String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
	            		String sbprint = sb.substring(0, endOfLineIndex);				// extract string
	            		
	            		sb.delete(0, sb.length());										// and clear
	                    txtArduino.setText("Time:" +currentDateTimeString + " # Data from Arduino: " + sbprint); 	        
	                		                    
	                	mVibrator.vibrate(300);
	                	
	                }
	            	//Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
	            	break;
    		}
        };
	};
	
	btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
    checkBTState();
	
    	
  }
  
  @Override
  public void onResume() {
    super.onResume();
 
    Log.d(TAG, "...onResume - try connect...");
   
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
   
	try {
		btSocket = createBluetoothSocket(device);
	} catch (IOException e) {
		errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
	}
    
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
   
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
      Log.d(TAG, "....Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
     
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
   
    mConnectedThread = new ConnectedThread(btSocket);
    mConnectedThread.start();
  }
 
  @Override
  public void onPause() {
    super.onPause();
 
    Log.d(TAG, "...In onPause()...");
  
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }
  
  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      if(Build.VERSION.SDK_INT >= 10){
          try {
              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
              return (BluetoothSocket) m.invoke(device, MY_UUID);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
  }
   
   
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }
 
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    
    finish();
  }
  
  private void showMessage(String theMsg) {
      Toast msg = Toast.makeText(getBaseContext(),
              theMsg, (Toast.LENGTH_LONG)/160);
      msg.show();
  }
 
  /**
   * Clase privada para manejar el hilo de conexión del Bluetooth
   * @author mpijierro
   *
   */
  private class ConnectedThread extends Thread {
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[256];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	        	try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(String message) {
	    	Log.d(TAG, "...Data to send: " + message + "...");
	    	byte[] msgBuffer = message.getBytes();
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
	          }
	    }
	    
	    public void cancel(BluetoothSocket mmSocket)
        {
            if (mmOutStream != null)
            {
                try {mmOutStream.close();} catch (Exception e) { Log.e(TAG, "close() of outputstream failed", e); }
                //mmOutStream = null;
            }

            if (mmInStream != null)
            {
                try {mmInStream.close();} catch (Exception e) { Log.e(TAG, "close() of inputstream failed", e); }
                //mmInStream = null;
            }

            if (mmSocket != null)
            {
                try {mmSocket.close();} catch (Exception e) { Log.e(TAG, "close() of connect socket failed", e); }
                mmSocket = null;
            }
        }
	}
}
