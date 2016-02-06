package resq;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;

/**
 * Created by akh06977 on 12/15/2015.
 */
public class EncoderTest extends RectResqCommon {
    boolean md = true;
    @Override
    public void loop() {
        if(md) {
            l0.setChannelMode(DcMotorController.RunMode.RUN_USING_ENCODERS);
            r0.setChannelMode(DcMotorController.RunMode.RUN_USING_ENCODERS);

            r0.setDirection(DcMotor.Direction.REVERSE);
            r1.setDirection(DcMotor.Direction.REVERSE);
            l0.setPower(0.3);
            r0.setPower(0.3);
            md = false;
        }
        else {

            l0.getController().setMotorControllerDeviceMode(DcMotorController.DeviceMode.READ_ONLY);
            if(l0.getController().getMotorControllerDeviceMode()== DcMotorController.DeviceMode.READ_ONLY){
                telemetry.addData("ENCL", l0.getCurrentPosition());
                telemetry.addData("ENCR", r0.getCurrentPosition());
            }
        }
    }
}
