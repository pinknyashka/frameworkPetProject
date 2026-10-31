package ru.framework2d.graphics.render;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShadowBlurPolicyTest {

	@Test
	public void sharpModeDisablesBlur() {
		assertEquals(0.0f, RenderServices.effectiveBlurRadius(2.0f, true), 0.0f);
	}

	@Test
	public void softModeUsesRequestedBlur() {
		assertEquals(2.0f, RenderServices.effectiveBlurRadius(2.0f, false), 0.0f);
	}

	@Test
	public void zeroOrNegativeSoftnessIsSharp() {
		assertEquals(0.0f, RenderServices.effectiveBlurRadius(0.0f, false), 0.0f);
		assertEquals(0.0f, RenderServices.effectiveBlurRadius(-1.0f, false), 0.0f);
	}
}
