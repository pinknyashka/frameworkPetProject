package ru.framework2d.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AreaTest {

	private static final double EPS = 1e-4;

	@Test
	public void setAreaFillsCenterRadiusAndDerivedAreas() {
		Area a = new Area();
		a.setArea(20, 30, 0, 10);
		assertEquals(10, a.radius, EPS);
		assertEquals(20, a.getX(), EPS);
		assertEquals(30, a.getY(), EPS);
		assertEquals(Math.PI * 100, a.areaBySphere, 1.0f);
		assertEquals(600, a.areaByAabb, EPS);
	}

	@Test
	public void aabbAroundCenterForAxisAlignedBox() {
		Area a = new Area();
		Position3 center = new Position3(100, 50, 0);
		a.setCenter(center);
		a.setArea(20, 10, 0, 12);

		a.left_top.set(90, 45, 0);
		a.right_bottom.set(110, 55, 0);

		a.isAabb = true;
		a.validateAabb();

		assertEquals(90, a.left_top.getX(), EPS);
		assertEquals(45, a.left_top.getY(), EPS);
		assertEquals(110, a.right_bottom.getX(), EPS);
		assertEquals(55, a.right_bottom.getY(), EPS);

		assertTrue(a.contains(new Point3(100, 50, 0)));
		assertFalse(a.contains(new Point3(120, 50, 0)));
	}

	@Test
	public void aabbAroundCenterForSphere() {
		Area a = new Area();
		Position3 center = new Position3(0, 0, 0);
		a.setCenter(center);
		a.setArea(0, 0, 0, 5);
		a.isSphere = true;
		a.validateAabb();

		assertEquals(-5, a.left_top.getX(), EPS);
		assertEquals(5, a.right_bottom.getX(), EPS);

		a.isAabb = false;
		assertTrue(a.contains(new Point3(4, 2, 0)));
		assertFalse(a.contains(new Point3(4, 4, 3)));
	}

	@Test
	public void rotatedBoxAabbGrows() {
		Area a = new Area();
		Position3 center = new Position3(0, 0, 0);
		a.setCenter(center);
		a.setArea(10, 0, 0, 0);
		a.isAabb = true;
		a.validateAabb();
		double flatWidth = a.right_bottom.getX() - a.left_top.getX();
		assertEquals(10, flatWidth, EPS);

		center.setAlpha(Math.PI / 4);
		a.validateAabb();
		double tiltedWidth = a.right_bottom.getX() - a.left_top.getX();
		assertEquals(10 * Math.cos(Math.PI / 4), tiltedWidth, EPS);
	}

	@Test
	public void containsReportsFalseForUnknownShapeFlags() {
		Area a = new Area();
		a.setCenter(new Position3(0, 0, 0));
		assertFalse(a.contains(new Point3(0, 0, 0)));
	}

	@Test
	public void incorruptedDetectsSweptAabbOverlap() {
		Area a = new Area();
		Position3 ca = new Position3(0, 0, 0);
		a.setCenter(ca);
		a.setArea(10, 10, 0, 8);
		a.isAabb = true;
		a.validateAabb();

		Area b = new Area();
		Position3 cb = new Position3(12, 0, 0);
		b.setCenter(cb);
		b.setArea(10, 10, 0, 8);
		b.isAabb = true;
		b.validateAabb();

		assertFalse(a.incorrupted(new Vector2(0, 0), b, new Vector2(0, 0)));

		// sweeping a to the right by 10 px must hit b
		assertTrue(a.incorrupted(new Vector2(10, 0), b, new Vector2(0, 0)));
		// and the mirror view: from b's point of view a sweeps into it
		assertTrue(b.incorrupted(new Vector2(0, 0), a, new Vector2(10, 0)));
	}

	@Test
	public void setAreaCopiesAnotherArea() {
		Area src = new Area();
		src.setArea(20, 10, 0, 12);
		src.isAabb = true;

		Area dst = new Area();
		dst.setArea(src);
		assertEquals(12, dst.radius, EPS);
		assertEquals(20, dst.getX(), EPS);
		assertTrue(dst.isAabb);
	}

	@Test
	public void validateAreaUsesZForVolumetricEstimate() {
		Area a = new Area();
		a.setArea(10, 10, 10, 5);
		assertEquals(Math.PI * 125, a.areaBySphere, 1.0f);
	}
}
