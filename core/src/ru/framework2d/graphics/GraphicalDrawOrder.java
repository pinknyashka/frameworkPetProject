package ru.framework2d.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Stable layer ordering for the legacy {@code OutputDraw} tree.
 *
 * <p>The legacy renderer used insertion order, so a shadow created after another
 * figure could paint over that figure. The HQ renderer already sorts by layer;
 * this keeps the direct path consistent and makes shadows act as ground decals.</p>
 */
public final class GraphicalDrawOrder {

	private static final Comparator<OutputGraphicalInterface> BY_LAYER =
			new Comparator<OutputGraphicalInterface>() {
				@Override
				public int compare(OutputGraphicalInterface first, OutputGraphicalInterface second) {
					return Float.compare(first.layer.value, second.layer.value);
				}
			};

	private GraphicalDrawOrder() {
	}

	public static void sort(ArrayList<OutputGraphicalInterface> graphicals) {
		if (graphicals != null && graphicals.size() > 1) {
			Collections.sort(graphicals, BY_LAYER);
		}
	}
}
