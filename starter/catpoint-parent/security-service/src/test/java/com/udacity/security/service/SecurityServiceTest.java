package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private BufferedImage bufferedImage;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        securityService = new SecurityService(securityRepository, imageService);
    }

    /**
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     */
    @Test
    public void testIfAlarmIsArmedAndSensorActivated_systemGoesToPendingState() {
        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     * set the alarm status to ALARM.
     */
    @Test
    public void testIfSensorActivatedWhilePending_setAlarmToAlarmState() {
        Sensor sensor = new Sensor("Window Sensor", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    public void testIfPendingAlarmAndAllSensorsInactive_returnToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of());

        securityService.changeSensorActivationStatus(new Sensor("Living Room", SensorType.MOTION), false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * If alarm is active, change in sensor state should not affect the alarm state.
     */
    @Test
    public void testIfAlarmIsActive_sensorStateChangeDoesNotAffectAlarm() {
        Sensor sensor = new Sensor("Back Door", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    /**
     * If a sensor is activated while already active and the system is in pending state,
     * change it to alarm state.
     */
    @Test
    public void testIfSensorActivatedWhileAlreadyActive_setAlarmToAlarmState() {
        Sensor sensor = new Sensor("Garage", SensorType.DOOR);
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    public void testIfSensorDeactivatedWhileAlreadyInactive_noAlarmStateChange() {
        Sensor sensor = new Sensor("Bedroom Sensor", SensorType.WINDOW);
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    /**
     * If the image service identifies an image containing a cat while the system is armed-home,
     * put the system into alarm status.
     */
    @Test
    public void testIfCatDetectedAndSystemIsArmedHome_setAlarmToAlarmState() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(bufferedImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * If the image service identifies an image that does not contain a cat,
     * change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void testIfNoCatDetectedAndNoActiveSensors_setAlarmToNoAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        lenient().when(securityRepository.getSensors()).thenReturn(Set.of());

        securityService.processImage(bufferedImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * If the system is disarmed, set the status to no alarm.
     */
    @Test
    public void testIfSystemIsDisarmed_setAlarmToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * If the system is armed, reset all sensors to inactive.
     */
    @Test
    public void testIfSystemIsArmed_resetAllSensorsToInactive() {
        Sensor sensor1 = new Sensor("Front Sensor", SensorType.MOTION);
        Sensor sensor2 = new Sensor("Garage Sensor", SensorType.DOOR);
        sensor1.setActive(true);
        sensor2.setActive(true);

        when(securityRepository.getSensors()).thenReturn(Set.of(sensor1, sensor2));

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
    }

    /**
     * If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
     */
    @Test
    public void testIfSystemIsArmedHomeAndCatDetected_setAlarmToAlarmState() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(bufferedImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

}
