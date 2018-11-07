package uav.gcs.hud;

public interface Hud {
    public void setRoll(double roll);
    public void setPitch(double pitch);
    public void setYaw(double yaw);
    public void setSystemStatus(String systemStatus);
    public void setArmed(boolean armed);
    public void setAltitude(double altitude);
    public void setBattery(double voltage, double current, double level);
    public void setGpsFixed(boolean gpsFixed);
    public void setMode(String mode);
    public void setAirSpeed(double airSpeed);
    public void setGroudSpeed(double groundSpeed);
    public void setInfo(String info);
    public void setTime(String time);
}
