package resq;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Servo;
import ftc.team6460.javadeck.ftc.Utils;
import org.swerverobotics.library.SynchronousOpMode;

import java.io.*;

/**
 * Created by akh06977 on 9/18/2015.
 */

public class ResqRecordV2Auton extends SynchronousOpMode {
    public void fillInSettings() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        startSide = getStartSide();
        teamColor = getTeam();

    }

    GyroHelper gh;

    protected static ResqAuton.Side startSide;
    protected static ResqAuton.Colors teamColor;

    public ResqAuton.Side getStartSide() {
        return ResqAuton.Side.valueOf(sharedPref.getString("auton_start_position", "MOUNTAIN"));
    } //get starting side from settings dialog (

    public ResqAuton.Colors getTeam() {
        return ResqAuton.Colors.valueOf(sharedPref.getString("auton_team_color", "BLUE"));

    }

    public String getGoal() {
        return (sharedPref.getString("auton_goal_position", "INVALID"));

    }

    double scaledPower;
    Servo btnPushSrvo; // Left servo, labeled 2
    boolean pushServoDeployed = false;
    double aimPos = 0.32;
    Servo aimServo; // Lift servo
    SharedPreferences sharedPref;
    FileOutputStream fileOutputStream;
    DataOutputStream dos;
    int actionCode = 0;
    ByteArrayOutputStream baos;
    DataOutputStream dbaos;
    double ourVoltage;
    DcMotor l0;
    DcMotor l1;
    DcMotor r0;
    DcMotor r1;
    DcMotor w;

    DcMotor w2;

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

        aimServo = hardwareMap.servo.get(DeviceNaming.AIM_SERVO);
        aimServo.setPosition(0.32);
        w = hardwareMap.dcMotor.get("w");
        boxSrvo = hardwareMap.servo.get(DeviceNaming.BOX_SERVO);
        Servo btnSrvo = hardwareMap.servo.get(DeviceNaming.BUTTON_SERVO);
        btnSrvo.setPosition(0.379);
        hardwareMap.servo.get(DeviceNaming.L_LEVER_SERVO).setPosition(0.036);
        hardwareMap.servo.get(DeviceNaming.R_LEVER_SERVO).setPosition(0.931);
        boxSrvo.setPosition(1.0);
        gh = new GyroHelper(this);
    }

    public void init_() {
        initm();
        try {
            fillInSettings();
            try {
                btnPushSrvo = hardwareMap.servo.get("btnPush");
            } catch (Exception e) {
                telemetry.addData("INITFAULT", "BTNSERVO");
            }
            try {

                aimServo = hardwareMap.servo.get("aimServo");
            } catch (Exception e) {
                telemetry.addData("INITFAULT", "BTNSERVO");
            }

            sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
            scaledPower = Utils.getSafeDoublePref("lowspeed_power_scale", sharedPref, 0.50);
            this.gamepad1.setJoystickDeadzone(0.1f);
            String name = "FTCREC-";
            if (teamColor == ResqAuton.Colors.BLUE) {
                name = name + "BLUE-";
            } else {
                name = name + "RED-";
            }

            if (startSide == ResqAuton.Side.MIDLINE) {
                name = name + "MID-";
            } else {
                name = name + "MTN-";
            }

            name = name + getGoal() + ".run";
            fileOutputStream = openFileOutput(name);
            telemetry.addData("FILENAME", name);


            try {
                ourVoltage = hardwareMap.voltageSensor.iterator().next().getVoltage();
            } catch (Exception e) {
                ourVoltage = 12.0;
            }
            dos = new DataOutputStream(fileOutputStream);
            baos = new ByteArrayOutputStream();
            dbaos = new DataOutputStream(baos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean runDrive = true;
    boolean hasWritten = false;
    int loops = 0;
    long ns = 0;

    private FileOutputStream openFileOutput(String s) throws FileNotFoundException {
        File file = new File(Environment.getExternalStorageDirectory(), s);
        return new FileOutputStream(file);
    }

    long tsLast;

    Servo boxSrvo;

    protected void dumpClimbers() throws InterruptedException {
        for (double d = 0.871; d >= 0.09; d -= .03) {
            boxSrvo.setPosition(d);
            idle();
        }
        idle();
        Thread.sleep(500);
        idle();
        for (double d = 0.09; d <= 0.871; d += .1) {
            boxSrvo.setPosition(d);
            idle();
        }
        idle();
        Thread.sleep(100);
    }

    public void loop_() throws InterruptedException {
        if (ns == 0) ns = System.nanoTime();
        if (runDrive) {
            if (gamepad1.x) {
                // turn mode...
                l0.setPower(0.0);
                r0.setPower(0.0);
                l1.setPower(0.0);
                r1.setPower(0.0);

                idle();
                Thread.sleep(300);
                while (gamepad1.x) {
                    updateGamepads();
                }  // wait for release.
                gh.update();

                double oldAngle = gh.getAngles().heading;
                while (!gamepad1.x) {
                    updateGamepads();
                    l0.setPower(this.gamepad1.left_stick_x / 4);
                    r0.setPower(-this.gamepad1.left_stick_x / 4);
                    idle();
                }
                l0.setPower(0);
                r0.setPower(0);
                Thread.sleep(300);
                gh.update();
                double newAngle = gh.getAngles().heading;
                double thetaDiff = newAngle - oldAngle;
                while (thetaDiff > 180) thetaDiff -= 360;
                while (thetaDiff < -180) thetaDiff += 360;
                ns = System.nanoTime() - tsLast-1;
                try {
                    dbaos.writeByte(0x01);
                    dbaos.writeLong((tsLast = System.nanoTime() - ns));
                    dbaos.writeDouble(thetaDiff);
                    dbaos.writeDouble(0.0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } else if (gamepad1.right_bumper) {
                // box out mode...
                l0.setPower(0.0);
                r0.setPower(0.0);
                l1.setPower(0.0);
                r1.setPower(0.0);

                dumpClimbers();

                ns = System.nanoTime() - tsLast;
                try {
                    dbaos.writeByte(0x02);
                    dbaos.writeLong((tsLast = System.nanoTime() - ns));
                    dbaos.writeDouble(0);
                    dbaos.writeDouble(0.0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                double scaleActual = (this.gamepad1.right_trigger > 0.2) ? scaledPower : 1.00;
                double tipPreventionPower = 0;
                double lCalculated = this.gamepad1.left_stick_y * scaleActual + tipPreventionPower;

                double rCalculated = this.gamepad1.right_stick_y * scaleActual + tipPreventionPower;


                lCalculated /= 2;
                rCalculated /= 2;

                l0.setPower(lCalculated);
                r0.setPower(rCalculated);

                l1.setPower(lCalculated);
                r1.setPower(rCalculated);
                try {
                    dbaos.writeByte(0x00);
                    dbaos.writeLong((tsLast = System.nanoTime() - ns));
                    dbaos.writeDouble(lCalculated);
                    dbaos.writeDouble(rCalculated);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                idle();
                loops++;
                telemetry.addData("LPS", loops);

                if (gamepad1.a) {
                    actionCode = 1;
                    runDrive = false;

                } else if (gamepad1.b) {
                    actionCode = 0;
                    runDrive = false;
                } else if (gamepad1.y) {
                    actionCode = 2;
                    runDrive = false;
                }
            }
        } else if (!hasWritten) {
            try {
                hasWritten = true;
                dos.writeByte(0x01); // version 1
                dos.writeInt(loops);
                dos.writeDouble(ourVoltage);
                dbaos.flush();
                baos.flush();
                baos.close();
                dos.write(baos.toByteArray());
                dos.close();
                fileOutputStream.close();
                telemetry.addData("SUCCESS", "WROTE DATA");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // pass
        }


    }


    @Override
    protected void main() throws InterruptedException {
        init_();
        waitForStart();
        while (!isStopRequested()) {
            updateGamepads();
            loop_();
        }
    }
}

