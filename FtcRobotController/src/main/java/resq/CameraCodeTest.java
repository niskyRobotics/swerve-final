package resq;

import android.preference.PreferenceManager;
import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Created by akh06977 on 12/12/2015.
 */
public class CameraCodeTest extends ResqAuton {
    public void main() throws InterruptedException {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        fillInSettings();
        startUpHardware();

        aimServo.setPosition(0.32);
        l0.setDirection(DcMotor.Direction.REVERSE);
        l1.setDirection(DcMotor.Direction.REVERSE);
        r0.setDirection(DcMotor.Direction.FORWARD);
        r1.setDirection(DcMotor.Direction.FORWARD);
        startCamera();
        this.waitForStart();
        Thread.sleep(1000);
        // TEST AUTON TO SEE IF BACKEND WORKS

        detectAndHitBeacon();
        //go to front of mountain, facing the mountain



    }
}
