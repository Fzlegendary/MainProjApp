package com.example.mainproj;

public class Gate {

    private String gateNumber;
    private String state;
    private String actionStatus;

    public Gate(String gateNumber, String state, String actionStatus) {
        this.gateNumber = gateNumber;
        this.state = state;
        this.actionStatus = actionStatus;
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

    public void setState(String state) {
        this.state = state;
    }

    public void setActionStatus(String actionStatus) {
        this.actionStatus = actionStatus;
    }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }
}
