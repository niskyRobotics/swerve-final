package resq;


import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.qualcomm.ftcrobotcontroller.FtcRobotControllerActivity;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;
import ftc.team6460.javadeck.ftc.Utils;
import ftc.team6460.javadeck.ftc.vision.OpenCvActivityHelper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.swerverobotics.library.SynchronousOpMode;
import org.swerverobotics.library.interfaces.*;

/**
 * Created by hon07726 on 10/2/2015.
 */
public class ResqAuton extends SynchronousOpMode {

    public static final double BTN_SRVO_RETRACTED = 0.875;
    public static final double BTN_SRVO_DEPLOYED = 0.09;
    protected static Side startSide;
    protected static Colors teamColor;
    private static double curX, curY, curYAW;
    final GyroHelper gyroHelper = new GyroHelper(this);
    private double delay;
    int DUMMY = Integer.MAX_VALUE;
    static MatColorSpreadCallback cb;
    Servo aimServo;
    DcMotor ledCtrl;
    private OpenCvActivityHelper ocvh;

    public void main() throws InterruptedException {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        fillInSettings();
        startUpHardware();
        startCamera();
        this.waitForStart();
        Thread.sleep(1000);
        // TEST AUTON TO SEE IF BACKEND WORKS
        offsetPosition(0, 0, 0);
        while (2 + 2 <= DUMMY) {
            updateGamepads();
            setLeftSpeed(gamepad1.left_stick_y);
            setRightSpeed(gamepad1.right_stick_y);
            doPeriodicTasks();
            cb.setOverText(String.format("%f, %f (%f deg)", getGyroX(), getGyroY(), getGyroYAW()));
        }


        if (2 + 2 <= Integer.MAX_VALUE) return;
        navigateTo(4, 4, 90);
        navigateTo(0, 0, 0);
        turnTo(180);
        turnTo(0);
        turnTo(90);
        if (2 + 2 <= Integer.MAX_VALUE) return;

        waitTime(1000);
        if (teamColor == Colors.BLUE) {
            if (startSide == Side.MOUNTAIN) {
                offsetPosition(29 / 4.0, 2 / 3, 90);
            } else if (startSide == Side.MIDLINE) {
                offsetPosition(17 / 6, 2 / 3, 90);
            }

        } else if (teamColor == Colors.RED) {
            if (startSide == Side.MOUNTAIN) {
                offsetPosition(2 / 3.0, 29.0 / 4, 0);
            } else if (startSide == Side.MIDLINE) {
                offsetPosition(2 / 3.0, 17 / 6.0, 0);
            }

        }
        waitForStart();
        waitAllianceTeamDelay();
        goForward(0.5); //Get away from wall
        if (teamColor == Colors.BLUE) {
            if (startSide == Side.MOUNTAIN) {
                navigateTo(10.5, 9, 90); //REPLACE
            } else if (startSide == Side.MIDLINE) {
                navigateTo(10.5, 9, 90); //REPLACE
            }

        } else if (teamColor == Colors.RED) {
            if (startSide == Side.MOUNTAIN) {
                navigateTo(9, 10.5, 0); //REPLACE
            } else if (startSide == Side.MIDLINE) {
                navigateTo(9, 10.5, 0); //REPLACE
            }

        }
        detectAndHitBeacon();
        //go to front of mountain, facing the mountain
        boolean farMountain = isFarMountain(); // THIS IS A PLACEHOLDER
        boolean closeMountain = !isFarMountain(); //THIS IS A PLACEHOLDER
        if (teamColor == Colors.RED) {
            if (farMountain) {
                navigateTo(10, 3.5, 315); //REPLACE
            } else if (closeMountain) {
                navigateTo(2, 8.5, 135); //REPLACE
            }

        } else if (teamColor == Colors.BLUE) {
            if (farMountain) {
                navigateTo(3.5, 10, 135); //REPLACE
            } else if (closeMountain) {
                navigateTo(8.5, 2, 315); //REPLACE
            }

        }


    }

    protected void startCamera() throws InterruptedException {
        if(cb!=null) return;
        cb = new MatColorSpreadCallback((Activity) hardwareMap.appContext, null);
        ocvh = new OpenCvActivityHelper((FtcRobotControllerActivity) hardwareMap.appContext);
        ((Activity) hardwareMap.appContext).runOnUiThread(new Runnable() {

            @Override
            public void run() {

                ocvh.addCallback(cb);
                ocvh.attach();
            }
        });
        ocvh.awaitStart();


    }

    private double err() {
        RuntimeException e = new RuntimeException("Fill in the caller!");
        e.printStackTrace();
        for (StackTraceElement ste : e.getStackTrace()) {
            Log.e("FILLIN", ste.toString());
        }
        throw e;
    }

    protected void waitTime(int i) {
        long t = System.currentTimeMillis();
        while (System.currentTimeMillis() < t + (i)) {
            doPeriodicTasks();
        }
    }


    public void fillInSettings() {
        startSide = getStartSide();
        teamColor = getTeam();
        delay = getAllyDelay();//such code

    }

    public void goForward(double dist, double factor) {
        long oriTime = System.currentTimeMillis();
        double oriX = getGyroX();
        double oriY = getGyroY();
        double oriYAW = getGyroYAW();
        double finalX = Math.cos(Math.toRadians(oriYAW))*dist+oriX;
        double finalY = Math.sin(Math.toRadians(oriYAW))*dist+oriY;
        double curX = oriX;
        double curY = oriY;
        double curYAW = oriYAW;
        double distTraveled = 0;
        int accelTime = 1000; //CHANGEABLE
        double speed = 0;
        while (System.currentTimeMillis()<oriTime+accelTime && distTraveled < dist/2) { //ACCELERATION
            speed = Math.min(0.25,((System.currentTimeMillis()-oriTime)/accelTime*1.0)*factor);
            setLeftSpeed(speed);
            setRightSpeed(speed);

            if (curYAW > oriYAW+5 ) { //CORRECTIONS
                while(curYAW>Math.atan2(finalX - curX, finalY - curY)){
                    blendRightSpeed(speed*0.8);
                    setLeftSpeed(speed);
                    speed = ((System.currentTimeMillis()-oriTime)/accelTime*1.00)*factor;
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }

            }
            if (curYAW < oriYAW-5) { //CORRECTIONS
                while(curYAW>Math.atan2(finalX - curX, finalY - curY)) {
                    blendLeftSpeed(speed * 0.8);
                    setRightSpeed(speed);
                    speed = ((System.currentTimeMillis() - oriTime) / accelTime * 1.00) * factor;
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
            }
            curX = getGyroX();
            curY = getGyroY();
            curYAW = getGyroYAW();
        }

        distTraveled = Math.sqrt((curX-oriX)*(curX-oriX)+(curY-oriY)*(curY-oriY));
        double distAccel = distTraveled;

        while (dist-distTraveled>distAccel) { //CONSTANT SPEED
            setLeftSpeed(1.00*factor);
            setRightSpeed(1.00*factor);
            if (curYAW > oriYAW+5) { //CORRECTIONS
                while(curYAW>Math.atan2(finalX - curX, finalY - curY)) {
                    setRightSpeed(1.0);
                    blendLeftSpeed(0.8);
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
            }
            if (curYAW < oriYAW-5) { //CORRECTIONS
                while(curYAW>Math.atan2(finalX - curX, finalY - curY)) {
                    blendRightSpeed(0.8);
                    setLeftSpeed(1.0);
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
            }
            curX = getGyroX();
            curY = getGyroY();
            curYAW = getGyroYAW();
            distTraveled = Math.sqrt((curX-oriX)*(curX-oriX)+(curY-oriY)*(curY-oriY));
        }

        long startDecelTime = System.currentTimeMillis();

        while (Math.abs(curX-finalX)>0.05 || Math.abs(curY-finalY)>0.05) { //DECELERATION
            speed = Math.sqrt(Math.hypot(finalX - curX, finalY - curY)/(dist-distTraveled))*factor;
            setLeftSpeed(speed);
            setRightSpeed(speed);
            if (curYAW > oriYAW+5) { //CORRECTIONS
                while(curYAW>Math.atan2(finalX - curX, finalY - curY)) {
                    blendRightSpeed(0.8 * speed);
                    setLeftSpeed(speed);
                    speed = Math.sqrt(Math.hypot(finalX - curX, finalY - curY) / (dist - distTraveled)) * factor;
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                    curY = getGyroY();
                    curX = getGyroX();
                }
            }
            if (curYAW < oriYAW-5) { //CORRECTIONS
                while(curYAW>Math.atan2(finalX - curX, finalY - curY)) {
                    blendRightSpeed(0.8 * speed);
                    setRightSpeed(speed);
                    speed = Math.sqrt(Math.hypot(finalX - curX, finalY - curY) / (dist - distTraveled)) * factor;
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                    curY = getGyroY();
                    curX = getGyroX();
                }
            }
            curX = getGyroX();
            curY = getGyroY();
            curYAW = getGyroYAW();
        }
    }

    /*
    public void goForward(double secs, double speed) {
        setLeftSpeed(speed);
        setRightSpeed(speed);
        long t = System.currentTimeMillis();
        while (System.currentTimeMillis() < t + (secs * 1000)) {
            doPeriodicTasks();
        }
        setLeftSpeed(0);
        setRightSpeed(0);

    }
*/
    public void goForward(double feet) {
        goForward(feet, 0.25);

    }


    public void turnTo(double YAW) {
        doPeriodicTasks();
        curYAW = getGyroYAW();
        double angle = Math.abs(curYAW - YAW);
        if (angle > 180) {
            angle = 360 - angle;
        }

        double incA = Math.min(15, angle); //the increment angle
        double oriYAW = getGyroYAW();
        double incr = 0;
        //begin turning cases
        if (((oriYAW > YAW) && (oriYAW - YAW) < 180)) { //TURN RIGHT

            while ((oriYAW - curYAW) < incA) {
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                incr = 1 * ((oriYAW - curYAW) / 15);

                doPeriodicTasks();
                curYAW = getGyroYAW();

            }
           /* START INTERMEDIATE MAX SPEED TURN*/
            while ((oriYAW - curYAW) < (angle - incA)) {
                setLeftSpeed(1*.25);
                setRightSpeed(-1*.25);
                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
           /* START DECLINING INCREMENT */
            while ((oriYAW - curYAW) < angle) {
                setLeftSpeed(incr * .25);
                setRightSpeed(-incr*.25);
                incr = (1 * (-(YAW - curYAW) / 15));

                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
            doPeriodicTasks();
            curYAW = getGyroYAW();

        } else if ((oriYAW < YAW) && (YAW - oriYAW) > 180) { //turn right past 0 line
            doPeriodicTasks();
            curYAW = getGyroYAW();
            if (oriYAW > incA) {
                while ((oriYAW - curYAW) < incA) {
                    setLeftSpeed(incr*.25);
                    setRightSpeed(-incr*.25);
                    incr = 1 * ((oriYAW - curYAW) / 15);
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
            } else {
                doPeriodicTasks();
                curYAW = getGyroYAW();
                while(curYAW<180){
                    setLeftSpeed(incr*.25);
                    setRightSpeed(-incr*.25);
                    incr = 1 * ((oriYAW - curYAW) / 15);
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
                while (oriYAW + (360 - curYAW) < incA) {
                    setLeftSpeed(incr*.25);
                    setRightSpeed(-incr*.25);
                    incr = 1 * ((oriYAW - curYAW) / 15);
                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }

        }
       	/* START INTERMEDIATE MAX SPEED TURN*/
            while (curYAW < oriYAW) { //these two while loops are the same thing
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
            while ((360 - curYAW) < (angle - incA)) {//they're just accounting for before the 0 line and after it
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                doPeriodicTasks();
                curYAW = getGyroYAW();

                doPeriodicTasks();
            }
       	/*START DECLINING INCREMENT SPEED */
            while ((oriYAW + (360 - curYAW)) < angle) {
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                incr = 1 * (-(YAW - curYAW) / 15);

                doPeriodicTasks();
                curYAW = getGyroYAW();

            }

            doPeriodicTasks();
            curYAW = getGyroYAW();
            if ((oriYAW + (360 - curYAW)) >= angle) { //stops motors
                setLeftSpeed(0);
                setRightSpeed(0);

                doPeriodicTasks();
            }

        } else if ((oriYAW < YAW) && ((YAW - oriYAW) < 180)) { // LEFT TURN (DO THIS!!)
            while ((curYAW - oriYAW) < incA) {
                setLeftSpeed(-incr*.25);
                setRightSpeed(incr*.25);
                incr = 1 * ((curYAW - oriYAW) / 15);


                doPeriodicTasks();
                curYAW = getGyroYAW();

            }
       	/* START INTERMEDIATE MAX SPEED TURN*/
            while ((curYAW - oriYAW) < (angle - incA)) {
                setLeftSpeed(-1);
                setRightSpeed(1);

                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
       	/* START DECLINING INCREMENT */
            while ((curYAW - oriYAW) < angle) {
                setLeftSpeed(-incr*.25);
                setRightSpeed(incr*.25);
                incr = (1 * (-(YAW - curYAW) / 15));


                doPeriodicTasks();
                curYAW = getGyroYAW();
            }

            doPeriodicTasks();
            curYAW = getGyroYAW();
            if (curYAW >= YAW) {
                setLeftSpeed(0);
                setRightSpeed(0);
            }
        } else if ((oriYAW > YAW) && (oriYAW - YAW) > 180) { //turn left past 0 line

            doPeriodicTasks();
            curYAW = getGyroYAW();
            if (oriYAW > incA) {
                while ((oriYAW - curYAW) < incA) {
                    setLeftSpeed(incr*.25);
                    setRightSpeed(-incr*.25);
                    incr = 1 * ((oriYAW - curYAW) / 15);

                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
            } else {

                doPeriodicTasks();
                curYAW = getGyroYAW();
                while (oriYAW + (360 - curYAW) < incA) {
                    setLeftSpeed(incr*.25);
                    setRightSpeed(-incr*.25);
                    incr = 1 * ((oriYAW - curYAW) / 15);


                    doPeriodicTasks();
                    curYAW = getGyroYAW();
                }
            }
       	/* START INTERMEDIATE MAX SPEED TURN*/
            while (curYAW < oriYAW) { //these two while loops are the same thing
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
            while ((360 - curYAW) < (angle - incA)) {//they're just accounting for before the 0 line and after it
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
       	/*START DECLINING INCREMENT SPEED */
            while ((oriYAW + (360 - curYAW)) < angle) {
                setLeftSpeed(incr*.25);
                setRightSpeed(-incr*.25);
                incr =  (-(YAW - curYAW) / 15);
                doPeriodicTasks();
                curYAW = getGyroYAW();
            }
            doPeriodicTasks();

            curYAW = getGyroYAW();
            if ((oriYAW + (360 - curYAW)) >= angle) { //stops motors
                setLeftSpeed(0);
                setRightSpeed(0);
            }

        }
    }

    DcMotor l0;
    DcMotor l1;
    DcMotor r0;
    DcMotor r1;
    DcMotor w;
    double lSpd, rSpd;



    public void setLeftSpeed(double spd) {

        spd = Range.clip(spd, -1, 1);
        lSpd = spd;
        l0.setPower(spd);
        l1.setPower(spd);
    }

    public void setRightSpeed(double spd) {
        spd = Range.clip(spd, -1, 1);
        rSpd = spd;
        r0.setPower(spd);
        r1.setPower(spd);
    }

    public void blendLeftSpeed(double spd){
        setLeftSpeed((lSpd + spd) / 2);
    }

    public void blendRightSpeed(double spd){
        setRightSpeed((rSpd + spd) / 2);
    }


    public Side getStartSide() {
        return Side.valueOf(sharedPref.getString("auton_start_position", "MOUNTAIN"));
    } //get starting side from settings dialog (

    public Colors getTeam() {
        return Colors.valueOf(sharedPref.getString("auton_team_color", "BLUE"));

    } //get team from settings dialog

    public void offsetPosition(double X, double Y, double YAW) {
        x = X;
        y = Y;
        initYaw = YAW;
    }


    /**
     * wait <i>delay</i> seconds, then go on
     */
    public void waitAllianceTeamDelay() {
        waitTime(getAllyDelay());
    }

    SharedPreferences sharedPref;

    public int getAllyDelay() {
        return (int) Utils.getSafeDoublePref("auton_beacon_area_clear_time", sharedPref, 5);
    }
//what does YAW give you -->

    /**
     * @param finalX   destination X coordinate
     * @param finalY   destination Y coordinate
     * @param finalYAW destination YAW, set -1 for no destination YAW
     */

    public void navigateTo(double finalX, double finalY, double finalYAW) {
        turnTo(Math.atan((finalY - getGyroY()) / (finalX - getGyroX())));
        goForward(Math.abs((finalX - getGyroX()) * (finalX - getGyroX()) + (finalY - getGyroY()) * (finalY - getGyroY())));
        turnTo(finalYAW);

    }

    /*public void navigateTo(double X, double Y, double YAW) {
        doPeriodicTasks();
        curX = getGyroX();
        curY = getGyroY();
        curYAW = getGyroYAW();
        long sTime = System.nanoTime();
        double bearing2Dest = Math.atan2(X - curX, Y - curY);
        while (!(Math.abs(curX - X) <= 0.05) || !(Math.abs(curY - Y) <= 0.05)) {
            while (Math.abs(bearing2Dest - curYAW) <= 2.5) {
                bearing2Dest = Math.atan2(X - curX, Y - curY);
                if (bearing2Dest - curYAW < 0)
                    turnTo(curYAW + 0.1);
                else
                    turnTo(curYAW - 0.1);
            }
            // divide by half a second
            goForward(0.01, Math.min(Math.min((System.nanoTime() - sTime) / 500000000.0, 1.0), 2 * Math.hypot(curX - X, curY - Y)));
            doPeriodicTasks();
            curX = getGyroX();
            curY = getGyroY();
            curYAW = getGyroYAW();

        }
        if (YAW >= 0) {
            if (((curYAW > YAW) && (curYAW - YAW) < 180) || ((curYAW < YAW) && (curYAW - YAW) > 180)) {
                while ((curYAW - YAW) > 2.5 || (curYAW - YAW) < 357.5) {
                    turnTo(YAW);
                }
            } else {
                while ((YAW - curYAW) > 2.5 || (YAW - curYAW) < 357.5) {
                    turnTo(YAW);
                }
            }
        }
    }*/
    enum GoalPos {
        /*
                <item>"MTN"</item>
        <item>"PARK"</item>
        <item>"BEACON_BEHIND"</item>
        <item>"BEACON_INFRONT"</item>
         */
        MTN, PARK, BEACON_BEHIND, BEACON_INFRONT, AUX1, AUX2, AUX3, AUX4, AUX5, AUX6, AUX7, AUX8;
    }
    public void detectAndHitBeacon() throws InterruptedException {
        ledCtrl.setPower(0);
        waitTime(1000);
        if (getTeam() == Colors.BLUE) {
            while ((!cb.getState().equals("RB")) && (!cb.getState().equals("BR"))) {
                setLeftSpeed(-0.07);
                setRightSpeed(-0.07);
                doPeriodicTasks();
            }
            setLeftSpeed(0);
            setRightSpeed(0);
            dumpClimbers();
            if (cb.getState().equals("RB")) {
                setLeftSpeed(-0.07);
                setRightSpeed(-0.07);
                while (true) {
                    if (cb.getState().equals("BB")) {
                        waitTime(Utils.safeInt(sharedPref.getString("camera_undershoot_correction", "0"), 0));
                        break;
                    }
                    if (cb.getState().equals("RR")) {
                        return;
                    }
                    if (cb.getState().contains("G")) {
                        return;
                    }

                }

                setLeftSpeed(0.0);
                setRightSpeed(0.0);
                Log.e("MADEIT", "MADEIT");
                pushButton();
            } else {
                setLeftSpeed(0.07);
                setRightSpeed(0.07);
                while (true) {
                    if (cb.getState().equals("BB")) {
                        waitTime(Utils.safeInt(sharedPref.getString("camera_undershoot_correction", "0"), 0));
                        break;
                    }
                    if (cb.getState().equals("RR")) {
                        return;
                    }
                    if (cb.getState().contains("G")) {
                        return;
                    }
                }
                setLeftSpeed(0.0);
                setRightSpeed(0.0);

                Log.e("MADEIT", "MADEIT");
                pushButton();
            }
        } else {
            while ((!cb.getState().equals("RB")) && (!cb.getState().equals("BR"))) {
                setLeftSpeed(0.07);
                setRightSpeed(0.07);
            }
            setLeftSpeed(0);
            setRightSpeed(0);

            dumpClimbers();
            if (cb.getState().equals("RB")) {
                setLeftSpeed(0.07);
                setRightSpeed(0.07);
                while (true) {
                    if (cb.getState().equals("RR")) {
                        waitTime(Utils.safeInt(sharedPref.getString("camera_undershoot_correction", "0"), 0));
                        break;
                    }
                    if (cb.getState().equals("BB")) {
                        return;
                    }
                    if (cb.getState().contains("G")) {
                        return;
                    }
                }
                setLeftSpeed(0.0);
                setRightSpeed(0.0);
                pushButton();
            } else {
                setLeftSpeed(-0.07);
                setRightSpeed(-0.07);
                while (true) {
                    if (cb.getState().equals("RR")) {
                        waitTime(Utils.safeInt(sharedPref.getString("camera_undershoot_correction", "0"), 0));
                        break;
                    }
                    if (cb.getState().equals("BB")) {
                        return;
                    }
                    if (cb.getState().contains("G")) {
                        return;
                    }
                }
                setLeftSpeed(0.0);
                setRightSpeed(0.0);
                pushButton();
                ledCtrl.setPower(1.0);
            }
        }




    }

    protected void dumpClimbers() throws InterruptedException {
        for(double d = 1.0; d >= 0.09; d-=.03) {
            boxSrvo.setPosition(d);
            idle();
        }
        idle();
        waitTime(500);
        idle();
        for(double d = 0.09; d <= 1.0; d+=.1) {
            boxSrvo.setPosition(d);
            idle();
        }
        idle();
        waitTime(100);
    }

    protected void pushButton() {
        ledCtrl.setPower(1.0);
        waitTime(200);
        ledCtrl.setPower(0.0);
        waitTime(200);
        ledCtrl.setPower(1.0);
        int compensationRunTime = Utils.safeInt(sharedPref.getString("camera_to_btn_offset", "0"), 0);
        setLeftSpeed(0.1 * Math.signum(compensationRunTime));
        setRightSpeed(0.1 * Math.signum(compensationRunTime));
        try {
            Thread.sleep(Math.abs(compensationRunTime));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setLeftSpeed(0.0);
        setRightSpeed(0.0);
        ledCtrl.setPower(0.0);
        for (double pos = BTN_SRVO_RETRACTED; pos >= BTN_SRVO_DEPLOYED; pos -= 0.06) {
            btnSrvo.setPosition(Math.max(BTN_SRVO_DEPLOYED, pos));
            Log.i("POS", "POS"+pos);
            try {
                idle();
                waitTime(4);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        for (double pos = BTN_SRVO_DEPLOYED; pos <= BTN_SRVO_RETRACTED; pos += 0.06) {
            btnSrvo.setPosition(Math.min(BTN_SRVO_RETRACTED, pos));
            try {
                idle();

                waitTime(4);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isFarMountain() {
        return sharedPref.getString("auton_ramp_selection", "FAR").equals("FAR");
    }

    public enum Side {
        MOUNTAIN, MIDLINE // returns whether you are starting close to the
    }

    public enum Colors {
        RED, BLUE
    }

    public enum Direction {
        RIGHT, LEFT, FORWARDS, BACKWARDS
    }

    Servo btnSrvo;
    Servo boxSrvo;
    protected void startUpHardware() throws InterruptedException {
        l0 = hardwareMap.dcMotor.get("l0");
        r0 = hardwareMap.dcMotor.get("r0");

        l1 = hardwareMap.dcMotor.get("l1");
        r1 = hardwareMap.dcMotor.get("r1");

        aimServo = hardwareMap.servo.get(DeviceNaming.AIM_SERVO);
        aimServo.setPosition(0.8);
        w = hardwareMap.dcMotor.get("w");
        boxSrvo = hardwareMap.servo.get(DeviceNaming.BOX_SERVO);
        btnSrvo = hardwareMap.servo.get(DeviceNaming.BUTTON_SERVO);
        btnSrvo.setPosition(BTN_SRVO_RETRACTED);
        hardwareMap.servo.get(DeviceNaming.L_LEVER_SERVO).setPosition(0.036);
        hardwareMap.servo.get(DeviceNaming.R_LEVER_SERVO).setPosition(0.931);
        boxSrvo.setPosition(1.0);

        l0.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        l1.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        r0.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        r1.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        w.setChannelMode(DcMotorController.RunMode.RUN_WITHOUT_ENCODERS);
        l0.setDirection(DcMotor.Direction.REVERSE);
        l1.setDirection(DcMotor.Direction.REVERSE);
        ledCtrl = hardwareMap.dcMotor.get(DeviceNaming.LED_DEV_NAME);
        ledCtrl.setPower(1.0);

        lastEncoderL = l0.getCurrentPosition();
        lastEncoderR = r0.getCurrentPosition();
        gyroHelper.startUpGyro();
        String gc = sharedPref.getString("gyrocalib", "!!");
        if(gc.matches("([0-9a-f]{2})*")) {
            try {
                gyroHelper.getImu().writeCalibrationData(Hex.decodeHex(gc.toCharArray()));
            } catch (DecoderException e) {
                throw new RuntimeException("EMERG-STOP: CANNOT CALIBRATE GYRO");
            }
        }
        idle();
        composeDashboard();
    }


    public double getGyroX() {


        doPeriodicTasks();
        return x;
    }

    public double getGyroY() {

        doPeriodicTasks();
        return y;
    }

    public double getGyroYAW() {

        return normalizeDegrees(gyroHelper.getAngles().heading - initYaw);
    }

    double initYaw = 0;
    double x = 0;
    double y = 0;
    int lastEncoderL = 0;
    int lastEncoderR = 0;

    public void doPeriodicTasks() {
        Log.w("TRACK", "ENTER DO-PERIODIC");
        gyroHelper.update();

        int l0p = l0.getCurrentPosition();
        int r0p = r0.getCurrentPosition();

        if(lastRunWasLite) {
            lastEncoderL = l0p;
            lastEncoderR = r0p;
            lastRunWasLite = false;
        }


        int delta = weightPositionEffects(l0p - lastEncoderL, r0p - lastEncoderR);
        //int delta = weightPositionEffects(uEL[1], uER[1]);
        double dist = remapWheelDiameter(delta);
        x += (dist * Math.cos(getGyroYAW()));
        y += (dist * Math.sin(getGyroYAW()));
        lastEncoderL = l0p;
        lastEncoderR = r0p;

        Log.i("MOVE", "DIST: "+dist);
        // The rest of this is pretty cheap to acquire, but we may as well do it
        // all while we're gathering the above.
        loopCycles = getLoopCount();
        i2cCycles = ((II2cDeviceClientUser) gyroHelper.getImu()).getI2cDeviceClient().getI2cCycleCount();
        ms = gyroHelper.getElapsed().time() * 1000.0;
        try {
            idle();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.w("TRACK", "EXIT DO-PERIODIC");
    }

    boolean lastRunWasLite = false;


    public void doTurnOnlyTasks() {
        lastRunWasLite = true;
        Log.w("TRACK", "ENTER DO-PERIODIC");
        gyroHelper.update();

        int delta = 0;
        //int delta = weightPositionEffects(uEL[1], uER[1]);
        double dist = remapWheelDiameter(delta);
        x += (dist * Math.cos(getGyroYAW()));
        y += (dist * Math.sin(getGyroYAW()));

        Log.i("MOVE", "DIST: "+dist);
        // The rest of this is pretty cheap to acquire, but we may as well do it
        // all while we're gathering the above.
        loopCycles = getLoopCount();
        i2cCycles = ((II2cDeviceClientUser) gyroHelper.getImu()).getI2cDeviceClient().getI2cCycleCount();
        ms = gyroHelper.getElapsed().time() * 1000.0;
        try {
            idle();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.w("TRACK", "EXIT DO-PERIODIC");
    }

    private double remapWheelDiameter(int delta) {
        // 4.000 inches
        return 4.000 * Math.PI * delta / 1440;
    }

    private int weightPositionEffects(int l, int r) {
        return (l + r) / 2;
    }

    int loopCycles;
    int i2cCycles;
    double ms;

    void composeDashboard() {
        // The default dashboard update rate is a little to slow for us, so we update faster
        telemetry.setUpdateIntervalMs(200);

        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() {
            @Override
            public void run() {
                // The rest of this is pretty cheap to acquire, but we may as well do it
                // all while we're gathering the above.
                loopCycles = getLoopCount();
                i2cCycles = ((II2cDeviceClientUser) gyroHelper.getImu()).getI2cDeviceClient().getI2cCycleCount();
                ms = gyroHelper.getElapsed().time() * 1000.0;
            }
        });
        telemetry.addLine(
                telemetry.item("loop count: ", new IFunc<Object>() {
                    public Object value() {
                        return loopCycles;
                    }
                }),
                telemetry.item("i2c cycle count: ", new IFunc<Object>() {
                    public Object value() {
                        return i2cCycles;
                    }
                }));

        telemetry.addLine(
                telemetry.item("loop rate: ", new IFunc<Object>() {
                    public Object value() {
                        return formatRate(ms / loopCycles);
                    }
                }),
                telemetry.item("i2c cycle rate: ", new IFunc<Object>() {
                    public Object value() {
                        return formatRate(ms / i2cCycles);
                    }
                }));

        telemetry.addLine(
                telemetry.item("status: ", new IFunc<Object>() {
                    public Object value() {
                        return decodeStatus(gyroHelper.getImu().getSystemStatus());
                    }
                }),
                telemetry.item("calib: ", new IFunc<Object>() {
                    public Object value() {
                        return decodeCalibration(gyroHelper.getImu().read8(IBNO055IMU.REGISTER.CALIB_STAT));
                    }
                }));

        telemetry.addLine(
                telemetry.item("heading: ", new IFunc<Object>() {
                    public Object value() {
                        return formatAngle(gyroHelper.getAngles().heading);
                    }
                }),
                telemetry.item("roll: ", new IFunc<Object>() {
                    public Object value() {
                        return formatAngle(gyroHelper.getAngles().roll);
                    }
                }),
                telemetry.item("pitch: ", new IFunc<Object>() {
                    public Object value() {
                        return formatAngle(gyroHelper.getAngles().pitch);
                    }
                }));

        telemetry.addLine(
                telemetry.item("x: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(gyroHelper.getPosition().x);
                    }
                }),
                telemetry.item("y: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(gyroHelper.getPosition().y);
                    }
                }),
                telemetry.item("z: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(gyroHelper.getPosition().z);
                    }
                }));
        telemetry.addLine(
                telemetry.item("X!!: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(getGyroX());
                    }
                }),
                telemetry.item("Y!!: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(getGyroY());
                    }
                }),
                telemetry.item("YAW!!: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(getGyroYAW());
                    }
                }));
        telemetry.addLine(
                telemetry.item("cal: ", new IFunc<Object>() {
                    public Object value() {
                        return gyroHelper.getImu().isSystemCalibrated();
                    }
                })
        );
        telemetry.addLine(
                telemetry.item("xa: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(gyroHelper.getAccel().accelX);
                    }
                }),
                telemetry.item("ya: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(gyroHelper.getAccel().accelY);
                    }
                }),
                telemetry.item("za: ", new IFunc<Object>() {
                    public Object value() {
                        return formatPosition(gyroHelper.getAccel().accelZ);
                    }
                }));

        telemetry.addLine(
                telemetry.item("STATE: ", new IFunc<Object>() {
                    public Object value() {
                        return cb.getState();
                    }
                }));

    }

    String formatAngle(double angle) {
        return gyroHelper.getParameters().angleunit == IBNO055IMU.ANGLEUNIT.DEGREES ? formatDegrees(angle) : formatRadians(angle);
    }



    String formatRadians(double radians) {
        return formatDegrees(degreesFromRadians(radians));
    }

    String formatDegrees(double degrees) {
        return String.format("%.1f", normalizeDegrees(degrees));
    }

    String formatRate(double cyclesPerSecond) {
        return String.format("%.2f", cyclesPerSecond);
    }

    String formatPosition(double coordinate) {
        String unit = gyroHelper.getParameters().accelunit == IBNO055IMU.ACCELUNIT.METERS_PERSEC_PERSEC
                ? "m" : "??";
        return String.format("%.2f%s", coordinate, unit);
    }

    /**
     * Normalize the angle into the range [-180,180)
     */
    double normalizeDegrees(double degrees) {
        while (degrees >= 360) degrees -= 360.0;
        while (degrees < 0.0) degrees += 360.0;
        return degrees;
    }

    double degreesFromRadians(double radians) {
        return radians * 180.0 / Math.PI;
    }

    /**
     * Turn a system status into something that's reasonable to show in telemetry
     */
    String decodeStatus(int status) {
        switch (status) {
            case 0:
                return "idle";
            case 1:
                return "syserr";
            case 2:
                return "periph";
            case 3:
                return "sysinit";
            case 4:
                return "selftest";
            case 5:
                return "fusion";
            case 6:
                return "running";
        }
        return "unk";
    }

    /**
     * Turn a calibration code into something that is reasonable to show in telemetry
     */
    String decodeCalibration(int status) {
        StringBuilder result = new StringBuilder();

        result.append(String.format("s%d", (status >> 6) & 0x03));  // SYS calibration status
        result.append(" ");
        result.append(String.format("g%d", (status >> 4) & 0x03));  // GYR calibration status
        result.append(" ");
        result.append(String.format("a%d", (status >> 2) & 0x03));  // ACC calibration status
        result.append(" ");
        result.append(String.format("m%d", (status >> 0) & 0x03));  // MAG calibration status

        return result.toString();
    }
}



