package com.example.ledblinky;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbManager;
import android.media.ExifInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.ledblinky.MVVM.VM.NPNHomeViewModel;
import com.example.ledblinky.MVVM.View.NPNHomeView;
import com.example.ledblinky.Network.ApiResponseListener;
import com.example.ledblinky.Network.VolleyRemoteApiClient;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileStreamFactory;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements NPNHomeView {

  private static final String TAG = "NPNIoTs";


  // UART Configuration Parameters
  private static final int BAUD_RATE = 115200;
  private static final int DATA_BITS = 8;
  private static final int STOP_BITS = 1;
  private UartDevice mUartDevice;
  private int CHUNK_SIZE = 512;


  //Timer for periodic uploading data
  Timer mBlinkyTimer;
  Timer updateWifiTimer;

  //An instance for network access
  NPNHomeViewModel mHomeViewModel;

  //Display IP Address of the device
  TextView txtIPAddress;

  // Since we only final variables are accessible in anonymous class,
  // we need to make final arrays to hold our variables
  // Final array to hold variable for temperature
  final String[] temperature = new String[1];
  String temperature_shared_pref_key = "temperature";
  // Final array to hold variable for temperature
  final String[] light = new String[1];
  String light_shared_pref_key = "light";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Setup timer to continuously update timer
    updateWifiTimer = new Timer();
    TimerTask updateWifiInfoTask = new TimerTask() {
      @Override
      public void run() {
        try {

          runOnUiThread(new Runnable() {

            @Override
            public void run() {

              WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
              String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
              txtIPAddress = findViewById(R.id.txtIPAddress);
              txtIPAddress.setText("IP: " + ip);
            }
          });


        } catch (Exception e){
          e.printStackTrace();
        }
      }
    };
    updateWifiTimer.schedule(updateWifiInfoTask,0, 10000);

    // Populate variable to send with last known data from microbit
    SharedPreferences sharedPrefTemperature = this.getSharedPreferences(
      getString(R.string.rasp_pi_shared_pref_key), Context.MODE_PRIVATE);
    temperature[0] = sharedPrefTemperature.getString(temperature_shared_pref_key, "0");
    light[0] = sharedPrefTemperature.getString(light_shared_pref_key, "0");
    Log.d(TAG, "Last known temperature: " + temperature[0]);
    Log.d(TAG, "Last known light: " + light[0]);

    //An instance for network access
    mHomeViewModel = new NPNHomeViewModel();
    mHomeViewModel.attach(this, this);
    initUart();
    setupBlinkyTimer();
  }


  @Override
  public void onSuccessUpdateServer(String message) {
    //txtConsole.setText("Request server is successful");
    Log.d(TAG, "Request server is successful");
  }

  @Override
  public void onErrorUpdateServer(String message) {

    Log.d(TAG, "Request server is fail");
  }

  private void updateVariables(String data){
    String[] tokens = data.toUpperCase().split("-");
    SharedPreferences sharedPref = this.getSharedPreferences(
      getString(R.string.rasp_pi_shared_pref_key), Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPref.edit();
    Log.d(TAG, "Data: " + data);
    switch (tokens[0]){
      case "TEMP":
        temperature[0] = tokens[1];
        editor.putString(temperature_shared_pref_key, tokens[1]);
        editor.commit();
        Log.d(TAG, "Temperature: " + tokens[1]);
        break;
      case "LIGHT":
        light[0] = tokens[1];
        editor.putString(light_shared_pref_key, tokens[1]);
        editor.commit();
        Log.d(TAG, "Light: " + tokens[1]);
        break;
    }
  }

  private void sendDataToThingSpeak(String value1, String value2) {
    OkHttpClient okHttpClient = new OkHttpClient();
    Request.Builder builder = new Request.Builder();
    String API_KEY = "BF7R9E1KD7FQXQ95";
    Request request = builder.url("https://api.thingspeak.com/update?api_key=" + API_KEY + "&field1=" + value1 + "&field2=" + value2).build();

    okHttpClient.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Request request, IOException e) {
        Log.d(TAG, "Exception : " + e);
      }

      @Override
      public void onResponse(Response response) throws IOException {
        Log.d(TAG, "Response is : " + response);
      }
    });
  }

  private void setupBlinkyTimer() {
    mBlinkyTimer = new Timer();
    TimerTask temperatureTask = new TimerTask() {
      @Override
      public void run() {
        sendDataToThingSpeak(temperature[0], light[0]);
      }
    };

    mBlinkyTimer.schedule(temperatureTask, 10000, 30000);
  }

  private void initUart() {
    try {
      openUart("UART0", BAUD_RATE);
      Log.d(TAG, "Init uart successful");
    } catch (IOException e) {
      Log.d(TAG, "Error on UART API");
    }
  }

  public void writeUartData(UartDevice uart) {
    Log.d(TAG, "writeUartData: HAAAAAAAAAAAAAAAAAAAAAAAAAAAAa");
    try {
      byte[] buffer = {'t'};
      int count = uart.write(buffer, buffer.length);
      Log.d(TAG, "Wrote " + count + " bytes to peripheral");
    } catch (IOException e) {
      Log.d(TAG, "Error on UART");
    }
  }

  /**
   * Callback invoked when UART receives new incoming data.
   */
  private UartDeviceCallback mCallback = new UartDeviceCallback() {
    @Override
    public boolean onUartDeviceDataAvailable(UartDevice uart) {
      //read data from Rx buffer
      try {
        byte[] buffer = new byte[CHUNK_SIZE];
        int noBytes = -1;
        String strData = "";
        while ((noBytes = mUartDevice.read(buffer, buffer.length)) > 0) {
          //Log.d(TAG, "Number of bytes: " + Integer.toString(noBytes));
          String str = new String(buffer, 0, noBytes, "UTF-8");
          //Log.d(TAG, "Buffer is: " + str);
          strData += str;
        }
        try {
          //Log.d(TAG, strData);
          updateVariables(strData);
        }catch (Exception e){
          Log.w(TAG, "Exception", e);
        }
      }catch (IOException e) {
        Log.w(TAG, "Unable to transfer data over UART", e);
      }
      return true;
    }

    @Override
    public void onUartDeviceError(UartDevice uart, int error) {
      Log.w(TAG, uart + ": Error event " + error);
    }
  };

  private void openUart(String name, int baudRate) throws IOException {

    Log.d(TAG, PeripheralManager.getInstance().getUartDeviceList().toString());
    List<String> uartList = PeripheralManager.getInstance().getUartDeviceList();

    mUartDevice = PeripheralManager.getInstance().openUartDevice(name);

    // Configure the UART
    mUartDevice.setBaudrate(baudRate);
    mUartDevice.setDataSize(DATA_BITS);
    mUartDevice.setParity(UartDevice.PARITY_NONE);
    mUartDevice.setStopBits(STOP_BITS);

    mUartDevice.registerUartDeviceCallback(mCallback);
  }

  private void closeUart() throws IOException {
    if (mUartDevice != null) {
      mUartDevice.unregisterUartDeviceCallback(mCallback);
      try {
        mUartDevice.close();
      } finally {
        mUartDevice = null;
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Attempt to close the UART device
    try {
      closeUart();
      mUartDevice.unregisterUartDeviceCallback(mCallback);

    } catch (IOException e) {
      Log.e(TAG, "Error closing UART device:", e);
    }
  }
}

