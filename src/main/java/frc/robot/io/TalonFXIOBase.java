package frc.robot.io;

import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularAcceleration;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Temperature;
import edu.wpi.first.units.measure.Voltage;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.MotionMagicTorqueCurrentFOC;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.ControlModeValue;

import frc.robot.Constants;

// TalonFXIO implementation that interfaces with a physical TalonFX. In sim this interfaces with a simulated TalonFX, so
// TalonFXIOSim extends this.
public class TalonFXIOBase extends TalonFXIO {
    // Debounce to make sure motor disconnects are real
    private Debouncer connectedDebounce = new Debouncer(Constants.debounceTime);

    // The actual TalonFX
    private TalonFX motor;

    // A bunch of StatusSignals that monitor the motor
    private StatusSignal<Angle> position;
    private StatusSignal<AngularVelocity> velocity;
    private StatusSignal<AngularAcceleration> accel;

    private StatusSignal<Voltage> appliedVoltage;
    private StatusSignal<Voltage> supplyVoltage;
    private StatusSignal<Current> supplyCurrent;
    private StatusSignal<Current> torqueCurrent;

    private StatusSignal<ControlModeValue> controlMode;

    private StatusSignal<Double> closedLoopReference;
    private StatusSignal<Double> closedLoopError;
    private StatusSignal<Double> feedforward;
    private StatusSignal<Double> derivOutput;
    private StatusSignal<Double> intOutput;
    private StatusSignal<Double> propOutput;

    private StatusSignal<Temperature> temp;

    private StatusSignal<Double> dutyCycle;

    private StatusSignal<Boolean> hardwareFault;
    private StatusSignal<Boolean> procTempFault;
    private StatusSignal<Boolean> deviceTempFault;
    private StatusSignal<Boolean> undervoltage;
    private StatusSignal<Boolean> bootDuringEnable;
    private StatusSignal<Boolean> forwardHardLimit;
    private StatusSignal<Boolean> forwardSoftLimit;
    private StatusSignal<Boolean> reverseHardLimit;
    private StatusSignal<Boolean> reverseSoftLimit;

    public TalonFXIOBase(int motorId, String canBus) {
        motor = new TalonFX(motorId, canBus);

        // Initialize all the StatusSignals
        position = motor.getPosition();
        velocity = motor.getVelocity();
        accel = motor.getAcceleration();

        appliedVoltage = motor.getMotorVoltage();
        supplyVoltage = motor.getSupplyVoltage();
        supplyCurrent = motor.getSupplyCurrent();
        torqueCurrent = motor.getTorqueCurrent();

        controlMode = motor.getControlMode();

        closedLoopReference = motor.getClosedLoopReference();
        closedLoopError = motor.getClosedLoopError();
        feedforward = motor.getClosedLoopFeedForward();
        derivOutput = motor.getClosedLoopDerivativeOutput();
        intOutput = motor.getClosedLoopIntegratedOutput();
        propOutput = motor.getClosedLoopProportionalOutput();

        temp = motor.getDeviceTemp();
        dutyCycle = motor.getDutyCycle();

        hardwareFault = motor.getFault_Hardware();
        procTempFault = motor.getFault_ProcTemp();
        deviceTempFault = motor.getFault_DeviceTemp();
        undervoltage = motor.getFault_Undervoltage();
        bootDuringEnable = motor.getFault_BootDuringEnable();
        forwardHardLimit = motor.getFault_ForwardHardLimit();
        forwardSoftLimit = motor.getFault_ForwardSoftLimit();
        reverseHardLimit = motor.getFault_ReverseHardLimit();
        reverseSoftLimit = motor.getFault_ReverseSoftLimit();
    }

    public TalonFXIOBase(int motorId) {
        this(motorId, "");
    }

    // Updates the TalonFXIOInputs
    @Override
    public void updateInputs(TalonFXIOInputs inputs) {
        // If we're in a simulation, update it
        updateSimulation();

        // Refresh all the signals
        BaseStatusSignal.refreshAll(
                position,
                velocity,
                accel,
                appliedVoltage,
                supplyCurrent,
                supplyVoltage,
                torqueCurrent,
                controlMode,
                closedLoopReference,
                closedLoopError,
                feedforward,
                derivOutput,
                intOutput,
                propOutput,
                temp,
                dutyCycle,
                hardwareFault,
                procTempFault,
                deviceTempFault,
                undervoltage,
                bootDuringEnable,
                forwardHardLimit,
                forwardSoftLimit,
                reverseHardLimit,
                reverseSoftLimit);

        // Update all the inputs from the signal values
        inputs.connected = connectedDebounce.calculate(motor.isConnected());

        // Some signals give rotations, so they have to be converted to radians
        inputs.positionRad = Units.rotationsToRadians(position.getValueAsDouble());
        inputs.velocityRadPerSec = Units.rotationsToRadians(velocity.getValueAsDouble());
        inputs.accelRadPerSecSquared = Units.rotationsToRadians(accel.getValueAsDouble());

        inputs.appliedVoltage = appliedVoltage.getValueAsDouble();
        inputs.supplyVoltage = supplyVoltage.getValueAsDouble();
        inputs.supplyCurrent = supplyCurrent.getValueAsDouble();
        inputs.torqueCurrent = torqueCurrent.getValueAsDouble();

        inputs.controlMode = controlMode.getValue().toString();

        inputs.setpointRad = Units.rotationsToRadians(closedLoopReference.getValueAsDouble());
        inputs.errorRad = Units.rotationsToRadians(closedLoopError.getValueAsDouble());
        inputs.feedforward = feedforward.getValueAsDouble();
        inputs.derivOutput = derivOutput.getValueAsDouble();
        inputs.intOutput = intOutput.getValueAsDouble();
        inputs.propOutput = propOutput.getValueAsDouble();

        inputs.temp = temp.getValueAsDouble();
        inputs.dutyCycle = dutyCycle.getValueAsDouble();

        inputs.hardwareFault = hardwareFault.getValue();
        inputs.procTempFault = procTempFault.getValue();
        inputs.deviceTempFault = deviceTempFault.getValue();
        inputs.undervoltageFault = undervoltage.getValue();
        inputs.bootDuringEnable = bootDuringEnable.getValue();
        inputs.forwardHardLimit = forwardHardLimit.getValue();
        inputs.forwardSoftLimit = forwardSoftLimit.getValue();
        inputs.reverseHardLimit = reverseHardLimit.getValue();
        inputs.reverseSoftLimit = reverseSoftLimit.getValue();
    }

    // Updates the simulation. Does nothing here, but can be overridden by subclasses
    public void updateSimulation() {}

    @Override
    public void applyConfig(TalonFXConfiguration config) {
        motor.getConfigurator().apply(config);
    }

    @Override
    public void setControl(DutyCycleOut control) {
        motor.setControl(control);
    }

    @Override
    public void setControl(VoltageOut control) {
        motor.setControl(control);
    }

    @Override
    public void setControl(MotionMagicTorqueCurrentFOC control) {
        motor.setControl(control);
    }

    protected TalonFX getMotor() {
        return motor;
    }
}
