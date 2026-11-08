package ru.fungames.checkers.launcher;

import java.io.BufferedReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import ru.framework2d.compat.Log;

/**
 * Launcher-side metadata of one map, read from the root tag of assets/xml/&lt;name&gt;.xml.
 * The prototype only needs a display name, a preview image and the game/test flag;
 * the description/preview pipeline will be expanded later.
 */
public class MapInfo {

    private static final String LOG_TAG = "Launcher";

    public final String fileName;
    public String name;
    public String preview;
    public boolean test;
    public boolean parallel;

    public MapInfo(String fileName) {
        this.fileName = fileName;
        this.name = displayNameFromFileName(fileName);
        readRootTag();
    }

    /** true when the map is a test scenario: explicit test="true" or a *_test name convention. */
    public static boolean looksLikeTest(String fileName, String testAttribute, String name) {
        if ("true".equalsIgnoreCase(testAttribute)) {
            return true;
        }
        String base = fileName.toLowerCase(java.util.Locale.US);
        if (base.contains("test") || base.startsWith("binding_") || base.startsWith("sound_")
                || base.startsWith("graphics_")) {
            return true;
        }
        return name != null && name.toLowerCase(java.util.Locale.US).contains("test");
    }

    public static String displayNameFromFileName(String fileName) {
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base.replace('_', ' ');
    }

    private void readRootTag() {
        FileHandle handle = Gdx.files.internal("xml/" + fileName);
        if (!handle.exists()) {
            Log.w(LOG_TAG, "Map <" + fileName + "> not found while reading metadata;");
            return;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new BufferedReader(handle.reader()));
            // The first start tag is the map root; its attributes carry the launcher metadata.
            int event = parser.next();
            while (event != XmlPullParser.END_DOCUMENT && event != XmlPullParser.START_TAG) {
                event = parser.next();
            }
            String testAttribute = null;
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                String attribute = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                if ("name".equals(attribute)) {
                    name = value;
                } else if ("preview".equals(attribute)) {
                    preview = value;
                } else if ("parallel".equals(attribute)) {
                    parallel = "true".equalsIgnoreCase(value);
                } else if ("test".equals(attribute)) {
                    testAttribute = value;
                }
            }
            test = looksLikeTest(fileName, testAttribute, name);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Cant read metadata of map <" + fileName + ">; Exception;");
        }
    }
}
