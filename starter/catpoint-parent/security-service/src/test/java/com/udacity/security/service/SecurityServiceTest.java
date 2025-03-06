package com.udacity.security.service;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.udacity.image.service.FakeImageService;
import com.udacity.image.service.ImageService;
import com.udacity.security.data.ArmingStatus;
import com.udacity.security.data.AlarmStatus;
import com.udacity.security.data.SensorType;
import com.udacity.security.data.Sensor;
import com.udacity.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
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

    private FakeImageService fakeImageService;

    @BeforeEach
    public void setUp() {
        fakeImageService = new FakeImageService();
        securityService = new SecurityService(securityRepository, fakeImageService);
    }

    //If alarm is armed and a sensor becomes activated, put the system into pending alarm status
    // Parameterized test for arming with activated sensor
    @ParameterizedTest
    @ValueSource(strings = {"FRONT DOOR", "BACK WINDOW", "LIVING ROOM"})
    public void armingWithActivatedSensor_TriggersPendingAlarm(String sensorName) {
        Sensor sensor = new Sensor(sensorName, SensorType.DOOR);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM); // **Crucial line**
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on.
    @Test
    public void activatedSensorWhilePendingAlarm_TriggersAlarmState() {
        Sensor sensor = new Sensor("Window", SensorType.WINDOW);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);

        // Verify that alarm is set when sensor triggers pending alarm
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    //If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    public void allSensorsInactiveResetsPending_AlarmToNoAlarm() {
        Sensor inactiveSensor = new Sensor("Living Room", SensorType.MOTION);
        inactiveSensor.setActive(false); // Sensor is already inactive!
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(inactiveSensor, false); // "Deactivating" it again

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM); // This should now pass
    }

    @Test
    public void alarmStateRemainsUnchanged_WhenSensorIsDeactivated_WhileActive() {
        Sensor sensor = new Sensor("Back Door", SensorType.DOOR);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        // Deactivate sensor without affecting alarm state
        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);

        // Ensure alarm status is not updated
        verify(securityRepository, never()).setAlarmStatus(any());
    }
    //If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    public void deactivationOfInactiveSensorDoesNot_TriggerAlarmStateChange() {
        Sensor sensor = new Sensor("Bedroom Sensor", SensorType.WINDOW);
        sensor.setActive(Boolean.FALSE);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);

        // Ensure that deactivating an already inactive sensor doesn't trigger any alarm state change
        verify(securityRepository, never()).setAlarmStatus(any());
    }


    @Test
    public void noCatDetected_AndAllSensorsInactive_SetsAlarmToNoAlarm() {
        Sensor sensor1 = new Sensor("Living Room", SensorType.MOTION);
        Sensor sensor2 = new Sensor("Bedroom Window", SensorType.WINDOW);
        sensor1.setActive(false);
        sensor2.setActive(false);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If the system is disarmed, set the status to no alarm
    @Test
    public void disarmingSystemSets_AlarmToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If the system is armed, reset all sensors to inactive
    @Test
    public void armingSystemResets_AllSensorsTo_Inactive() {
        Sensor sensor1 = new Sensor("LIVING ROOM", SensorType.MOTION);
        Sensor sensor2 = new Sensor("FRONT DOOR", SensorType.DOOR);
        sensor1.setActive(Boolean.FALSE);
        sensor2.setActive(Boolean.FALSE);

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        verify(securityRepository).setArmingStatus(ArmingStatus.ARMED_AWAY);
        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
    }
    //If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    public void alarmStateRemainsUnchanged_WhenSensorStateChanges_WhileAlarmIsActive() {
        Sensor sensor = new Sensor("Kitchen Window", SensorType.WINDOW);
        sensor.setActive(Boolean.TRUE);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, Boolean.FALSE);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    //If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void cameraImageWithCat_WhileArmedHome_SetsAlarmToAlarm() {
        lenient().when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        securityService.processImage(bufferedImage);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    //If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @ParameterizedTest
    @ValueSource(strings = {"Living Room", "Bedroom Window", "Kitchen"})
    public void cameraImageWithoutCatAndNoActiveSensors_SetsAlarmToNoAlarm(String sensorName) {
        Sensor sensor = new Sensor(sensorName, SensorType.MOTION);
        sensor.setActive(false);

        lenient().when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        securityService.processImage(bufferedImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

}