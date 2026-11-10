package ru.framework2d.compat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LightingColorFilterTest {

	@Test
	public void aLightingFilterWithColorBlackAsAdditiveColorKeepsSourceAlpha() {
		LightingColorFilter filter = new LightingColorFilter(Color.WHITE, Color.BLACK);

		int result = filter.filter(Color.argb(120, 10, 20, 30));

		assertEquals(120, Color.alpha(result));
		assertEquals(10, Color.red(result));
		assertEquals(20, Color.green(result));
		assertEquals(30, Color.blue(result));
	}

	@Test
	public void additiveColorStillChangesRgb() {
		LightingColorFilter filter = new LightingColorFilter(Color.rgb(255, 255, 255), Color.rgb(10, 20, 30));

		int result = filter.filter(Color.argb(64, 100, 100, 100));

		assertEquals(64, Color.alpha(result));
		assertEquals(110, Color.red(result));
		assertEquals(120, Color.green(result));
		assertEquals(130, Color.blue(result));
	}
}
