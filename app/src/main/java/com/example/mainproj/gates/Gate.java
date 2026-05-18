package com.example.mainproj.gates;

public class Gate {

    private String gateNumber;
    private String state;
    private String actionStatus;
    private long lastAckAt;
    private String lastCommandId;
    private boolean isClickable = true;
    private int visibilityLevel = 0;
    private int openableLevel = 0;
    private int controlLevel = 0;
    private boolean disabled = false;

    public Gate(String gateNumber, String state, String actionStatus, long lastAckAt, String lastCommandId) {
        this.gateNumber = gateNumber;
        this.state = state;
        this.actionStatus = actionStatus;
        this.lastAckAt = lastAckAt;
        this.lastCommandId = lastCommandId;
    }

    public Gate(String gateNumber, String state, String actionStatus, long lastAckAt, String lastCommandId, int visibilityLevel, int openableLevel, int controlLevel) {
        this.gateNumber = gateNumber;
        this.state = state;
        this.actionStatus = actionStatus;
        this.lastAckAt = lastAckAt;
        this.lastCommandId = lastCommandId;
        this.visibilityLevel = visibilityLevel;
        this.openableLevel = openableLevel;
        this.controlLevel = controlLevel;
    }

    public String getGateNumber() {
        return gateNumber;
    }

    public String getState() {
        return state;
    }

    public String getActionStatus() {
        return actionStatus;
    }

    public long getLastAckAt() {
        return lastAckAt;
    }

    public String getLastCommandId() {
        return lastCommandId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public void setLastAckAt(long lastAckAt) {
        this.lastAckAt = lastAckAt;
    }

    public void setLastCommandId(String lastCommandId) {
        this.lastCommandId = lastCommandId;
    }

    public int getVisibilityLevel() {
        return visibilityLevel;
    }

    public void setVisibilityLevel(int visibilityLevel) {
        this.visibilityLevel = visibilityLevel;
    }

    public int getOpenableLevel() {
        return openableLevel;
    }

    public void setOpenableLevel(int openableLevel) {
        this.openableLevel = openableLevel;
    }

    public int getControlLevel() {
        return controlLevel;
    }

    public void setControlLevel(int controlLevel) {
        this.controlLevel = controlLevel;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }

    public boolean isClickable() {
        return isClickable;
    }

    public void setClickable(boolean clickable) {
        this.isClickable = clickable;
    }
}
