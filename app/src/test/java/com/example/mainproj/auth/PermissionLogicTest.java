package com.example.mainproj.auth;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the permission logic independently of Firebase dependencies.
 * Mirrors the pure functions in SessionManager.
 */
public class PermissionLogicTest {

    private static boolean canViewGate(int roleLevel, int gateLevel) {
        return roleLevel >= gateLevel;
    }

    private static boolean canControlGate(int roleLevel, int gateLevel) {
        return roleLevel >= gateLevel;
    }

    private static boolean isAdmin(int roleLevel) {
        return roleLevel >= 1;
    }

    private static boolean isSuperAdmin(int roleLevel) {
        return roleLevel >= 2;
    }

    private static boolean canModifyUser(int roleLevel, int targetRoleLevel) {
        return roleLevel > targetRoleLevel;
    }

    @Test
    public void canViewGate_role0_gate0() {
        assertTrue(canViewGate(0, 0));
    }

    @Test
    public void canViewGate_role0_gate1() {
        assertFalse(canViewGate(0, 1));
    }

    @Test
    public void canViewGate_role1_gate1() {
        assertTrue(canViewGate(1, 1));
    }

    @Test
    public void canViewGate_role2_gate2() {
        assertTrue(canViewGate(2, 2));
    }

    @Test
    public void canViewGate_role1_gate2() {
        assertFalse(canViewGate(1, 2));
    }

    @Test
    public void canControlGate_role0_gate1() {
        assertFalse(canControlGate(0, 1));
    }

    @Test
    public void canControlGate_role1_gate1() {
        assertTrue(canControlGate(1, 1));
    }

    @Test
    public void canControlGate_role2_gate0() {
        assertTrue(canControlGate(2, 0));
    }

    @Test
    public void isAdmin_role0() {
        assertFalse(isAdmin(0));
    }

    @Test
    public void isAdmin_role1() {
        assertTrue(isAdmin(1));
    }

    @Test
    public void isAdmin_role2() {
        assertTrue(isAdmin(2));
    }

    @Test
    public void isSuperAdmin_role0() {
        assertFalse(isSuperAdmin(0));
    }

    @Test
    public void isSuperAdmin_role1() {
        assertFalse(isSuperAdmin(1));
    }

    @Test
    public void isSuperAdmin_role2() {
        assertTrue(isSuperAdmin(2));
    }

    @Test
    public void canModifyUser_lowerLevel() {
        assertTrue(canModifyUser(2, 1));
    }

    @Test
    public void canModifyUser_equalLevel() {
        assertFalse(canModifyUser(1, 1));
    }

    @Test
    public void canModifyUser_higherLevel() {
        assertFalse(canModifyUser(1, 2));
    }
}
