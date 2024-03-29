// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.math.filter.SlewRateLimiter;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.util.WPIUtilJNI;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;

import com.ctre.phoenix.sensors.Pigeon2;
import com.ctre.phoenix.sensors.Pigeon2Configuration;

import frc.robot.Util.Constants.DriveConstants;
import frc.utils.SwerveUtils;
import frc.robot.Util.Constants.ModuleConstants;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class DriveSubsystem extends SubsystemBase {
  // Create MAXSwerveModules
  private final MAXSwerveModule m_frontLeft = new MAXSwerveModule(
      DriveConstants.kFrontLeftDrivingCanId,
      DriveConstants.kFrontLeftTurningCanId,
      DriveConstants.kFrontLeftChassisAngularOffset);

  private final MAXSwerveModule m_frontRight = new MAXSwerveModule(
      DriveConstants.kFrontRightDrivingCanId,
      DriveConstants.kFrontRightTurningCanId,
      DriveConstants.kFrontRightChassisAngularOffset);

  private final MAXSwerveModule m_rearLeft = new MAXSwerveModule(
      DriveConstants.kRearLeftDrivingCanId,
      DriveConstants.kRearLeftTurningCanId,
      DriveConstants.kBackLeftChassisAngularOffset);

  private final MAXSwerveModule m_rearRight = new MAXSwerveModule(
      DriveConstants.kRearRightDrivingCanId,
      DriveConstants.kRearRightTurningCanId,
      DriveConstants.kBackRightChassisAngularOffset);

  private final MAXSwerveModule[] SwerveModules = {m_frontLeft,m_frontRight,m_rearLeft,m_rearRight};

  

  // The gyro sensor
  private final Pigeon2 pigeonGyro = new Pigeon2(DriveConstants.kPigeonGyroPort);

  // Slew rate filter variables for controlling lateral acceleration
  private double m_currentRotation = 0.0;
  private double m_currentTranslationDir = 0.0;
  private double m_currentTranslationMag = 0.0;

  private SlewRateLimiter m_magLimiter = new SlewRateLimiter(DriveConstants.kMagnitudeSlewRate);
  private SlewRateLimiter m_rotLimiter = new SlewRateLimiter(DriveConstants.kRotationalSlewRate);
  private double m_prevTime = WPIUtilJNI.now() * 1e-6;

  // Odometry class for tracking robot pose
  SwerveDriveOdometry m_odometry = new SwerveDriveOdometry(
      DriveConstants.kDriveKinematics,
      Rotation2d.fromDegrees(pigeonGyro.getYaw()),
      new SwerveModulePosition[] {
          m_frontLeft.getPosition(),
          m_frontRight.getPosition(),
          m_rearLeft.getPosition(),
          m_rearRight.getPosition()
      });
  
  private double currentTurningkP = ModuleConstants.kTurningP;
  private double currentTurningkI = ModuleConstants.kTurningI;
  private double currentTurningkD = ModuleConstants.kTurningD;

  private double currentDrivingkP = ModuleConstants.kDrivingP;
  private double currentDrivingkI = ModuleConstants.kDrivingI;
  private double currentDrivingkD = ModuleConstants.kDrivingD;
  private double currentDrivingkFF = ModuleConstants.kDrivingFF;

  private ShuffleboardTab tab = Shuffleboard.getTab("TestTab");
  private GenericEntry TurningPEntry = tab.addPersistent("TurningkP", ModuleConstants.kTurningP).getEntry();
  private GenericEntry TurningIEntry = tab.addPersistent("TurningkI", ModuleConstants.kTurningI).getEntry();
  private GenericEntry TurningDEntry = tab.addPersistent("TurningkD", ModuleConstants.kTurningD).getEntry();
  private GenericEntry DrivingPEntry = tab.addPersistent("DrivingkP", ModuleConstants.kDrivingP).getEntry();
  private GenericEntry DrivingIEntry = tab.addPersistent("DrivingkI", ModuleConstants.kDrivingI).getEntry();
  private GenericEntry DrivingDEntry = tab.addPersistent("DrivingkD", ModuleConstants.kDrivingD).getEntry();
  private GenericEntry DrivingFFEntry = tab.addPersistent("DrivingkFF", ModuleConstants.kDrivingFF).getEntry();
  private GenericEntry SwerveAngleEntry = tab.add("SwerveAngle", 0.0).getEntry();
  private GenericEntry SwerveVelocityEntry = tab.add("SwerveVelocity", 0.0).getEntry();
  private GenericEntry SwervePositionEntry = tab.add("SwervePosition", 0.0).getEntry();
  private GenericEntry GyroAngleEntry = tab.add("GyroAngle", 0.0).getEntry();

  


  /** Creates a new DriveSubsystem. */
  public DriveSubsystem() {
    Pigeon2Configuration config = new Pigeon2Configuration();
    config.MountPosePitch = 0;
    config.MountPoseRoll = 0;
    config.MountPoseYaw = 0;
    pigeonGyro.configAllSettings(config);

  }

  @Override
  public void periodic() {

    double turningP = TurningPEntry.getDouble(currentTurningkP); 
    double turningI = TurningIEntry.getDouble(currentTurningkI);
    double turningD = TurningDEntry.getDouble(currentTurningkD);
    double drivingP = DrivingPEntry.getDouble(currentDrivingkP);
    double drivingI = DrivingIEntry.getDouble(currentDrivingkI);
    double drivingD = DrivingDEntry.getDouble(currentDrivingkD);
    double drivingFF = DrivingFFEntry.getDouble(currentDrivingkFF);
     if (turningP != currentTurningkP || turningI != currentTurningkD || turningD != currentTurningkD){
        currentTurningkP = turningP;
        currentTurningkI = turningI;
        currentTurningkD = turningD;
        setTurningPID();
     }
     if (drivingP != currentDrivingkP || drivingI != currentDrivingkI || drivingD != currentDrivingkD || drivingFF != currentDrivingkFF){
        currentDrivingkP = drivingP;
        currentDrivingkI = drivingI;
        currentDrivingkD = drivingD;
        currentDrivingkFF = drivingFF;
        setDrivingPIDF();
     }
    SwerveAngleEntry.setDouble(m_frontLeft.getStateAngle());
    SwerveVelocityEntry.setDouble(m_frontLeft.getStateVelocity());
    SwervePositionEntry.setDouble(m_frontLeft.getPositionMeters());
    GyroAngleEntry.setDouble(pigeonGyro.getYaw());
    // Update the odometry in the periodic block
    m_odometry.update(
        Rotation2d.fromDegrees(pigeonGyro.getYaw()),
        new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
        });
  }

  public void setTurningPID(){
    for (MAXSwerveModule module: SwerveModules){
      module.getTurningPIDController().setP(currentTurningkP);
      module.getTurningPIDController().setI(currentTurningkI);
      module.getTurningPIDController().setD(currentTurningkD);
    }
  }

  public void setDrivingPIDF(){
    for (MAXSwerveModule module: SwerveModules){
      module.getDrivingPIDController().setP(currentDrivingkP);
      module.getDrivingPIDController().setI(currentDrivingkI);
      module.getDrivingPIDController().setD(currentDrivingkD);
      module.getDrivingPIDController().setFF(currentDrivingkFF);
    }

  }

  /**
   * Returns the currently-estimated pose of the robot.
   *
   * @return The pose.
   */
  public Pose2d getPose() {
    return m_odometry.getPoseMeters();
  }

  /**
   * Resets the odometry to the specified pose.
   *
   * @param pose The pose to which to set the odometry.
   */
  public void resetOdometry(Pose2d pose) {
    m_odometry.resetPosition(
        Rotation2d.fromDegrees(pigeonGyro.getYaw()),
        new SwerveModulePosition[] {
            m_frontLeft.getPosition(),
            m_frontRight.getPosition(),
            m_rearLeft.getPosition(),
            m_rearRight.getPosition()
        },
        pose);
  }

  /**
   * Method to drive the robot using joystick info.
   *
   * @param xSpeed        Speed of the robot in the x direction (forward).
   * @param ySpeed        Speed of the robot in the y direction (sideways).
   * @param rot           Angular rate of the robot.
   * @param fieldRelative Whether the provided x and y speeds are relative to the
   *                      field.
   * @param rateLimit     Whether to enable rate limiting for smoother control.
   */
  public void drive(double xSpeed, double ySpeed, double rot, boolean fieldRelative, boolean rateLimit) {
    
    double xSpeedCommanded;
    double ySpeedCommanded;

    if (rateLimit) {
      // Convert XY to polar for rate limiting
      double inputTranslationDir = Math.atan2(ySpeed, xSpeed);
      double inputTranslationMag = Math.sqrt(Math.pow(xSpeed, 2) + Math.pow(ySpeed, 2));

      // Calculate the direction slew rate based on an estimate of the lateral acceleration
      double directionSlewRate;
      if (m_currentTranslationMag != 0.0) {
        directionSlewRate = Math.abs(DriveConstants.kDirectionSlewRate / m_currentTranslationMag);
      } else {
        directionSlewRate = 500.0; //some high number that means the slew rate is effectively instantaneous
      }
      

      double currentTime = WPIUtilJNI.now() * 1e-6;
      double elapsedTime = currentTime - m_prevTime;
      double angleDif = SwerveUtils.AngleDifference(inputTranslationDir, m_currentTranslationDir);
      if (angleDif < 0.45*Math.PI) {
        m_currentTranslationDir = SwerveUtils.StepTowardsCircular(m_currentTranslationDir, inputTranslationDir, directionSlewRate * elapsedTime);
        m_currentTranslationMag = m_magLimiter.calculate(inputTranslationMag);
      }
      else if (angleDif > 0.85*Math.PI) {
        if (m_currentTranslationMag > 1e-4) { //some small number to avoid floating-point errors with equality checking
          // keep currentTranslationDir unchanged
          m_currentTranslationMag = m_magLimiter.calculate(0.0);
        }
        else {
          m_currentTranslationDir = SwerveUtils.WrapAngle(m_currentTranslationDir + Math.PI);
          m_currentTranslationMag = m_magLimiter.calculate(inputTranslationMag);
        }
      }
      else {
        m_currentTranslationDir = SwerveUtils.StepTowardsCircular(m_currentTranslationDir, inputTranslationDir, directionSlewRate * elapsedTime);
        m_currentTranslationMag = m_magLimiter.calculate(0.0);
      }
      m_prevTime = currentTime;
      
      xSpeedCommanded = m_currentTranslationMag * Math.cos(m_currentTranslationDir);
      ySpeedCommanded = m_currentTranslationMag * Math.sin(m_currentTranslationDir);
      m_currentRotation = m_rotLimiter.calculate(rot);


    } else {
      xSpeedCommanded = xSpeed;
      ySpeedCommanded = ySpeed;
      m_currentRotation = rot;
    }

    // Convert the commanded speeds into the correct units for the drivetrain
    double xSpeedDelivered = xSpeedCommanded * DriveConstants.kMaxSpeedMetersPerSecond;
    double ySpeedDelivered = ySpeedCommanded * DriveConstants.kMaxSpeedMetersPerSecond;
    double rotDelivered = m_currentRotation * DriveConstants.kMaxAngularSpeed;

    var swerveModuleStates = DriveConstants.kDriveKinematics.toSwerveModuleStates(
        fieldRelative
            ? ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, Rotation2d.fromDegrees(pigeonGyro.getYaw()))
            : new ChassisSpeeds(xSpeed, ySpeed, rot));
    SwerveDriveKinematics.desaturateWheelSpeeds(
        swerveModuleStates, DriveConstants.kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(swerveModuleStates[0]);
    m_frontRight.setDesiredState(swerveModuleStates[1]);
    m_rearLeft.setDesiredState(swerveModuleStates[2]);
    m_rearRight.setDesiredState(swerveModuleStates[3]);
  }

  /**
   * Sets the wheels into an X formation to prevent movement.
   */
  public void setX() {
    m_frontLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
    m_frontRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    m_rearLeft.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(-45)));
    m_rearRight.setDesiredState(new SwerveModuleState(0, Rotation2d.fromDegrees(45)));
  }

  /**
   * Sets the swerve ModuleStates.
   *
   * @param desiredStates The desired SwerveModule states.
   */
  public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(
        desiredStates, DriveConstants.kMaxSpeedMetersPerSecond);
    m_frontLeft.setDesiredState(desiredStates[0]);
    m_frontRight.setDesiredState(desiredStates[1]);
    m_rearLeft.setDesiredState(desiredStates[2]);
    m_rearRight.setDesiredState(desiredStates[3]);
  }

  /** Resets the drive encoders to currently read a position of 0. */
  public void resetEncoders() {
    m_frontLeft.resetEncoders();
    m_rearLeft.resetEncoders();
    m_frontRight.resetEncoders();
    m_rearRight.resetEncoders();
  }

  /** Zeroes the heading of the robot. */
  public void zeroHeading() {
    pigeonGyro.setYaw(0);
    pigeonGyro.setAccumZAngle(0);

  }

  /**
   * Returns the heading of the robot.
   *
   * @return the robot's heading in degrees, from -180 to 180
   */
  public double getHeading() {
    return Rotation2d.fromDegrees(pigeonGyro.getYaw()).getDegrees();
  }

  /**
   * Returns the turn rate of the robot.
   *
   * @return The turn rate of the robot, in degrees per second
   */
  public double getTurnRate() {
    return pigeonGyro.getHandle() * (DriveConstants.kGyroReversed ? -1.0 : 1.0);
  }
}
