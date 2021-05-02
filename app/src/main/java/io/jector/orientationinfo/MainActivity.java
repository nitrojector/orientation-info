package io.jector.orientationinfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener {
    // Raw value display
    TextView calMagRawTxtX;
    TextView calMagRawTxtY;
    TextView calMagRawTxtZ;
    TextView gyroRawTxtX;
    TextView gyroRawTxtY;
    TextView gyroRawTxtZ;

    // Log columns
    TextView magLogCol0;
    TextView magLogCol1;
    TextView magLogCol2;
    TextView gyroLogCol0;
    TextView gyroLogCol1;
    TextView gyroLogCol2;

    // Data angular representation views
    ImageView calMagByXY;
    ImageView calMagByYZ;
    ImageView calMagByZX;
    ImageView gyroByX;
    ImageView gyroByY;
    ImageView gyroByZ;

    // Manipulation
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch updateVals;
    boolean updateStatus = false;
    Button recordValsButt;
    Button resetGyroDisp;
    Button clearLogs;

    // System time
    long lastSensorUpdateSysTime = System.currentTimeMillis();
    long lastGyroChangeTime = System.currentTimeMillis();

    // Constants / Params
    final float LPF_ALPHA = 0.25f; // LPF filter Alpha

    // Labels
    final String[] axesLabels = {"X", "Y", "Z"};
    final String[] vectSumLabels = {"XY", "YZ", "ZX"};

    // Raw sensor data record
    float[] calMagDataRaw = {0, 0, 0};
    double[] calMagAng = {0, 0, 0};
    float[] gyroDataRaw = {0, 0, 0};
    double[] gyroAng = {0, 0, 0};

    // Interfaces
    SensorManager sensorManager;

    // Sensor processed value records
    ArrayList<BigDecimal> calMagRecXY = new ArrayList<>();
    ArrayList<BigDecimal> calMagRecYZ = new ArrayList<>();
    ArrayList<BigDecimal> calMagRecZX = new ArrayList<>();
    ArrayList<BigDecimal> gyroRecX = new ArrayList<>();
    ArrayList<BigDecimal> gyroRecY = new ArrayList<>();
    ArrayList<BigDecimal> gyroRecZ = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_main);

        // Register listeners
        sensorManager.registerListener((SensorEventListener) this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener((SensorEventListener) this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        // Sensor visual processed representation
        calMagByXY = (ImageView) findViewById(R.id.calMagByXY);
        calMagByYZ = (ImageView) findViewById(R.id.calMagByYZ);
        calMagByZX = (ImageView) findViewById(R.id.calMagByZX);
        gyroByX = (ImageView) findViewById(R.id.gyroByX);
        gyroByY = (ImageView) findViewById(R.id.gyroByY);
        gyroByZ = (ImageView) findViewById(R.id.gyroByZ);

        // Sensor direct reading displays
        calMagRawTxtX = (TextView) findViewById(R.id.calMagTxtX);
        calMagRawTxtY = (TextView) findViewById(R.id.calMagTxtY);
        calMagRawTxtZ = (TextView) findViewById(R.id.calMagTxtZ);
        gyroRawTxtX = (TextView) findViewById(R.id.gyroTxtX);
        gyroRawTxtY = (TextView) findViewById(R.id.gyroTxtY);
        gyroRawTxtZ = (TextView) findViewById(R.id.gyroTxtZ);

        // Value recording functionality
        recordValsButt = (Button) findViewById(R.id.recordButton);
        recordValsButt.setOnClickListener(this);
        resetGyroDisp = (Button) findViewById(R.id.resetButton);
        resetGyroDisp.setOnClickListener(this);
        clearLogs = (Button) findViewById(R.id.clearLogs);
        clearLogs.setOnClickListener(this);

        // Logging views
        magLogCol0 = (TextView) findViewById(R.id.magLogCol0);
        magLogCol1 = (TextView) findViewById(R.id.magLogCol1);
        magLogCol2 = (TextView) findViewById(R.id.magLogCol2);
        gyroLogCol0 = (TextView) findViewById(R.id.gyroLogCol0);
        gyroLogCol1 = (TextView) findViewById(R.id.gyroLogCol1);
        gyroLogCol2 = (TextView) findViewById(R.id.gyroLogCol2);

        // Value updating switch
        updateVals = (Switch) findViewById(R.id.updateSwitch);
        updateVals.setChecked(updateStatus);
        updateVals.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateStatus = isChecked;
            }
        });

        // Display records if previously displayed
        setViewsDisp(vectSumLabels, "°", new ArrayList[]{calMagRecXY, calMagRecYZ, calMagRecZX}, new TextView[]{magLogCol0, magLogCol1, magLogCol2});
        setViewsDisp(axesLabels, "°", new ArrayList[]{gyroRecX, gyroRecY, gyroRecZ}, new TextView[]{gyroLogCol0, gyroLogCol1, gyroLogCol2});
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.recordButton) {
            // Record current data
            calMagRecXY.add(0, round((float) Math.toDegrees(Math.atan2(calMagDataRaw[0], calMagDataRaw[1])), 2));
            calMagRecYZ.add(0, round((float) Math.toDegrees(Math.atan2(calMagDataRaw[1], calMagDataRaw[2])), 2));
            calMagRecZX.add(0, round((float) Math.toDegrees(Math.atan2(calMagDataRaw[2], calMagDataRaw[0])), 2));
            // Remove extra lines
            if (calMagRecXY.size() > R.integer.maxLinesRec) {
                calMagRecXY.remove(calMagRecXY.size() - 1);
                calMagRecYZ.remove(calMagRecYZ.size() - 1);
                calMagRecZX.remove(calMagRecZX.size() - 1);
            }
            gyroRecX.add(0, round((float) Math.toDegrees(gyroAng[0]), 2));
            gyroRecY.add(0, round((float) Math.toDegrees(gyroAng[1]), 2));
            gyroRecZ.add(0, round((float) Math.toDegrees(gyroAng[2]), 2));
            // Remove extra lines
            if (gyroRecX.size() > R.integer.maxLinesRec) {
                gyroRecX.remove(calMagRecXY.size() - 1);
                gyroRecY.remove(calMagRecYZ.size() - 1);
                gyroRecZ.remove(calMagRecZX.size() - 1);
            }
            // Display data
            setViewsDisp(vectSumLabels, "°", new ArrayList[]{calMagRecXY, calMagRecYZ, calMagRecZX}, new TextView[]{magLogCol0, magLogCol1, magLogCol2});
            setViewsDisp(axesLabels, "°", new ArrayList[]{gyroRecX, gyroRecY, gyroRecZ}, new TextView[]{gyroLogCol0, gyroLogCol1, gyroLogCol2});
        } else if (v.getId() == R.id.resetButton) {
            // Reset gyro angle
            gyroAng = new double[]{0, 0, 0};
        } else if (v.getId() == R.id.clearLogs) {
            clearRecLogs();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Process data
        if (updateVals.isChecked()) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                long tDiff = System.currentTimeMillis() - lastGyroChangeTime;
                lastGyroChangeTime = System.currentTimeMillis();
                gyroDataRaw = lowPass(event.values.clone(), gyroDataRaw);
                gyroAng[0] += tDiff / 1000.0 * gyroDataRaw[0];
                gyroAng[1] += tDiff / 1000.0 * gyroDataRaw[1];
                gyroAng[2] += tDiff / 1000.0 * gyroDataRaw[2];
                gyroDataRaw = event.values.clone();
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                calMagDataRaw = lowPass(event.values.clone(), calMagDataRaw);
                calMagAng[0]= Math.atan2(calMagDataRaw[0], calMagDataRaw[1]);
                calMagAng[1] = Math.atan2(calMagDataRaw[1], calMagDataRaw[2]);
                calMagAng[2] = Math.atan2(calMagDataRaw[2], calMagDataRaw[0]);
            }
            lastSensorUpdateSysTime = System.currentTimeMillis();
        }
        // Set display for raw data
        setViewsDisp(axesLabels, "μT", round(calMagDataRaw, 4), new TextView[]{calMagRawTxtX, calMagRawTxtY, calMagRawTxtZ});
        setViewsDisp(axesLabels, "rad/s", round(gyroDataRaw, 4), new TextView[]{gyroRawTxtX, gyroRawTxtY, gyroRawTxtZ});
        // Set display for visual angle rotation
        calMagByXY.setRotation((float) Math.toDegrees(calMagAng[0]));
        calMagByYZ.setRotation((float) Math.toDegrees(calMagAng[1]));
        calMagByZX.setRotation((float) Math.toDegrees(calMagAng[2]));
        gyroByX.setRotation((float) Math.toDegrees(gyroAng[0]));
        gyroByY.setRotation((float) Math.toDegrees(gyroAng[1]));
        gyroByZ.setRotation((float) Math.toDegrees(gyroAng[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Unused
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener((SensorEventListener) this);
    }

    // Displays sets of numerical data to a set of multiline TextViews
    public void setViewsDisp(String[] labels, String unit, ArrayList<BigDecimal>[] values, TextView[] columns) {
        if (!(labels.length == values.length && values.length == columns.length))
            Log.d("ERROR", "Set display failed, lists' length do not match");
        for (int i = 0; i < values.length; i++) {
            String dispStr = "";
            for (BigDecimal data : values[i]) {
                dispStr += labels[i] + ": " + addPlusSign(data) + unit + "\n";
            }
            columns[i].setText(dispStr);
        }
    }

    // Displays one set of data to a set of TextViews
    public void setViewsDisp(String[] labels, String unit, BigDecimal[] values, TextView[] columns) {
        if (!(labels.length == values.length && values.length == columns.length))
            Log.d("ERROR", "Set display failed, lists' length do not match");
        for (int i = 0; i < values.length; i++) {
            columns[i].setText(labels[i] + ": " + addPlusSign(values[i]) + unit + "\n");
        }
    }

    // A simple implementation of the Low Pass Filter (LPF)
    public float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + LPF_ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    // Rounds an array of floats to an array of BigDecimal objects with certain decimal places
    public static BigDecimal[] round(float[] d, int decimalPlace) {
        BigDecimal[] bdarr = new BigDecimal[d.length];
        for (int i = 0; i < d.length; i++) {
            BigDecimal bd = new BigDecimal(d[i] + "");
            bdarr[i] = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        }
        return bdarr;
    }

    // Rounds a float to BigDecimal object with certain decimal places
    public static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(d + "");
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    // Adds positive sign to positive numbers for displaying
    public String addPlusSign(BigDecimal num) {
        if (num.compareTo(new BigDecimal(0)) != -1) {
            return "+" + num;
        }
        return "" + num;
    }

    // Clears all logs logged by user
    public void clearRecLogs() {
        // Clear variables
        calMagRecXY.clear();
        calMagRecYZ.clear();
        calMagRecZX.clear();
        gyroRecX.clear();
        gyroRecY.clear();
        gyroRecZ.clear();
        // Clear display
        setViewsDisp(vectSumLabels, "°", new ArrayList[]{calMagRecXY, calMagRecYZ, calMagRecZX}, new TextView[]{magLogCol0, magLogCol1, magLogCol2});
        setViewsDisp(axesLabels, "°", new ArrayList[]{gyroRecX, gyroRecY, gyroRecZ}, new TextView[]{gyroLogCol0, gyroLogCol1, gyroLogCol2});
    }
}