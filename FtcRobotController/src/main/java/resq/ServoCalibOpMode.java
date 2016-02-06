package resq;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.ServoController;

import java.util.Arrays;


public class ServoCalibOpMode  extends OpMode{
    int servoId = 0;
    double[] val = new double[6];
    boolean lastLeft = false, lastRight = false, lastUp = false, lastDown = false;
    ServoController sc;
    @Override
    public void init() {
        sc = hardwareMap.servoController.get("testservo");
    }

    @Override
    public void loop() {
        if(gamepad1.dpad_down & !lastDown) val[servoId] -= (1.0/128.0);
        lastDown = gamepad1.dpad_down;
        if(gamepad1.dpad_up & !lastUp) val[servoId] += (1.0/128.0);
        lastUp = gamepad1.dpad_up;

        val[servoId] -= gamepad1.left_stick_y/512;
        val[servoId] -= gamepad1.right_stick_y/2048;

        if(val[servoId]<0) val[servoId] = 0;
        if(val[servoId]>1) val[servoId] = 1;
        if(gamepad1.dpad_left & !lastLeft) servoId--;
        if(gamepad1.dpad_right & !lastRight) servoId++;
        lastLeft = gamepad1.dpad_left;
        lastRight = gamepad1.dpad_right;
        for(int i = 0; i < 6; i++){
            sc.setServoPosition(i+1, val[i]);
        }
        servoId %= 6;
        telemetry.addData("CURRENT", servoId+1);
        telemetry.addData("VALS", Arrays.toString(val));

    }
}

