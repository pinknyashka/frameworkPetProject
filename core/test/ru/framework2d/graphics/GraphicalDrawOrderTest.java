package ru.framework2d.graphics;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;

import org.junit.Test;

public class GraphicalDrawOrderTest {

	@Test
	public void lowerLayersDrawFirstAndEqualLayersKeepInsertionOrder() {
		OutputGraphicalInterface figure = graphical(2.0f);
		OutputGraphicalInterface shadow = graphical(0.0f);
		OutputGraphicalInterface background = graphical(-3.0f);
		OutputGraphicalInterface relief = graphical(2.0f);

		ArrayList<OutputGraphicalInterface> graphicals = new ArrayList<OutputGraphicalInterface>();
		graphicals.add(figure);
		graphicals.add(shadow);
		graphicals.add(background);
		graphicals.add(relief);

		GraphicalDrawOrder.sort(graphicals);

		assertSame(background, graphicals.get(0));
		assertSame(shadow, graphicals.get(1));
		assertSame(figure, graphicals.get(2));
		assertSame(relief, graphicals.get(3));
	}

	private static OutputGraphicalInterface graphical(float layer) {
		OutputGraphicalInterface graphical = new OutputGraphicalInterface() {
		};
		graphical.layer.value = layer;
		return graphical;
	}
}
