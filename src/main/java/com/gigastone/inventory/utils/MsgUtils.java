package com.gigastone.inventory.utils;

public class MsgUtils {
    public static void printMsg() {
        System.out.println();
    }

    public static void printMsg(String msg) {
        System.out.println(msg == null ? "" : msg);
    }

    public static void printWaitingMsg(String msg) {
        System.out.print(msg == null ? "" : msg);
    }

    public static void printErrorMsg() {
        System.err.println();
    }

    public static void printErrorMsg(String msg) {
        System.err.println(msg == null ? "" : msg);
    }

    public static void printErrorMsg(Exception ex) {
        if (ex == null) return;
        System.err.println(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        ex.printStackTrace(System.err);
    }
}
