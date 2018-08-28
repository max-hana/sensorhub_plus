/*
 * Copyright 2018 BrainPad Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.sensorhub.collector;

import android.util.Log;

import com.example.androidthings.sensorhub.SensorData;
import com.leinardi.android.things.driver.lsm9ds1.Lsm9ds1;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lsm9ds1Collector implements SensorCollector {

    private static final String TAG = Lsm9ds1Collector.class.getSimpleName();

    private static final String SENSOR_ACCEL = "ACCEL";
    private static final String SENSOR_ACCEL_X = "ACCEL_X";
    private static final String SENSOR_ACCEL_Y = "ACCEL_Y";
    private static final String SENSOR_ACCEL_Z = "ACCEL_Z";

    private static final String SENSOR_GYROL = "GYRO";
    private static final String SENSOR_GYRO_X = "GYRO_X";
    private static final String SENSOR_GYRO_Y = "GYRO_Y";
    private static final String SENSOR_GYRO_Z = "GYRO_Z";

    private static final String SENSOR_MAG = "MAG";
    private static final String SENSOR_MAG_X = "MAG_X";
    private static final String SENSOR_MAG_Y = "MAG_Y";
    private static final String SENSOR_MAG_Z = "MAG_Z";

    private static final String SENSOR_TEMPERATURE = "TEMP_LSM9DS1";


    private boolean isAccelerometerEnabled;
    private boolean isGyroEnabled;
    private boolean isMagnetEnabled;


    private String i2cBus;
    private Lsm9ds1 lsm9ds1;

    public Lsm9ds1Collector(String i2cBus) {
        this.i2cBus = i2cBus;
        // By default, enable all available sensors. Different initial state can be set by calling
        // setEnabled before activate.
        this.isAccelerometerEnabled = true;
        this.isGyroEnabled = true;
        this.isMagnetEnabled = true;
    }

    @Override
    public boolean activate() {
        if (lsm9ds1 != null) {
            return true;
        }
        try {
            lsm9ds1 = new Lsm9ds1.Builder(this.i2cBus) // All the following setters are optional
                    .setI2cAddressAccelGyro(Lsm9ds1.I2C_ADDRESS_ACCEL_GYRO)
                    .setI2cAddressMag(Lsm9ds1.I2C_ADDRESS_MAG)
                    .setAccelerometerDecimation(Lsm9ds1.AccelerometerDecimation.ACCEL_DEC_0_SAMPLES)
                    .setAccelerometerEnabledAxes(Lsm9ds1.ACCEL_AXIS_X | Lsm9ds1.ACCEL_AXIS_Y | Lsm9ds1.ACCEL_AXIS_Z)
                    .setAccelerometerHighResolution(true)
                    .setAccelerometerOdr(Lsm9ds1.AccelGyroOutputDataRate.ODR_952HZ)
                    .setAccelerometerRange(Lsm9ds1.AccelerometerRange.ACCEL_RANGE_2G)
                    .setFifoMemoryEnabled(false)
                    .setFifoModeAndTreshold(Lsm9ds1.FifoMode.FIFO_OFF, Lsm9ds1.FIFO_MAX_THRESHOLD)
                    .setGyroscopeOdr(Lsm9ds1.AccelGyroOutputDataRate.ODR_952HZ)
                    .setGyroscopeScale(Lsm9ds1.GyroscopeScale.GYRO_SCALE_245DPS)
                    .setMagnetometerGain(Lsm9ds1.MagnetometerGain.MAG_GAIN_4GAUSS)
                    .setMagnetometerSystemOperatingMode(Lsm9ds1.MagnetometerSystemOperatingMode.MAG_CONTINUOUS_CONVERSION)
                    .setMagnetometerTemperatureCompensation(true)
                    .setMagnetometerXYOperatingMode(Lsm9ds1.MagnetometerXYOperatingMode.MAG_XY_OM_ULTRA_HIGH_PERFORMANCE)
                    .setMagnetometerZOperatingMode(Lsm9ds1.MagnetometerZOperatingMode.MAG_Z_OM_ULTRA_HIGH_PERFORMANCE)
                    .build();
            Log.d(TAG, "Lsm9ds1 initialized");
            return true;
        } catch (Throwable t) {
            Log.i(TAG, "Could not initialize LSM9DS1 sensor on I2C bus " + i2cBus, t);
        }
        return false;
    }

    @Override
    public void setEnabled(String sensor, boolean enabled) {
        Log.w(TAG, "Cannot set sensor " + sensor + " to " + enabled + ". Ignoring request");
    }


    @Override
    public boolean isEnabled(String sensor) {
        switch (sensor) {
            case SENSOR_ACCEL:
                return isAccelerometerEnabled;
            case SENSOR_GYROL:
                return isGyroEnabled;
            case SENSOR_MAG:
                return isMagnetEnabled ;
            default:
                Log.w(TAG, "Unknown sensor " + sensor + ". Ignoring request");
        }
        return false;
    }

    @Override
    public List<String> getAvailableSensors() {
        List<String> sensors = new ArrayList<>();
        sensors.add(SENSOR_ACCEL);
        sensors.add(SENSOR_GYROL);
        sensors.add(SENSOR_MAG);
        return sensors;
    }

    @Override
    public List<String> getEnabledSensors() {
        List<String> sensors = new ArrayList<>();
        if (isEnabled(SENSOR_ACCEL)) {
            sensors.add(SENSOR_ACCEL);
        }
        if (isEnabled(SENSOR_GYROL)) {
            sensors.add(SENSOR_GYROL);
        }
        if (isEnabled(SENSOR_MAG)) {
            sensors.add(SENSOR_MAG);
        }
        return sensors;
    }

    @Override
    public void collectRecentReadings(List<SensorData> output) {
        if (lsm9ds1 == null) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (isEnabled(SENSOR_ACCEL) ) {
                // If both temperature and pressure are enabled, we can read both with a single
                // I2C read, so we will report both values with the same timestamp


                float[] acceleration = lsm9ds1.readAcceleration();

                output.add(new SensorData(now, SENSOR_ACCEL_X, acceleration[0]));
                output.add(new SensorData(now, SENSOR_ACCEL_Y, acceleration[1]));
                output.add(new SensorData(now, SENSOR_ACCEL_Z, acceleration[2]));

            }
            if (isEnabled(SENSOR_GYROL) ) {
                // If both temperature and pressure are enabled, we can read both with a single
                // I2C read, so we will report both values with the same timestamp


                float[] angularVelocity = lsm9ds1.readAngularVelocity();
                float temperature = lsm9ds1.readTemperature();

                output.add(new SensorData(now, SENSOR_GYRO_X, angularVelocity[0]));
                output.add(new SensorData(now, SENSOR_GYRO_Y, angularVelocity[1]));
                output.add(new SensorData(now, SENSOR_GYRO_Z, angularVelocity[2]));


            }

            if (isEnabled(SENSOR_MAG) ) {
                // If both temperature and pressure are enabled, we can read both with a single
                // I2C read, so we will report both values with the same timestamp

                float[] magneticInduction = lsm9ds1.readMagneticInduction();

                output.add(new SensorData(now, SENSOR_MAG_X, magneticInduction[0]));
                output.add(new SensorData(now, SENSOR_MAG_Y, magneticInduction[1]));
                output.add(new SensorData(now, SENSOR_MAG_Z, magneticInduction[2]));

            }

            float temperature = lsm9ds1.readTemperature();
            output.add(new SensorData(now, SENSOR_TEMPERATURE, temperature));


        } catch (Throwable t) {
            Log.w(TAG, "Cannot collect BMx280 data. Ignoring it for now", t);
        }
    }

    @Override
    public void closeQuietly() {
        if (lsm9ds1 != null) {
            try {
                lsm9ds1.close();
            } catch (IOException e) {
                // close quietly
            }
            lsm9ds1 = null;
        }
    }
}
