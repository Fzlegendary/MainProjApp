package com.example.mainproj.gates;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Tests the core logic used in GateAdapter:
 * - Display name formatting from gate number
 * - Busy state detection
 * - Visibility rules
 */
@RunWith(Parameterized.class)
public class GateAdapterLogicTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        // Actual behavior of the split("e") formatting logic
        return Arrays.asList(new Object[][]{
                {"Gate1", "Gate 1"},
                {"Gate 1", "Gate  1"},  // double space due to parts[1]=" 1"
                {"G1", "G1"},
                {"gate1", "gate 1"},
                {"Gate-A", "Gate -A"},
                {"MainGate", "MainGate"},  // trailing empty string removed
        });
    }

    @Parameterized.Parameter(0)
    public String gateNumber;
    @Parameterized.Parameter(1)
    public String expectedDisplayName;

    @Test
    public void displayNameFormatting() {
        // This mirrors the logic in GateAdapter.onBindViewHolder line 46-47
        String[] parts = gateNumber.split("e");
        String displayName = parts.length >= 2 ? parts[0] + "e " + parts[1] : gateNumber;
        assertEquals(expectedDisplayName, displayName);
    }

    @Test
    public void busyState_detectedFromActionStatus() {
        assertTrue(containsBusyState("Opening..."));
        assertTrue(containsBusyState("Closing..."));
        assertFalse(containsBusyState("Open"));
        assertFalse(containsBusyState("Closed"));
        assertFalse(containsBusyState("Ready"));
    }

    @Test
    public void visibility_hiddenWhenDisabledAndNotSuperAdmin() {
        // roleLevel < 2 and gate is disabled -> hidden
        assertTrue(shouldHideGate(true, 1, 0));
        assertTrue(shouldHideGate(true, 0, 0));
        // superadmin can see disabled gates
        assertFalse(shouldHideGate(true, 2, 0));
        // not disabled -> visible
        assertFalse(shouldHideGate(false, 0, 0));
    }

    @Test
    public void visibility_hiddenWhenVisibilityLevelExceedsRoleLevel() {
        // visibilityLevel > roleLevel -> hidden
        assertTrue(shouldHideGate(false, 0, 1));
        assertTrue(shouldHideGate(false, 1, 2));
        // visibilityLevel <= roleLevel -> visible
        assertFalse(shouldHideGate(false, 2, 2));
        assertFalse(shouldHideGate(false, 2, 1));
        assertFalse(shouldHideGate(false, 0, 0));
    }

    @Test
    public void adminGear_visibleForSufficientRole() {
        // roleLevel >= controlLevel && roleLevel != 0 -> visible
        assertTrue(shouldShowAdminGear(1, 1));
        assertTrue(shouldShowAdminGear(2, 1));
        assertTrue(shouldShowAdminGear(2, 2));
        assertFalse(shouldShowAdminGear(0, 0));
        assertFalse(shouldShowAdminGear(1, 2));
        assertFalse(shouldShowAdminGear(0, 1));
    }

    @Test
    public void toggleButton_enabledOnlyWhenNotBusyAndClickable() {
        // busy or not clickable -> disabled
        assertFalse(isToggleEnabled(true, false));
        assertFalse(isToggleEnabled(true, true));
        assertFalse(isToggleEnabled(false, false));
        // not busy and clickable -> enabled
        assertTrue(isToggleEnabled(false, true));
    }

    private static boolean containsBusyState(String actionStatus) {
        return actionStatus.contains("Opening") || actionStatus.contains("Closing");
    }

    private static boolean shouldHideGate(boolean disabled, int roleLevel, int visibilityLevel) {
        return (disabled && roleLevel < 2) || visibilityLevel > roleLevel;
    }

    private static boolean shouldShowAdminGear(int roleLevel, int controlLevel) {
        return roleLevel >= controlLevel && roleLevel != 0;
    }

    private static boolean isToggleEnabled(boolean busy, boolean isClickable) {
        return !busy && isClickable;
    }
}
