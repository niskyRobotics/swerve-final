package resq;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import com.qualcomm.ftcrobotcontroller.R;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;
import ftc.team6460.javadeck.ftc.Utils;
import org.swerverobotics.library.SynchronousOpMode;

/**
 * Created by akh06977 on 9/18/2015.
 */

public class RectResqFallbackTeleop extends SynchronousOpMode {
    DcMotor l0;
    DcMotor l1;
    DcMotor r0;
    DcMotor r1;
    DcMotor w;

    DcMotor w2;
    MediaPlayer r2Beep, r2Startup;
    public void initm() {
        l0 = hardwareMap.dcMotor.get("l0");
        r0 = hardwareMap.dcMotor.get("r0");

        l1 = hardwareMap.dcMotor.get("l1");
        r1 = hardwareMap.dcMotor.get("r1");


        w = hardwareMap.dcMotor.get("w");
        w2 = hardwareMap.dcMotor.get("w2");

        r0.setDirection(DcMotor.Direction.REVERSE);
        r1.setDirection(DcMotor.Direction.REVERSE);
        l0.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        l1.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        r0.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        r1.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        w.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        try {
            r2Beep = MediaPlayer.create(hardwareMap.appContext, R.raw.r2beep);
            r2Startup = MediaPlayer.create(hardwareMap.appContext, R.raw.r2startup);
            r2Startup.setLooping(false);
            r2Startup.start();
        } catch(Exception e) {
            // pass on error. Not critical functionality.
        }


    }
    DcMotor ledCtrl;
    double scaledPower;
    Servo btnPushSrvo; // Left servo, labeled 2
    boolean pushServoDeployed = false;
    double aimPos = 0.800;
    Servo aimServo; // Lift servo
    Servo lLvr, rLvr;
    Servo boxServo;
    Servo lHook, rHook;
    int climbLoops = 0;
    public void init_() {
        initm();
        ledCtrl = hardwareMap.dcMotor.get(DeviceNaming.LED_DEV_NAME);
        ledCtrl.setPower(1.0);

        try {
            btnPushSrvo = hardwareMap.servo.get(DeviceNaming.BUTTON_SERVO);
            btnPushSrvo.setPosition(0.875);
        } catch (Exception e) {
            telemetry.addData("INITFAULT", "BTNSERVO");
        }
        try {

            aimServo = hardwareMap.servo.get(DeviceNaming.AIM_SERVO);
            aimServo.setPosition(0.8);
        } catch (Exception e) {
            telemetry.addData("INITFAULT", "BTNSERVO");
        }
        try {

            lLvr = hardwareMap.servo.get(DeviceNaming.L_LEVER_SERVO);
            rLvr = hardwareMap.servo.get(DeviceNaming.R_LEVER_SERVO);
            boxServo = hardwareMap.servo.get(DeviceNaming.BOX_SERVO);
            boxServo.setPosition(1.0);
        } catch (Exception e) {
            telemetry.addData("INITFAULT", "LEVER");
        }
        try {

            lHook = hardwareMap.servo.get(DeviceNaming.L_HOOK_SERVO);
            rHook = hardwareMap.servo.get(DeviceNaming.R_HOOK_SERVO);
            lHook.setPosition(1.0);
            rHook.setPosition(0.0);
        } catch (Exception e) {
            telemetry.addData("INITFAULT", "HOOK");
        }


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        scaledPower = Utils.getSafeDoublePref("lowspeed_power_scale", sharedPref, 0.50);
        this.gamepad1.setJoystickDeadzone(0.1f);

    }
    boolean lLevOut = false;
    boolean rLevOut = false;
    double hookPosition = 0.0;
    double smoothedWinchJoystick = 0.0;

    public void loop_() {
        updateGamepads();
        if(gamepad2.y){
            try {
                dumpClimbers();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        double scaleActual = (this.gamepad1.right_trigger > 0.2) ? scaledPower : 1.00;
        boolean fullOverrideNeg = (this.gamepad1.right_trigger > 0.2);
        boolean fullOverridePos = (this.gamepad1.left_trigger > 0.2);
        double tipPreventionPower = 0;
        double lCalculated = this.gamepad1.left_stick_y * scaleActual + tipPreventionPower;

        double rCalculated = this.gamepad1.right_stick_y * scaleActual + tipPreventionPower;
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
        if(gamepad2.left_trigger>0.5){
            hookPosition+= 0.03;
        } else {
            hookPosition-=0.03;
        }
        hookPosition = Range.clip(hookPosition, 0, 1);

        lHook.setPosition(1-hookPosition);
        rHook.setPosition(hookPosition);

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
        if(this.gamepad2.right_stick_y>0.1) climbLoops++; else climbLoops = 0;
        if(climbLoops==20){
            //exact match to play ONCE during each climb burst
            try {
                r2Beep.setLooping(false);
                r2Beep.start();
            } catch(Exception e) {
                // pass on error; not critical functionality
            }
        }

        else smoothedWinchJoystick = (3 * smoothedWinchJoystick + this.gamepad2.right_stick_y)/4;

        double g2y = this.gamepad2.right_stick_y;
        if(g2y<0) g2y/=2;
        w.setPower(-g2y);
        w2.setPower(-g2y);
        telemetry.addData("w", "1");


        aimPos += this.gamepad2.left_stick_y / 256;
        aimPos = Range.clip(aimPos, 0.3, 1.0);
        telemetry.addData("aimPos", aimPos);
        if (aimServo != null)
            aimServo.setPosition(aimPos);

        //if (btnPushSrvo != null) btnPushSrvo.setPosition(pushServoDeployed ? 0.091 : 0.365);
        if (lLvr != null) lLvr.setPosition(lLevOut ? 0.576:0.036);
        if (rLvr != null) rLvr.setPosition(rLevOut ? 0.438:0.931);
        if(gamepad2.right_trigger > 0.2){
            ledCtrl.setPower(0.0);
        }
        if(gamepad2.right_bumper){
            ledCtrl.setPower(1.0);
        }


    }

    protected void dumpClimbers() throws InterruptedException {
        l0.setPower(0.0);
        l1.setPower(0.0);
        r0.setPower(0.0);
        r1.setPower(0.0);
        w.setPower(0.0);
        w2.setPower(0.0);
        for(double d = 1.0; d >= 0.09; d-=.01) {
            boxServo.setPosition(d);
            idle();
            Thread.sleep(20);
        }
        idle();
        Thread.sleep(500);
        for(double d = 0.09; d <= 1.0; d+=.03) {
            boxServo.setPosition(d);

            Thread.sleep(20);
            idle();
        }
        idle();
    }
    public @Override void main() throws InterruptedException {
        init_();

        waitForStart();
        while(!isStopRequested()){
            loop_();
        }
    }

}
