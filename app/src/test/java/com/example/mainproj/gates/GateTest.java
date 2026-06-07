package com.example.mainproj.gates;

import org.junit.Test;

import static org.junit.Assert.*;

public class GateTest {

    @Test
    public void constructor_5args_setsFields() {
        Gate gate = new Gate("Gate1", "open", "Opening...", 1000L, "cmd-123");
        assertEquals("Gate1", gate.getGateNumber());
        assertEquals("open", gate.getState());
        assertEquals("Opening...", gate.getActionStatus());
        assertEquals(1000L, gate.getLastAckAt());
        assertEquals("cmd-123", gate.getLastCommandId());
        assertTrue(gate.isClickable());
        assertEquals(0, gate.getVisibilityLevel());
        assertEquals(0, gate.getControlLevel());
        assertFalse(gate.isDisabled());
    }

    @Test
    public void constructor_8args_setsFields() {
        Gate gate = new Gate("Gate2", "closed", "Ready", 0L, "", 1, 1);
        assertEquals("Gate2", gate.getGateNumber());
        assertEquals("closed", gate.getState());
        assertEquals(1, gate.getVisibilityLevel());
        assertEquals(1, gate.getControlLevel());
    }

    @Test
    public void isOpen_returnsTrueForOpenIgnoreCase() {
        assertTrue(new Gate("G1", "open", "", 0, "").isOpen());
        assertTrue(new Gate("G1", "Open", "", 0, "").isOpen());
        assertTrue(new Gate("G1", "OPEN", "", 0, "").isOpen());
    }

    @Test
    public void isOpen_returnsFalseForClosed() {
        assertFalse(new Gate("G1", "closed", "", 0, "").isOpen());
        assertFalse(new Gate("G1", "closing", "", 0, "").isOpen());
    }

    @Test
    public void isOpen_returnsFalseForNullState() {
        Gate gate = new Gate("G1", null, "", 0, "");
        assertFalse(gate.isOpen());
    }

    @Test
    public void setState_updatesState() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        gate.setState("open");
        assertEquals("open", gate.getState());
    }

    @Test
    public void setActionStatus_updatesActionStatus() {
        Gate gate = new Gate("G1", "closed", "Ready", 0, "");
        gate.setActionStatus("Opening...");
        assertEquals("Opening...", gate.getActionStatus());
    }

    @Test
    public void clickable_defaultTrue() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        assertTrue(gate.isClickable());
    }

    @Test
    public void setClickable_false_disablesClick() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        gate.setClickable(false);
        assertFalse(gate.isClickable());
    }

    @Test
    public void disabled_defaultFalse() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        assertFalse(gate.isDisabled());
    }

    @Test
    public void setDisabled_changesDisabled() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        gate.setDisabled(true);
        assertTrue(gate.isDisabled());
        gate.setDisabled(false);
        assertFalse(gate.isDisabled());
    }

    @Test
    public void setLevels_updatesCorrectly() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        gate.setVisibilityLevel(2);
        gate.setControlLevel(0);
        assertEquals(2, gate.getVisibilityLevel());
        assertEquals(0, gate.getControlLevel());
    }

    @Test
    public void setLastAckAt_updatesTimestamp() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        gate.setLastAckAt(5000L);
        assertEquals(5000L, gate.getLastAckAt());
    }

    @Test
    public void setLastCommandId_updatesCommandId() {
        Gate gate = new Gate("G1", "closed", "", 0, "");
        gate.setLastCommandId("new-cmd-456");
        assertEquals("new-cmd-456", gate.getLastCommandId());
    }
}
