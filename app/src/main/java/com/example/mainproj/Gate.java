package com.example.mainproj;

public class Gate {

    private String gateNumber;
    private String state;
    private String actionStatus;
    private long lastAckAt;
    private String lastCommandId;
    private boolean isClickable = true;

    public Gate(String gateNumber, String state, String actionStatus, long lastAckAt, String lastCommandId) {
        this.gateNumber = gateNumber;
        this.state = state;
        this.actionStatus = actionStatus;
        this.lastAckAt = lastAckAt;
        this.lastCommandId = lastCommandId;
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
