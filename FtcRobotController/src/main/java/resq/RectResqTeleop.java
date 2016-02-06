package resq;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;
import ftc.team6460.javadeck.ftc.Utils;
import org.swerverobotics.library.interfaces.Acceleration;

/**
 * Created by akh06977 on 9/18/2015.
 */

public class RectResqTeleop extends RectResqCommon {
    DcMotor ledCtrl;

    double scaledPower;
    private static final double TIP_PREVENTION_WARNING_ANGLE = 50;
    private static final double TIP_PREVENTION_CRIT_ANGLE = 65;
    private static final double TIP_PREVENTION_PWR = 0.1; // per m*s^-2
    GyroHelper gh;


    Servo btnPushSrvo;
    Servo aimServo; // Lift servo
    boolean pushServoDeployed = false;
    @Override
    public void init() {
        super.init();
        lLvr = hardwareMap.servo.get(DeviceNaming.L_LEVER_SERVO);
        rLvr = hardwareMap.servo.get(DeviceNaming.R_LEVER_SERVO);
        aimServo = hardwareMap.servo.get("aimServo");
        ledCtrl = hardwareMap.dcMotor.get(DeviceNaming.LED_DEV_NAME);
        ledCtrl.setPower(1.0);
        btnPushSrvo = hardwareMap.servo.get("btnPush");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        scaledPower = Utils.getSafeDoublePref("lowspeed_power_scale", sharedPref, 0.50);
        this.gamepad1.setJoystickDeadzone(0.1f);
        gh = new GyroHelper(this);
        gh.startUpGyro();
    }


    double aimPos = 0.32;

    Servo lLvr, rLvr;

    boolean lLevOut = false;
    boolean rLevOut = false;
    @Override
    public void loop() {

        double scaleActual = (this.gamepad1.right_trigger > 0.2) ? scaledPower : 1.00;

        gh.update();
        Acceleration grav = gh.getRawAccel();
        double tipAngle = Math.toDegrees(Math.acos(grav.accelZ/Math.hypot(Math.hypot(grav.accelX, grav.accelY), grav.accelZ)));
        telemetry.addData("TIPANGLE", "tipAngle");
        double tipPreventionPower = 0;
        if(tipAngle>TIP_PREVENTION_CRIT_ANGLE) {
            tipPreventionPower = grav.accelX * TIP_PREVENTION_PWR;
            telemetry.addData("TIP", "DANGER");

        }
        else if (tipAngle > TIP_PREVENTION_WARNING_ANGLE){
            telemetry.addData("TIP", "WARNING");
        } else {
            telemetry.addData("TIP", "OK");
        }

        boolean fullOverrideNeg = (this.gamepad1.right_trigger > 0.2);
        boolean fullOverridePos = (this.gamepad1.left_trigger > 0.2);

        double lCalculated = this.gamepad1.left_stick_y * scaleActual + tipPreventionPower;

        double rCalculated = this.gamepad1.right_stick_y * scaleActual + tipPreventionPower;
        if (fullOverrideNeg) {
            lCalculated = -1;
            rCalculated = -1;
        } else if (fullOverridePos) {
            lCalculated = 1;
            rCalculated = 1;
        }
        l0.setPower(lCalculated);
        r0.setPower(rCalculated);

        l1.setPower(lCalculated);
        r1.setPower(rCalculated);


        //self explanatory winch

        w.setPower(this.gamepad2.right_stick_y);
        telemetry.addData("w", "1");

        if(this.gamepad2.dpad_left) {
            lLevOut = true;
            rLevOut = false;
        } else if(this.gamepad2.dpad_right) {
            lLevOut = false;
            rLevOut = true;
        } else if(this.gamepad2.dpad_up) {
            lLevOut = false;
            rLevOut = false;
        }

        if (lLvr != null) lLvr.setPosition(lLevOut ? 0.576:0.036); // TODO calibrate
        if (rLvr != null) rLvr.setPosition(rLevOut ? 0.438:0.931); // TODO calibrate
        pushServoDeployed = (this.gamepad1.left_trigger>0.2);

        btnPushSrvo.setPosition(pushServoDeployed ? 0.091 : 0.365);
        aimPos-=this.gamepad2.left_stick_y/512;
        aimPos = Range.clip(aimPos, 0.32, 0.92);
        aimServo.setPosition(aimPos);
        if(gamepad2.right_trigger > 0.2){
            ledCtrl.setPower(0.0);
        }
        if(gamepad2.right_bumper){
            ledCtrl.setPower(1.0);
        }
    }


}

