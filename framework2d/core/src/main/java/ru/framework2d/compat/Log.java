package ru.framework2d.compat;

import com.badlogic.gdx.Gdx;

/** Drop-in replacement for android.util.Log, delegating to libGDX logging. */
public final class Log {

    private Log() {
    }

    public static void v(String tag, String msg) {
        log(tag, msg);
    }

    public static void d(String tag, String msg) {
        log(tag, msg);
    }

    public static void i(String tag, String msg) {
        log(tag, msg);
    }

    public static void w(String tag, String msg) {
        log(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (Gdx.app != null) {
            Gdx.app.error(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (Gdx.app != null) {
            Gdx.app.error(tag, msg, tr);
        }
    }

    private static void log(String tag, String msg) {
        if (Gdx.app != null) {
            Gdx.app.log(tag, msg);
        }
    }
}
