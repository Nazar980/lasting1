package edu.unl.csce466.cheat;

public class CheatManager {
    public static boolean flyEnabled = false;
    public static boolean killAuraEnabled = false;
    public static boolean espEnabled = false;
    public static boolean noClipEnabled = false;   
    public static boolean fullbrightEnabled = false;

    public static float speedMultiplier = 2.0f;
    public static float reachDistance = 4.5f;
    public static float flySpeed = 0.1f;     

    public static float[] speedMultiplierRef = {2.0f};
    public static float[] reachDistanceRef = {4.5f};
    public static float[] flySpeedRef = {0.1f};

    public static void disableAll() {
        flyEnabled = false;
        killAuraEnabled = false;
        espEnabled = false;
        noClipEnabled = false;
        fullbrightEnabled = false;
        speedMultiplier = 2.0f;
        reachDistance = 4.5f;
        flySpeed = 0.1f;

        speedMultiplierRef[0] = speedMultiplier;
        reachDistanceRef[0] = reachDistance;
        flySpeedRef[0] = flySpeed;
    }

}
