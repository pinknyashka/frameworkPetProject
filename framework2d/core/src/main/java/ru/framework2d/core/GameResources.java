package ru.framework2d.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import ru.framework2d.compat.Bitmap;
import ru.framework2d.compat.Log;

public class GameResources {

    private static final String LOG_TAG = "Resources";

    public static final String APP_DIRECTORY = "sPart";
    public static final String CONFIG_NAME = "config.xml";

    public static final String IMAGE_NOT_FOUND = "image_not_found.png";

    public static final String DEAFULT_MAP_DIRECTORY = "Maps";
    public static final String DEFAULT_IMAGES_DIRECTORY = "Images";
    public static final String DEFAULT_SOUNDS_DIRECTORY = "Sounds";

    protected Config mConfiguration;

    public GameResources() {
        mConfiguration = new Config();
    }

    // ---------------------------------------------- Config ---------------------------------------
    private class Config {

        String mapsDirectory = "" + DEAFULT_MAP_DIRECTORY;
        String imagesDirectory = "" + DEFAULT_IMAGES_DIRECTORY;
        String soundsDirectory = "" + DEFAULT_SOUNDS_DIRECTORY;

        Config() {
            FileHandle config = Gdx.files.internal(CONFIG_NAME);
            if (!config.exists()) {
                Log.w(LOG_TAG, "Config not found in assets; using default directories;");
                return;
            }
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser xmlConfig = factory.newPullParser();
                xmlConfig.setInput(config.reader());
                readConfigsFromXml(xmlConfig);
            } catch (XmlPullParserException e) {
                Log.w(LOG_TAG, "Config read failure. Xml pull parser error;");
                e.printStackTrace();
            }
        }

        private void readConfigsFromXml(XmlPullParser xmlConfig) throws XmlPullParserException {
            try {
                while (xmlConfig.getEventType() != XmlPullParser.END_DOCUMENT) {
                    if (xmlConfig.getEventType() == XmlPullParser.START_TAG) {
                        String tagName = xmlConfig.getName();
                        if (tagName.contentEquals("maps")) {
                            mapsDirectory = xmlConfig.nextText();
                        } else if (tagName.contentEquals("images")) {
                            imagesDirectory = xmlConfig.nextText();
                        } else if (tagName.contentEquals("sounds")) {
                            soundsDirectory = xmlConfig.nextText();
                        }
                    }
                    xmlConfig.next();
                }
            } catch (IOException e) {
                Log.w(LOG_TAG, "Xml pull parser error while config reading: IOException;");
                e.printStackTrace();
            }
        }
    }

    // ------------------------------------------ Sprites / Images ----------------------------------
    private final ArrayList<Sprite> lstSprites = new ArrayList<Sprite>();

    public Sprite getSprite(String spriteName, int collumnsNum, int rawsNum) {

        for (Sprite sprite : lstSprites) {
            if (sprite.name.contentEquals(spriteName)) {
                return sprite;
            }
        }

        Sprite newSprite = new Sprite(getImage(spriteName), spriteName, collumnsNum, rawsNum);
        lstSprites.add(newSprite);
        return newSprite;
    }

    public Bitmap getImage(String imageName) {
        imageName = imageName.replaceFirst(".png", "") + ".png";

        Bitmap fromExternal = tryLoadExternal(mConfiguration.imagesDirectory, imageName);
        if (fromExternal != null) {
            return fromExternal;
        }

        Log.w(LOG_TAG, "Searching for image " + imageName + " in local resources;");
        return getImageFromLocalRes(imageName);
    }

    public Bitmap getImageFromLocalRes(String imageName) {
        imageName = imageName.replaceFirst(".png", "") + ".png";

        Bitmap b = tryLoadInternal(mConfiguration.imagesDirectory + "/" + imageName);
        if (b != null) {
            return b;
        }
        b = tryLoadInternal(imageName);
        if (b != null) {
            return b;
        }

        if (imageName.contentEquals(IMAGE_NOT_FOUND)) {
            Log.w(LOG_TAG, "Cant load image <" + imageName + ">; using placeholder;");
            return placeholder();
        }
        Log.w(LOG_TAG, "Cant load image <" + imageName + "> from local resources;");
        return getImageFromLocalRes(IMAGE_NOT_FOUND);
    }

    private Bitmap tryLoadExternal(String dir, String name) {
        FileHandle handle = Gdx.files.external(APP_DIRECTORY + "/" + dir + "/" + name);
        if (handle.exists()) {
            try {
                return new Bitmap(new Texture(handle));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Cant load image from external storage: " + name + "; Exception;");
            }
        }
        return null;
    }

    private Bitmap tryLoadInternal(String path) {
        FileHandle handle = Gdx.files.internal(path);
        if (handle.exists()) {
            try {
                return new Bitmap(new Texture(handle));
            } catch (Exception e) {
                Log.w(LOG_TAG, "Cant decode image: " + path + "; Exception;");
            }
        }
        return null;
    }

    private Bitmap placeholder() {
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(1, 0, 1, 1);
        px.fill();
        return new Bitmap(new Texture(px));
    }

    // ------------------------------------------------- Maps ---------------------------------------
    public String[] getLocalMapNames() {
        return names(Gdx.files.internal("xml").list());
    }

    public String[] getSdMapNames() {
        return names(Gdx.files.external(APP_DIRECTORY + "/" + mConfiguration.mapsDirectory).list());
    }

    private static String[] names(FileHandle[] handles) {
        String[] result = new String[handles.length];
        for (int i = 0; i < handles.length; i++) {
            result[i] = handles[i].name();
        }
        return result;
    }

    public XmlPullParser getSdMap(String mapName) {
        mapName = mapName.replaceFirst(".xml", "") + ".xml";
        FileHandle handle = Gdx.files.external(APP_DIRECTORY + "/" + mConfiguration.mapsDirectory + "/" + mapName);
        if (handle.exists()) {
            return newParser(handle.reader());
        }
        Log.w(LOG_TAG, "Map not found in external storage; searching local;");
        return getLocalMap(mapName);
    }

    public XmlPullParser getLocalMap(String mapName) {
        mapName = mapName.replaceFirst(".xml", "") + ".xml";
        FileHandle handle = Gdx.files.internal("xml/" + mapName);
        if (!handle.exists()) {
            Log.e(LOG_TAG, "Local map <" + mapName + "> not found;");
            return null;
        }
        return newParser(handle.reader());
    }

    private XmlPullParser newParser(java.io.Reader reader) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new BufferedReader(reader));
            return parser;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean writeMapToSd(String localMapName) {
        FileHandle target = Gdx.files.external(APP_DIRECTORY + "/" + mConfiguration.mapsDirectory + "/" + localMapName);
        FileHandle source = Gdx.files.internal("xml/" + localMapName);
        if (!target.exists() && source.exists()) {
            try {
                target.writeBytes(source.readBytes(), false);
                return true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Map copy error;");
            }
        }
        return false;
    }

    public boolean removeMapFromSd(String mapName) {
        FileHandle target = Gdx.files.external(APP_DIRECTORY + "/" + mConfiguration.mapsDirectory + "/" + mapName);
        if (target.exists()) {
            target.delete();
            return true;
        }
        return false;
    }
}
