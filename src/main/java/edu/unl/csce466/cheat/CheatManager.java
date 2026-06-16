package edu.unl.csce466.cheat;  // <- Этот пакет остаётся без изменений

public class CheatManager {
    // Основные toggles (вкл/выкл)
    public static boolean flyEnabled = false;
    public static boolean killAuraEnabled = false;
    public static boolean espEnabled = false;
    public static boolean noClipEnabled = false;     // для будущего
    public static boolean fullbrightEnabled = false;

    // Настройки слайдеров (float)
    public static float speedMultiplier = 2.0f;
    public static float reachDistance = 4.5f;
    public static float flySpeed = 0.1f;             // скорость полёта

    // Для ImGui sliders (ImFloat требует ref, поэтому используем массивы как хак)
    // Или можно в ImGuiScreen использовать ImFloat каждый раз - но static удобно хранить здесь
    public static float[] speedMultiplierRef = {2.0f};
    public static float[] reachDistanceRef = {4.5f};
    public static float[] flySpeedRef = {0.1f};

    // Метод для отключения всех читов (panic button)
    public static void disableAll() {
        flyEnabled = false;
        killAuraEnabled = false;
        espEnabled = false;
        noClipEnabled = false;
        fullbrightEnabled = false;
        speedMultiplier = 2.0f;
        reachDistance = 4.5f;
        flySpeed = 0.1f;

        // Синхронизируем ref'ы
        speedMultiplierRef[0] = speedMultiplier;
        reachDistanceRef[0] = reachDistance;
        flySpeedRef[0] = flySpeed;
    }

    // Можно добавить save/load в файл в будущем (через Gson или Forge config)
}
