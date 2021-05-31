package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    // class under Test
    SecurityService securityService;

    private Sensor sensor;

    private Set<Sensor> allSensors = new HashSet<>();

    BufferedImage bufferedImage = ImageIO.read(new File("D:\\Development\\Java\\catpoint-parent\\sample-cat.jpg"));

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    SecurityServiceTest() throws IOException {
    }

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository,imageService);
        sensor = new Sensor("TestMotionSensor", SensorType.MOTION);
        securityService.addSensor(sensor);
        allSensors.add(sensor);
        StatusListener statusListener = Mockito.mock(StatusListener.class);
        securityService.addStatusListener(statusListener);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    public void test_1_ifAlarmArmedAndSensorActivated_putSystemIntoPendingAlarmStatus (ArmingStatus armingStatus) {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    public void test_2_ifAlarmArmedAndSensorActivatedAndAlreadyPendingAlarm_putSystemIntoAlarmStatus (ArmingStatus armingStatus) {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    // 3. If pending alarm and all sensors are inactive, return to no alarm state
    @Test
    public void test_3_ifPendingAlarmAndAllSensorsInactive_putSystemIntoNoAlarmStatus () {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state
    @Test
    public void test_4_ifAlarmActiveAndChangeSensorState_KeepAlarmState () {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(Mockito.any());
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state
    @Test
    public void test_5_ifSensorActivatedWhileAlreadyActiveAndAlarmPendingState_putSystemIntoAlarmStatus () {
        sensor.setActive(true);
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state
    @Test
    public void test_6_ifSensorIsDeactivatedWhileAlreadyInactive_KeepAlarmState () {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(Mockito.any());
    }

    // 7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status
    @Test
    public void test_7_ifAlarmArmedAndSensorActivated_putSystemIntoPendingAlarmStatus () {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(BufferedImage.class),Mockito.anyFloat())).thenReturn(true);

        securityService.processImage(bufferedImage);

        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active
    @Test
    public void test_8_ifImageDoesNotContainCatAndSensorsNotActiveAlarmArmedAndSensorActivated_putSystemIntoNoAlarmStatus () {
        //Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(BufferedImage.class),Mockito.anyFloat())).thenReturn(false);
        sensor.setActive(false);
        securityService.processImage(bufferedImage);

        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    // 9. If the system is disarmed, set the status to no alarm
    @Test
    public void test_9_ifSystemIsDisarmed_putSystemIntoNoAlarmStatus () {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    public void test_10_ifSystemIsArmed_resetSensorsToInactive (ArmingStatus armingStatus) {
        //Mockito.when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Mockito.when(securityRepository.getSensors()).thenReturn(allSensors);
        sensor.setActive(true);
        securityService.setArmingStatus(armingStatus);
        Assertions.assertEquals(false, sensor.getActive());
    }

    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm
    @Test
    public void test_11_ifSystemIsArmedHomeAndCameraShowsCat_putAlarmStatusToAlarm () {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Mockito.when(imageService.imageContainsCat(Mockito.any(BufferedImage.class),Mockito.anyFloat())).thenReturn(true);

        securityService.processImage(bufferedImage);

        Mockito.verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Coverage 1. Get the alarm status
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"PENDING_ALARM", "NO_ALARM", "ALARM"})
    public void cov_1_getTheAlarmStatus (AlarmStatus alarmStatus) {
        Mockito.when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        Assertions.assertEquals(alarmStatus, securityService.getAlarmStatus());
    }

    // Coverage 2. Remove sensor
    @Test
    public void cov_2_removeSensor () {
        securityService.removeSensor(sensor);
        Mockito.verify(securityRepository).removeSensor(sensor);
    }

    // Coverage 3. Sensor active and system disarmed
    @Test
    public void cov_3_sensorActiveAndSystemDisarmed_doNothing () {
        Mockito.when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor,true);
        Mockito.verify(securityRepository, Mockito.never()).setAlarmStatus(Mockito.any(AlarmStatus.class));
    }

    @Test
    public void cov_4_removeStatusListener () {
        securityService.removeStatusListener(statusListener);
    }

}