package resq;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.swerverobotics.library.ClassFactory;
import org.swerverobotics.library.interfaces.Acceleration;
import org.swerverobotics.library.interfaces.EulerAngles;
import org.swerverobotics.library.interfaces.IBNO055IMU;
import org.swerverobotics.library.interfaces.Position;

public class GyroHelper {
    private final OpMode backingOpMode;
    IBNO055IMU imu;

    public IBNO055IMU getImu() {
        return imu;
    }

    ElapsedTime elapsed = new ElapsedTime();

    public ElapsedTime getElapsed() {
        return elapsed;
    }

    IBNO055IMU.Parameters parameters = new IBNO055IMU.Parameters();

    public IBNO055IMU.Parameters getParameters() {
        return parameters;
    }

    // Here we have state we use for updating the dashboard. The first of these is important
    // to read only once per update, as its acquisition is expensive. The remainder, though,
    // could probably be read once per item, at only a small loss in display accuracy.
    EulerAngles angles;

    public EulerAngles getAngles() {
        return angles;
    }


    Position position;

    public Position getPosition() {
        return position;
    }



    Acceleration accel;

    public Acceleration getAccel() {
        return accel;
    }


    Acceleration rawAccel;

    public Acceleration getRawAccel() {
        return rawAccel;
    }

    public GyroHelper(OpMode backingOpMode) {
        this.backingOpMode = backingOpMode;
    }

    public void update(){
        this.angles = (getImu().getAngularOrientation());
        this.position = (getImu().getPosition());
        this.accel = (getImu().getLinearAcceleration());
        this.rawAccel = getImu().getOverallAcceleration();
    }

    public boolean isGyroCalibrated(){
        int status = imu.read8(IBNO055IMU.REGISTER.CALIB_STAT);
        return ((((status >> 4) & 0x03) == 0x03) );

    }
    public void startUpGyro() {
        parameters.angleunit = IBNO055IMU.ANGLEUNIT.DEGREES;
        parameters.accelunit = IBNO055IMU.ACCELUNIT.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled = true;
        parameters.mode = IBNO055IMU.SENSOR_MODE.NDOF;
        parameters.loggingTag = "BNO055";
        imu = ClassFactory.createAdaFruitBNO055IMU(backingOpMode.hardwareMap.i2cDevice.get("bno055"), parameters);

    }
}