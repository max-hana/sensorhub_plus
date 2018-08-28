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
import com.google.android.things.contrib.driver.bmx055.Bmx055;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bmx055Collector implements SensorCollector {

    private static final String TAG = Bmx055Collector.class.getSimpleName();

    private static final String SENSOR_TEMPERATURE = "temperature";
    private static final String SENSOR_HUMIDITY = "humidity";
    private static final String SENSOR_PRESSURE = "ambient_pressure";

    private boolean isTemperatureEnabled;
    private boolean isPressureEnabled;
    private boolean isHumidityEnabled;

    private boolean isHumidityAvailable;

    private String i2cBus;
    private Bmx055 bmx055;

    public Bmx055Collector(String i2cBus) {
        this.i2cBus = i2cBus;
        // By default, enable all available sensors. Different initial state can be set by calling
        // setEnabled before activate.
        this.isTemperatureEnabled = true;
        this.isPressureEnabled = true;
        this.isHumidityEnabled = true;
    }

    @Override
    public boolean activate() {
        if (bmx055 != null) {
            return true;
        }
        try {
            bmx055 = new Bmx055(i2cBus);
            isHumidityAvailable = bmx055.hasHumiditySensor();
            setEnabled(SENSOR_TEMPERATURE, isTemperatureEnabled);
            setEnabled(SENSOR_PRESSURE, isPressureEnabled);
            setEnabled(SENSOR_HUMIDITY, isHumidityAvailable && isHumidityEnabled);
            bmx055.setMode(Bmx055.MODE_NORMAL);
            Log.d(TAG, "BMx055 initialized");
            return true;
        } catch (Throwable t) {
            Log.i(TAG, "Could not initialize BMx055 sensor on I2C bus " + i2cBus, t);
        }
        return false;
    }

    @Override
    public void setEnabled(String sensor, boolean enabled) {
        try {
            int overSampling = enabled ? Bmx055.OVERSAMPLING_1X : Bmx055.OVERSAMPLING_SKIPPED;
            switch (sensor) {
                case SENSOR_TEMPERATURE:
                    if (bmx055 != null) {
                        bmx055.setTemperatureOversampling(overSampling);
                    }
                    isTemperatureEnabled = enabled;
                    break;
                case SENSOR_PRESSURE:
                    if (bmx055 != null) {
                        bmx055.setPressureOversampling(overSampling);
                    }
                    isPressureEnabled = enabled;
                    break;
                case SENSOR_HUMIDITY:
                    if (enabled && !isHumidityAvailable) {
                        Log.i(TAG, "Humidity sensor not available. Ignoring request to enable it");
                    } else {
                        if (bmx055 != null && isHumidityAvailable) {
                            bmx055.setHumidityOversampling(overSampling);
                        }
                        isHumidityEnabled = enabled;
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown sensor " + sensor + ". Ignoring request");
            }
        } catch (IOException ex) {
            Log.w(TAG, "Cannot set sensor " + sensor + " to " + enabled + ". Ignoring request", ex);
        }
    }

    @Override
    public boolean isEnabled(String sensor) {
        switch (sensor) {
            case SENSOR_TEMPERATURE:
                return isTemperatureEnabled;
            case SENSOR_PRESSURE:
                return isPressureEnabled;
            case SENSOR_HUMIDITY:
                return isHumidityAvailable && isHumidityEnabled;
            default:
                Log.w(TAG, "Unknown sensor " + sensor + ". Ignoring request");
        }
        return false;
    }

    @Override
    public List<String> getAvailableSensors() {
        List<String> sensors = new ArrayList<>();
        sensors.add(SENSOR_TEMPERATURE);
        sensors.add(SENSOR_PRESSURE);
        if (isHumidityAvailable) {
            sensors.add(SENSOR_HUMIDITY);
        }
        return sensors;
    }

    @Override
    public List<String> getEnabledSensors() {
        List<String> sensors = new ArrayList<>();
        if (isEnabled(SENSOR_TEMPERATURE)) {
            sensors.add(SENSOR_TEMPERATURE);
        }
        if (isEnabled(SENSOR_PRESSURE)) {
            sensors.add(SENSOR_PRESSURE);
        }
        if (isEnabled(SENSOR_HUMIDITY)) {
            sensors.add(SENSOR_HUMIDITY);
        }
        return sensors;
    }

    @Override
    public void collectRecentReadings(List<SensorData> output) {
        if (bmx055 == null) {
            return;
        }
        try {
            if (isEnabled(SENSOR_TEMPERATURE) && isEnabled(SENSOR_PRESSURE)) {
                // If both temperature and pressure are enabled, we can read both with a single
                // I2C read, so we will report both values with the same timestamp
                long now = System.currentTimeMillis();
                float[] data = bmx055.readTemperatureAndPressure();
                output.add(new SensorData(now, SENSOR_TEMPERATURE, data[0]));
                output.add(new SensorData(now, SENSOR_PRESSURE, data[1]));
            } else if (isEnabled(SENSOR_TEMPERATURE)) {
                float data = bmx055.readTemperature();
                output.add(new SensorData(SENSOR_TEMPERATURE, data));
            } else if (isEnabled(SENSOR_PRESSURE)) {
                float data = bmx055.readPressure();
                output.add(new SensorData(SENSOR_PRESSURE, data));
            }
            if (isEnabled(SENSOR_HUMIDITY)) {
                output.add(new SensorData(SENSOR_HUMIDITY, bmx055.readHumidity()));
            }
        } catch (Throwable t) {
            Log.w(TAG, "Cannot collect BMx055 data. Ignoring it for now", t);
        }
    }

    @Override
    public void closeQuietly() {
        if (bmx055 != null) {
            try {
                bmx055.close();
            } catch (IOException e) {
                // close quietly
            }
            bmx055 = null;
        }
    }
}
