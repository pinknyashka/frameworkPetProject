package ru.framework2d.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PointAndPositionTest {

	private static final double EPS = 1e-9;

	@Test
	public void point2DistanceAndMove() {
		Point2 a = new Point2(0, 0);
		Point2 b = new Point2(3, 4);
		assertEquals(5, a.dist(b), EPS);

		a.move(new Vector2(1, 1));
		assertEquals(1, a.getX(), EPS);
		assertEquals(1, a.getY(), EPS);

		a.move(new Vector2(0, 1), 10);
		assertEquals(1, a.getX(), EPS);
		assertEquals(11, a.getY(), EPS);
	}

	@Test
	public void point2CopyAndSetVariants() {
		Point2 src = new Point2(2, 3);
		Point2 copy = new Point2(src);
		assertEquals(2, copy.getX(), EPS);
		copy.set(5, 6);
		assertEquals(5, copy.getX(), EPS);
		copy.set(src);
		assertEquals(2, copy.getX(), EPS);
	}

	@Test
	public void checkLineHelpersUseStrictInequalities() {
		Point2 p = new Point2(10, 20);
		assertTrue(p.checkLineX(0, 20, 1));
		assertFalse(p.checkLineX(9, 20, 1));   // 10-1 > 9 is false: border is exclusive
		assertTrue(p.checkLineY(0, 40, 5));
		assertFalse(p.checkLineY(0, 25, 5));
	}

	@Test
	public void checkFieldVariants() {
		Point2 p = new Point2(50, 50);
		assertTrue(p.checkField(100, 100, 10));
		assertFalse(p.checkField(0, 100, 10)); // first parameter is the right/bottom bound in this helper
		assertTrue(p.checkField(0, 0, 100, 100, 10));
		assertFalse(p.checkField(0, 0, 55, 100, 10));
	}

	@Test
	public void point3AddsZAxis() {
		Point3 a = new Point3(0, 0, 0);
		Point3 b = new Point3(2, 3, 6);
		assertEquals(7, a.dist(b), EPS);

		a.move(new Vector3(1, 1, 1));
		assertEquals(1, a.getZ(), EPS);

		b.set(1, 2, 3);
		a.set(b);
		assertEquals(3, a.getZ(), EPS);
	}

	@Test
	public void point3InnerDataAndPropertyPaths() {
		Point3 p = new Point3();
		p.setName("contact");
		assertEquals(p.z, p.getInnerData("z"));
		assertNull(p.getInnerData("q"));
		assertTrue(p.setPropertyData("contact:z", new DoubleFractional(4)));
		assertTrue(p.setProperty("contact:z", "-1"));
		assertEquals(-1, p.getZ(), EPS);
	}

	@Test
	public void position3Angles() {
		Position3 p = new Position3(1, 2, 3, 0.5, 0.25, 0.125);
		assertEquals(0.5, p.getAlpha(), EPS);
		assertEquals(0.25, p.getBeta(), EPS);
		assertEquals(0.125, p.getGamma(), EPS);

		Position3 q = new Position3(4, 5, 6);
		p.set(q);
		assertEquals(0, p.getAlpha(), EPS); // set(Point3) overload resets angles

		Position3 r = new Position3(7, 8, 9, 1, 1, 1);
		p.set(r);
		assertEquals(1, p.getAlpha(), EPS);
		assertEquals(9, p.getZ(), EPS);
	}

	@Test
	public void position3InnerDataAndPropertyPaths() {
		Position3 p = new Position3();
		p.setName("position");
		assertEquals(p.alpha, p.getInnerData("alpha"));
		assertEquals(p.beta, p.getInnerData("beta"));
		assertEquals(p.gamma, p.getInnerData("gamma"));
		assertEquals(p.z, p.getInnerData("z"));

		assertTrue(p.setPropertyData("position:alpha", new DoubleFractional(2)));
		assertTrue(p.setProperty("position:beta", "3"));
		assertTrue(p.setProperty("position:gamma", "4"));
		assertEquals(2, p.getAlpha(), EPS);
		assertEquals(3, p.getBeta(), EPS);
		assertEquals(4, p.getGamma(), EPS);
	}

	@Test
	public void position3FromPosition2() {
		Position2 p2 = new Position2();
		p2.set(11, 22);
		p2.setAlpha(0.7);

		Position3 p3 = new Position3();
		p3.set(p2);
		assertEquals(11, p3.getX(), EPS);
		assertEquals(22, p3.getY(), EPS);
		assertEquals(0, p3.getZ(), EPS);
		assertEquals(0.7, p3.getAlpha(), EPS);
	}
}
