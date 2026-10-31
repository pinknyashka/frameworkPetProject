package ru.framework2d.graphics.render.shadow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShadowProjectionMathTest {

	private static final double EPS = 1e-3;

	@Test
	public void awayDirection() {
		assertEquals(0.70710678, ShadowRenderSystem.awayX(0, 0, -1, -1), EPS);
		assertEquals(0.70710678, ShadowRenderSystem.awayY(0, 0, -1, -1), EPS);
	}

	@Test
	public void awayDirectionDegenerate() {
		assertEquals(0, ShadowRenderSystem.awayX(-2.5f, 3.5f, -2.5f, 3.5f), EPS);
		assertEquals(0, ShadowRenderSystem.awayY(-2.5f, 3.5f, -2.5f, 3.5f), EPS);
	}

	@Test
	public void tallerCasterLongerShadow() {
		float shortCaster = ShadowRenderSystem.shadowLength(0, 0, -60, 0, 50, 2, 0);
		float tallCaster = ShadowRenderSystem.shadowLength(0, 0, -60, 0, 50, 8, 0);
		assertTrue("height 8 (" + tallCaster + ") must beat height 2 (" + shortCaster + ")",
				tallCaster > shortCaster);
	}

	@Test
	public void fartherCasterLongerShadow() {
		float near = ShadowRenderSystem.shadowLength(0, 0, -30, 0, 50, 5, 0);
		float far = ShadowRenderSystem.shadowLength(0, 0, -90, 0, 50, 5, 0);
		assertTrue("distance 90 (" + far + ") must beat distance 30 (" + near + ")", far > near);
	}

	@Test
	public void higherLightShorterShadow() {
		float lowLight = ShadowRenderSystem.shadowLength(0, 0, -60, 0, 50, 10, 0);
		float highLight = ShadowRenderSystem.shadowLength(0, 0, -60, 0, 100, 10, 0);
		assertTrue("lightZ 50 (" + lowLight + ") must beat lightZ 100 (" + highLight + ")",
				lowLight > highLight);
	}

	@Test
	public void minimumLengthFloor() {
		assertEquals(7, ShadowRenderSystem.shadowLength(0, 0, 0, 0, 50, 5, 7), EPS);
	}

	@Test
	public void grazingLightCapped() {
		assertEquals(300, ShadowRenderSystem.shadowLength(0, 0, -1000, 0, 50, 50, 0), EPS);
	}

	@Test
	public void stretchCapNearGrazing() {
		float justAboveGrazing = ShadowRenderSystem.shadowLength(0, 0, -10, 0, 50.002f, 50, 0);
		assertEquals(90, justAboveGrazing, EPS);

		float hugeDistance = ShadowRenderSystem.shadowLength(0, 0, -100000, 0, 50.002f, 50, 0);
		assertTrue(hugeDistance <= 300);
		assertTrue(hugeDistance >= 0);
	}

	@Test
	public void projectionMatchesLegacyPointLightHomothety() {
		float scale = ShadowRenderSystem.projectionScale(50, 0, 2);
		assertEquals(50.0d / 48.0d, scale, EPS);

		float length = ShadowRenderSystem.shadowLength(0, 0, -60, 0, 50, 0, 2, 0);
		assertEquals((scale - 1.0d) * 60.0d, length, EPS);
		assertEquals(scale, ShadowRenderSystem.effectiveStretch(60, length, scale), EPS);
	}

	@Test
	public void projectionScaleShrinksAsLightRises() {
		assertTrue(ShadowRenderSystem.projectionScale(50, 0, 10) > ShadowRenderSystem.projectionScale(100, 0, 10));
	}

	@Test
	public void overHeadShadowHasNoHorizontalOffsetWhenMinimumLengthIsZero() {
		assertEquals(0, ShadowRenderSystem.shadowLength(0, 0, 0, 0, 50, 0, 2, 0), EPS);
	}
}
