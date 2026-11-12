import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Turns art/Logo-checkers.png into the launcher logo texture assets/Images/checkers_logo.png.
 *
 * The artwork ships already chroma keyed: its background is transparent and the violet RGB left
 * in those pixels is a placeholder colour, so this tool works from the alpha channel instead of
 * keying a colour. It drops dust specks, closes stray holes, trims the empty margins, keeps the
 * crop square and centred, and resamples to a power-of-two side. Resampling averages colour
 * weighted by alpha, so the placeholder RGB of transparent pixels never bleeds into the pieces.
 *
 *   java art/PrepareLogo.java inspect                report the mask, write nothing
 *   java art/PrepareLogo.java build                  write assets/Images/checkers_logo.png
 *   java art/PrepareLogo.java preview WxH out.png    composite over the menu background
 *
 * Run all commands from the repository root.
 */
public final class PrepareLogo {

    private static final String SOURCE = "art/Logo-checkers.png";
    private static final String TARGET = "assets/Images/checkers_logo.png";
    private static final String BACKGROUND = "art/backgrounds/main.png";

    /** Emitted square and power-of-two so no backend has to rescale or pad the texture. */
    private static final int OUT_SIDE = 1024;

    /** Below this the artwork is keyed out; the violet RGB there is only a placeholder. */
    private static final int MIN_KEPT_ALPHA = 8;

    /** Debris chips are hundreds of pixels wide; below this area a blob is dust. */
    private static final int MIN_COMPONENT_AREA = 120;

    /** Edge rows and columns holding less coverage than this are trimmed off the logo box. */
    private static final double EDGE_COVERAGE = 0.003;

    /** Violet cast pulled out of the soft rim where the keyed background still blends in. */
    private static final double RIM_DESPILL = 0.8;

    // Mask values: kept pixel, keyed pixel, kept pixel already covered by a scanned blob.
    private static final byte KEPT = 0;
    private static final byte KEYED = 1;
    private static final byte SCANNED = 2;

    // Mirrors LauncherScreen.drawMain/drawLogo so the preview needs no GL window; keep in sync.
    private static final float LOGO_TOP = 0.01f;
    private static final float LOGO_MAX_WIDTH_RATIO = 0.558f;
    private static final float LOGO_MAX_HEIGHT_RATIO = 0.288f;
    private static final float BUTTON_WIDTH_RATIO = 0.38f;
    private static final float BUTTON_STACK_TOP = 0.34f;
    private static final float BUTTON_STACK_BOTTOM = 0.90f;
    private static final int BUTTON_COUNT = 4;
    private static final float BUTTON_ASPECT = 1650f / 630f;
    private static final float MIN_BUTTON_HEIGHT = 30f;

    private PrepareLogo() {
    }

    public static void main(String[] args) throws IOException {
        String mode = args.length > 0 ? args[0] : "inspect";

        BufferedImage source = read(new File(SOURCE));
        int width = source.getWidth();
        int height = source.getHeight();
        int[] argb = pixels(source);

        byte[] mask = alphaMask(argb, MIN_KEPT_ALPHA);
        if (countKeyed(mask) * 20 < argb.length) {
            System.out.println("WARNING: " + SOURCE + " is almost opaque: it is not chroma keyed, "
                    + "the logo would come out as a full square");
        }

        int dropped = dropDust(mask, width, MIN_COMPONENT_AREA);
        int[] rawBounds = keptBounds(mask, width, height);
        int[] bounds = trimToCoverage(mask, width, rawBounds, EDGE_COVERAGE);
        int side = squareSide(bounds, width, height);
        int originX = squareOrigin(bounds[0], bounds[2], side, width);
        int originY = squareOrigin(bounds[1], bounds[3], side, height);

        System.out.println(width + "x" + height + " source, keyed "
                + percent(countKeyed(mask), argb.length) + "%, dropped " + dropped + " dust blobs"
                + ", bounds " + describe(rawBounds) + " -> " + describe(bounds) + ", square "
                + side + "px at " + originX + "," + originY);

        if ("build".equals(mode)) {
            build(argb, mask, width, side, originX, originY);
        } else if ("preview".equals(mode)) {
            preview(argb, mask, width, side, originX, originY, args);
        }
    }

    private static void build(int[] argb, byte[] mask, int width, int side, int originX,
            int originY) throws IOException {
        BufferedImage logo = resample(cut(argb, mask, width, side, originX, originY), OUT_SIDE);
        File target = new File(TARGET);
        target.getParentFile().mkdirs();
        if (!ImageIO.write(logo, "png", target)) {
            throw new IOException("no PNG writer available");
        }
        System.out.println("wrote " + TARGET + " " + logo.getWidth() + "x" + logo.getHeight());
    }

    // ------------------------------------------------------------------ mask

    /** @return {@link #KEYED} where the artwork is transparent, {@link #KEPT} on the artwork. */
    private static byte[] alphaMask(int[] argb, int minAlpha) {
        byte[] mask = new byte[argb.length];
        for (int i = 0; i < argb.length; i++) {
            if (((argb[i] >>> 24) & 0xff) < minAlpha) {
                mask[i] = KEYED;
            }
        }
        return mask;
    }

    /**
     * Gives every kept blob smaller than {@code minArea} back to the keyed background: those are
     * dust specks, not logo content. A scanned blob keeps its mark until the whole pass ends, so
     * the outer scan never re-walks a blob it already covered; without that mark the pass is
     * quadratic in blob size and never finishes on a full size logo.
     *
     * @return number of dust blobs removed
     */
    private static int dropDust(byte[] mask, int width, int minArea) {
        int[] queue = new int[mask.length];
        int[] blob = new int[mask.length];
        int dust = 0;
        int kept = 0;

        for (int start = 0; start < mask.length; start++) {
            if (mask[start] != KEPT) {
                continue;
            }
            int head = 0;
            int tail = 0;
            int size = 0;
            mask[start] = SCANNED;
            queue[tail++] = start;

            while (head < tail) {
                int p = queue[head++];
                blob[size++] = p;
                int x = p % width;
                if (x > 0) {
                    tail = collect(mask, queue, tail, p - 1);
                }
                if (x + 1 < width) {
                    tail = collect(mask, queue, tail, p + 1);
                }
                if (p - width >= 0) {
                    tail = collect(mask, queue, tail, p - width);
                }
                if (p + width < mask.length) {
                    tail = collect(mask, queue, tail, p + width);
                }
            }

            if (size < minArea) {
                for (int i = 0; i < size; i++) {
                    mask[blob[i]] = KEYED;
                }
                dust++;
            } else {
                kept++;
            }
        }

        for (int i = 0; i < mask.length; i++) {
            if (mask[i] == SCANNED) {
                mask[i] = KEPT;
            }
        }
        System.out.println("kept " + kept + " artwork blobs, dropped " + dust + " dust blobs");
        return dust;
    }

    private static int collect(byte[] mask, int[] queue, int tail, int p) {
        if (mask[p] == KEPT) {
            mask[p] = SCANNED;
            queue[tail++] = p;
        }
        return tail;
    }

    // -------------------------------------------------------------- geometry

    /** @return {minX, minY, maxX, maxY} of kept pixels, inclusive; empty when all is keyed out */
    private static int[] keptBounds(byte[] mask, int width, int height) {
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[y * width + x] == KEPT) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return new int[] {minX, minY, maxX, maxY};
    }

    /** Drops border rows and columns that only hold dust and generation artifacts. */
    private static int[] trimToCoverage(byte[] mask, int width, int[] box, double minCoverage) {
        int height = mask.length / width;
        int minX = box[0];
        int minY = box[1];
        int maxX = box[2];
        int maxY = box[3];

        while (minY < maxY && rowCoverage(mask, width, minY, minX, maxX)
                < minCoverage * (maxX - minX + 1)) {
            minY++;
        }
        while (maxY > minY && rowCoverage(mask, width, maxY, minX, maxX)
                < minCoverage * (maxX - minX + 1)) {
            maxY--;
        }
        while (minX < maxX && columnCoverage(mask, width, minX, minY, maxY)
                < minCoverage * (maxY - minY + 1)) {
            minX++;
        }
        while (maxX > minX && columnCoverage(mask, width, maxX, minY, maxY)
                < minCoverage * (maxY - minY + 1)) {
            maxX--;
        }
        return new int[] {minX, minY, maxX, maxY};
    }

    private static int rowCoverage(byte[] mask, int width, int y, int fromX, int toX) {
        int count = 0;
        for (int x = fromX; x <= toX; x++) {
            if (mask[y * width + x] == KEPT) {
                count++;
            }
        }
        return count;
    }

    private static int columnCoverage(byte[] mask, int width, int x, int fromY, int toY) {
        int count = 0;
        for (int y = fromY; y <= toY; y++) {
            if (mask[y * width + x] == KEPT) {
                count++;
            }
        }
        return count;
    }

    /** The logo stays square: the shorter content side is padded, so nothing gets squashed. */
    private static int squareSide(int[] box, int width, int height) {
        int content = Math.max(box[2] - box[0] + 1, box[3] - box[1] + 1) + 2;
        return Math.min(content, Math.min(width, height));
    }

    /** Centres the content in the square box without cutting it or leaving the source. */
    private static int squareOrigin(int boxFrom, int boxTo, int side, int extent) {
        int lowest = Math.max(0, boxTo + 1 - side);
        int highest = Math.min(boxFrom, extent - side);
        int centered = Math.round((boxFrom + boxTo + 1 - side) / 2f);
        return Math.max(lowest, Math.min(highest, centered));
    }

    // --------------------------------------------------------------- cutting

    private static BufferedImage cut(int[] argb, byte[] mask, int width, int side, int originX,
            int originY) {
        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int p = (y + originY) * width + (x + originX);
                int alpha = mask[p] == KEPT ? (argb[p] >>> 24) & 0xff : 0;
                int rgb = despill(argb[p], alpha);
                out.setRGB(x, y, rgb | (alpha << 24));
            }
        }
        return out;
    }

    /** Neutralises the violet that still bleeds into the rim the key left half transparent. */
    private static int despill(int rgb, int alpha) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        int violet = Math.min(r - g, b - g);
        if (alpha >= 250 || violet <= 0) {
            return (r << 16) | (g << 8) | b;
        }
        int pull = (int) Math.round(violet * RIM_DESPILL);
        return (Math.max(g, r - pull) << 16) | (g << 8) | Math.max(g, b - pull);
    }

    // -------------------------------------------------------------- resizing

    /** Halves while at least 2x too big, then lands on the exact side; both keep the alpha true. */
    private static BufferedImage resample(BufferedImage source, int targetSide) {
        BufferedImage current = source;
        while (current.getWidth() / 2 >= targetSide) {
            current = average(current, current.getWidth() / 2);
        }
        if (current.getWidth() != targetSide) {
            current = average(current, targetSide);
        }
        return current;
    }

    /**
     * Box average of the source region under each output pixel. Colour is summed weighted by
     * alpha and divided by the alpha total, i.e. premultiplied: the placeholder colour of keyed
     * pixels is weight 0 and never smears into the edge of the pieces.
     */
    private static BufferedImage average(BufferedImage source, int side) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] src = pixels(source);
        int[] dst = new int[side * side];

        for (int y = 0; y < side; y++) {
            int fromY = y * height / side;
            int toY = Math.max(fromY + 1, (y + 1) * height / side);
            for (int x = 0; x < side; x++) {
                int fromX = x * width / side;
                int toX = Math.max(fromX + 1, (x + 1) * width / side);
                int sumAlpha = 0;
                int sumR = 0;
                int sumG = 0;
                int sumB = 0;
                int taps = 0;
                for (int sy = fromY; sy < toY; sy++) {
                    int row = sy * width;
                    for (int sx = fromX; sx < toX; sx++) {
                        int pixel = src[row + sx];
                        int alpha = (pixel >>> 24) & 0xff;
                        sumAlpha += alpha;
                        sumR += ((pixel >> 16) & 0xff) * alpha;
                        sumG += ((pixel >> 8) & 0xff) * alpha;
                        sumB += (pixel & 0xff) * alpha;
                        taps++;
                    }
                }
                if (sumAlpha == 0) {
                    continue;
                }
                int outAlpha = Math.min(255, (sumAlpha + taps / 2) / taps);
                dst[y * side + x] = (outAlpha << 24)
                        | (roundChannel(sumR, sumAlpha) << 16)
                        | (roundChannel(sumG, sumAlpha) << 8)
                        | roundChannel(sumB, sumAlpha);
            }
        }

        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, side, side, dst, 0, side);
        return out;
    }

    private static int roundChannel(int weighted, int sumAlpha) {
        return Math.min(255, (weighted + sumAlpha / 2) / sumAlpha);
    }

    // --------------------------------------------------------------- preview

    /**
     * Composites the finished logo over the menu background at the geometry {@code LauncherScreen}
     * uses, so the layout can be checked without opening a GL window.
     */
    private static void preview(int[] argb, byte[] mask, int width, int side, int originX,
            int originY, String[] args) throws IOException {
        int viewWidth = 1280;
        int viewHeight = 720;
        if (args.length > 1 && args[1].matches("\\d+x\\d+")) {
            String[] parts = args[1].split("x");
            viewWidth = Integer.parseInt(parts[0]);
            viewHeight = Integer.parseInt(parts[1]);
        }
        File target = new File(args.length > 2 ? args[2] : "logo-preview.png");

        BufferedImage view = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = view.createGraphics();
        g.setColor(new Color(34, 39, 48));
        g.fillRect(0, 0, viewWidth, viewHeight);
        File background = new File(BACKGROUND);
        if (background.exists()) {
            drawCover(g, ImageIO.read(background), viewWidth, viewHeight);
        }

        BufferedImage logo = resample(cut(argb, mask, width, side, originX, originY), OUT_SIDE);
        float cover = Math.min(
                Math.min(viewWidth * LOGO_MAX_WIDTH_RATIO, OUT_SIDE) / logo.getWidth(),
                Math.min(viewHeight * LOGO_MAX_HEIGHT_RATIO, OUT_SIDE) / logo.getHeight());
        cover = Math.min(cover, 1f);
        int logoWidth = Math.round(logo.getWidth() * cover);
        int logoHeight = Math.round(logo.getHeight() * cover);
        int logoTop = Math.round(viewHeight * LOGO_TOP);
        g.drawImage(logo, Math.round((viewWidth - logoWidth) / 2f), logoTop, logoWidth, logoHeight,
                null);
        drawButtonStack(g, viewWidth, viewHeight, logoTop + logoHeight);
        g.setColor(Color.WHITE);
        g.drawString("logo " + logoWidth + "x" + logoHeight, 12f, viewHeight - 12f);
        g.dispose();

        if (!ImageIO.write(view, "png", target)) {
            throw new IOException("no PNG writer available");
        }
        System.out.println("wrote " + target.getPath() + " " + viewWidth + "x" + viewHeight
                + ", logo " + logoWidth + "x" + logoHeight);
    }

    /** Mirrors the main-screen button stack of LauncherScreen: fits under the logo, no overlap. */
    private static void drawButtonStack(Graphics2D g, int viewWidth, int viewHeight,
            int logoBottom) {
        float margin = Math.min(viewWidth, viewHeight) * 0.08f;
        float desiredWidth = Math.min(viewWidth - 2f * margin, viewWidth * BUTTON_WIDTH_RATIO);
        float desiredHeight = desiredWidth / BUTTON_ASPECT;
        float gap = Math.max(8f, desiredHeight * 0.12f);
        float stackBottom = viewHeight * BUTTON_STACK_BOTTOM;
        float stackTop = Math.max(logoBottom + gap, viewHeight * BUTTON_STACK_TOP);
        float buttonHeight = Math.max(MIN_BUTTON_HEIGHT, Math.min(desiredHeight,
                (stackBottom - stackTop - (BUTTON_COUNT - 1) * gap) / BUTTON_COUNT));
        float buttonWidth = Math.min(desiredWidth, buttonHeight * BUTTON_ASPECT);
        gap = Math.max(8f, buttonHeight * 0.12f);
        float top = Math.min(stackTop,
                stackBottom - BUTTON_COUNT * buttonHeight - (BUTTON_COUNT - 1) * gap);
        float left = (viewWidth - buttonWidth) / 2f;

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD,
                Math.max(10, Math.round(buttonHeight * 0.34f))));
        FontMetrics metrics = g.getFontMetrics();
        String[] labels = {"New Game", "Continue", "Tests", "About"};
        for (int i = 0; i < BUTTON_COUNT; i++) {
            float y = top + i * (buttonHeight + gap);
            g.setColor(new Color(198, 150, 88));
            g.fillRect(Math.round(left), Math.round(y), Math.round(buttonWidth),
                    Math.round(buttonHeight));
            g.setColor(new Color(55, 30, 16));
            String label = labels[i];
            g.drawString(label, Math.round(left + (buttonWidth - metrics.stringWidth(label)) / 2f),
                    Math.round(y + buttonHeight / 2f + metrics.getAscent() / 2f - 2f));
        }
        System.out.println("logo bottom " + logoBottom + ", stack top " + Math.round(top)
                + ", button " + Math.round(buttonWidth) + "x" + Math.round(buttonHeight)
                + ", stack bottom " + Math.round(top + BUTTON_COUNT * buttonHeight
                + (BUTTON_COUNT - 1) * gap) + " of " + Math.round(stackBottom));
    }

    private static void drawCover(Graphics2D g, BufferedImage background, int viewWidth,
            int viewHeight) {
        float scale = Math.max(viewWidth / (float) background.getWidth(),
                viewHeight / (float) background.getHeight());
        int scaledWidth = Math.round(background.getWidth() * scale);
        int scaledHeight = Math.round(background.getHeight() * scale);
        g.drawImage(background, (viewWidth - scaledWidth) / 2, (viewHeight - scaledHeight) / 2,
                scaledWidth, scaledHeight, null);
    }

    // ---------------------------------------------------------------- helpers

    private static BufferedImage read(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("missing source art: " + file.getPath());
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("cannot decode " + file.getPath());
        }
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static int countKeyed(byte[] mask) {
        int total = 0;
        for (byte b : mask) {
            if (b == KEYED) {
                total++;
            }
        }
        return total;
    }

    private static String describe(int[] box) {
        return "[" + box[0] + "," + box[1] + " .. " + box[2] + "," + box[3] + "]";
    }

    private static int percent(int part, int whole) {
        return Math.round(100f * part / whole);
    }
}
