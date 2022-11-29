package com.bakoconsigne.bako_collector_match.services;

import android.util.Log;
import com.bakoconsigne.bako_collector_match.listeners.CustomArduinoListener;
import me.aflak.arduino.Arduino;

import static com.bakoconsigne.bako_collector_match.MainActivity.LOGGER_TAG;

/**
 * Service to interact with arduino
 */
public class ArduinoService {

    private static final ArduinoService INSTANCE = new ArduinoService();

    private Arduino arduino;

    private CustomArduinoListener customArduinoListener;

    /**
     * Private constructor
     */
    private ArduinoService() {
    }

    /**
     * Return an instance of {@link ArduinoService}
     *
     * @return an instance of {@link ArduinoService}
     */
    public static ArduinoService getInstance() {
        return INSTANCE;
    }

    public void setArduino(final Arduino arduino) {
        this.arduino = arduino;
    }

    public void setCustomArduinoListener(final CustomArduinoListener customArduinoListener) {
        this.customArduinoListener = customArduinoListener;
    }

    /**
     * Open the box
     *
     * @param numDrawer
     *     N° of drawer
     */
    public void openBox(final Integer numDrawer) {

        Log.d(LOGGER_TAG, "ArduinoService openBox" + numDrawer);
        arduino.send(("ob" + numDrawer).getBytes());
    }

    /**
     * Get weight of drawer
     *
     * @param numDrawer
     *     N° of drawer
     */
    public void getWeightBox(final Integer numDrawer) {
        arduino.send(("pb1" + numDrawer).getBytes());
    }

    /**
     * Open all drawers
     */
    public void open() {
        arduino.send("open".getBytes());
    }

    /**
     * Close all drawers
     */
    public void close() {
        arduino.send("close".getBytes());
    }

    public void send(final String command) {
        arduino.send(command.getBytes());
    }

    /**
     * Check the status of the collector
     * - open or close
     */
    public void status() {
        arduino.send("status".getBytes());
    }

    /**
     * Check the status of the doors, tare the pesons
     * and check magnet
     */
    public void check() {
        arduino.send("check".getBytes());
    }

    public void onMessageReceived(final String message) {
        Log.d(LOGGER_TAG, "ArduinoService onMessageReceived " + message);
        if (this.customArduinoListener != null) {
            this.customArduinoListener.onMessageReceived(message);
        }
    }

}
