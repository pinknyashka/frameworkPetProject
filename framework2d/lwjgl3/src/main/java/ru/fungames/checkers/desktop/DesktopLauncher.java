package ru.fungames.checkers.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import ru.fungames.checkers.CheckersGame;

public final class DesktopLauncher {

    public static void main(String[] args) {
        CheckersGame game = new CheckersGame();
        for (String arg : args) {
            if ("--smoke".equals(arg)) {
                game.exitAfterFrames = 180;
            } else if (arg.startsWith("--frames=")) {
                game.exitAfterFrames = Integer.parseInt(arg.substring("--frames=".length()));
            } else if (arg.startsWith("--shot=")) {
                game.screenshotPath = arg.substring("--shot=".length());
            } else if (arg.startsWith("--map=")) {
                game.mapName = arg.substring("--map=".length());
            } else if ("--drag-smoke".equals(arg)) {
                game.dragSmoke = true;
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("framework2d");
        // Portrait, like the original Android app: the maps size the board from DisplayWidth,
        // so a portrait window shows the whole board with margins above and below it.
        config.setWindowedMode(600, 900);
        config.setForegroundFPS(60);
        config.useVsync(true);
        new Lwjgl3Application(game, config);
    }

    private DesktopLauncher() {
    }
}
