package com.udacity.security.service;

import com.udacity.image.service.FakeImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.SecurityRepository;
import com.udacity.security.data.Sensor;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 */
public class SecurityService {

    private FakeImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();

    public SecurityService(SecurityRepository securityRepository, FakeImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
            securityRepository.getSensors().forEach(sensor -> sensor.setActive(false));
            securityRepository.setArmingStatus(armingStatus);
            return;
        }
        if (armingStatus == ArmingStatus.ARMED_HOME || armingStatus == ArmingStatus.ARMED_AWAY) {
            securityRepository.getSensors().forEach(sensor -> sensor.setActive(false));
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        System.out.println("catDetected called with cat=" + cat + ",ArmingStatus=" + getArmingStatus());
        if (cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            if(getAlarmStatus() != AlarmStatus.ALARM ){
                setAlarmStatus(AlarmStatus.ALARM);
                System.out.println("setAlarmStatus(ALARM) called");
            }
        } else {
            if(getAlarmStatus() != AlarmStatus.NO_ALARM )
                setAlarmStatus(AlarmStatus.NO_ALARM);
            System.out.println("setAlarmStatus(NO_ALARM) called");
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        System.out.println("setAlarmStatus called with status=" + status);
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if (securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; // No action if the system is disarmed
        }
        switch (securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated.
     */
    private void handleSensorDeactivated() {
        if (securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            return; // Do not change the alarm state if it is already in ALARM state
        }
        if (securityRepository.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            // Check if all sensors are inactive
            boolean allSensorsInactive = securityRepository.getSensors().stream()
                    .noneMatch(Sensor::getActive);
            if (allSensorsInactive) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        if (!sensor.getActive() && active) {
            // Sensor is being activated
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            // Sensor is being deactivated
            handleSensorDeactivated();
        } else if (!sensor.getActive() && !active) {
            // Sensor is already inactive and is being deactivated again
            handleSensorDeactivated(); // Add this line to handle the case
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        System.out.println("processing called");
        catDetected(Boolean.valueOf(imageService.imageContainsCat(currentCameraImage, 50.0f)));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    /**
     * Get an immutable set of sensors to avoid exposing the internal mutable collection.
     */
    public Set<Sensor> getSensors() {
        return Collections.unmodifiableSet(securityRepository.getSensors());
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}