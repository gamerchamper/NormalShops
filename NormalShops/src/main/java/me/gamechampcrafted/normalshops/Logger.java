package me.gamechampcrafted.normalshops;

public class Logger {

    public static void info(String message) {
        NormalShops.getInstance().getLogger().info(message);
    }

    public static void severe(String message) {
        NormalShops.getInstance().getLogger().severe(message);
    }

    public static void warning(String message) {
        NormalShops.getInstance().getLogger().warning(message);
    }
}
