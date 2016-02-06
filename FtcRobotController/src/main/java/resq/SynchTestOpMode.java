package resq;

import org.swerverobotics.library.SynchronousOpMode;

/**
 * Created by akh06977 on 10/9/2015.
 */
public class SynchTestOpMode extends SynchronousOpMode {

    private void goTo(int x, int y){

    }
    @Override
    protected void main() throws InterruptedException {
        waitForStart();
        hardwareMap.dcMotor.get("l0").setPower(1.0);
        hardwareMap.dcMotor.get("l1").setPower(1.0);
        hardwareMap.dcMotor.get("l2").setPower(1.0);
        hardwareMap.dcMotor.get("r0").setPower(1.0);
        hardwareMap.dcMotor.get("r1").setPower(1.0);
        hardwareMap.dcMotor.get("r2").setPower(1.0);
        long t = System.currentTimeMillis();
        while(System.currentTimeMillis()< t+1000){
            idle();
        }
        hardwareMap.dcMotor.get("l0").setPower(1.0);
        hardwareMap.dcMotor.get("l1").setPower(1.0);
        hardwareMap.dcMotor.get("l2").setPower(1.0);
        hardwareMap.dcMotor.get("r0").setPower(-1.0);
        hardwareMap.dcMotor.get("r1").setPower(-1.0);
        hardwareMap.dcMotor.get("r2").setPower(-1.0);
        t = System.currentTimeMillis();
        while(System.currentTimeMillis()< t+1000){
            idle();
        }
        hardwareMap.dcMotor.get("l0").setPower(0.0);
        hardwareMap.dcMotor.get("l1").setPower(0.0);
        hardwareMap.dcMotor.get("l2").setPower(0.0);
        hardwareMap.dcMotor.get("r0").setPower(0.0);
        hardwareMap.dcMotor.get("r1").setPower(0.0);
        hardwareMap.dcMotor.get("r2").setPower(0.0);
    }
}
