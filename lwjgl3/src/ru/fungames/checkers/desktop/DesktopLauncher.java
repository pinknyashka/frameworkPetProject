package ru.fungames.checkers.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import ru.fungames.checkers.CheckersGame;

public final class DesktopLauncher {

    public static void main(String[] args) {
        CheckersGame game = new CheckersGame();
        // Portrait, like the original Android app: the maps size the board from DisplayWidth,
        // so a portrait window shows the whole board with margins above and below it.
        int windowWidth = 600;
        int windowHeight = 900;
        for (String arg : args) {
            if ("--smoke".equals(arg)) {
                game.exitAfterFrames = 180;
                game.showLauncher = false;
            } else if (arg.startsWith("--frames=")) {
                game.exitAfterFrames = Integer.parseInt(arg.substring("--frames=".length()));
            } else if (arg.startsWith("--shot=")) {
                game.screenshotPath = arg.substring("--shot=".length());
            } else if (arg.startsWith("--map=")) {
                game.mapName = arg.substring("--map=".length());
                game.showLauncher = false;
            } else if ("--launcher".equals(arg)) {
                game.showLauncher = true;
            } else if (arg.startsWith("--launcher-select=")) {
                String value = arg.substring("--launcher-select=".length());
                int colon = value.indexOf(':');
                int frames = Integer.parseInt(colon < 0 ? value : value.substring(0, colon));
                game.showLauncher = true;
                game.launcherAutoSelectAfterFrames = frames;
                if (colon >= 0) {
                    String rest = value.substring(colon + 1);
                    if ("about".equals(rest)) {
                        game.launcherAutoOpenAbout = true;
                    } else if ("tests".equals(rest)) {
                        game.launcherAutoSelectTests = true;
                    } else if (rest.startsWith("tests:")) {
                        game.launcherAutoSelectTests = true;
                        game.launcherAutoSelectMap = rest.substring("tests:".length());
                    } else {
                        game.launcherAutoSelectMap = rest;
                    }
                }
            } else if (arg.startsWith("--launcher-scroll=")) {
                game.showLauncher = true;
                game.launcherAutoScrollPixels =
                        Integer.parseInt(arg.substring("--launcher-scroll=".length()));
            } else if (arg.startsWith("--window=")) {
                String size = arg.substring("--window=".length());
                int x = size.indexOf('x');
                if (x > 0) {
                    windowWidth = Integer.parseInt(size.substring(0, x));
                    windowHeight = Integer.parseInt(size.substring(x + 1));
                }
            } else if ("--drag-smoke".equals(arg)) {
                game.dragSmoke = true;
            } else if (arg.startsWith("--back-after=")) {
                game.backSmokeAfterFrames = Integer.parseInt(arg.substring("--back-after=".length()));
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("framework2d");
        config.setWindowedMode(windowWidth, windowHeight);
        config.setForegroundFPS(60);
        config.useVsync(true);
        new Lwjgl3Application(game, config);
    }

    private DesktopLauncher() {
    }
}
