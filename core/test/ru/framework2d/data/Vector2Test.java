package ru.framework2d.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Vector2Test {

	private static final double EPS = 1e-9;

	@Test
	public void setAndLength() {
		Vector2 v = new Vector2();
		v.set(3, 4);
		assertEquals(3, v.getX(), EPS);
		assertEquals(4, v.getY(), EPS);
		assertEquals(5, v.length2(), EPS);

		Vector2 fromPoints = new Vector2(new Point2(10, 10), new Point2(4, 6));
		assertEquals(6, fromPoints.getX(), EPS);
		assertEquals(4, fromPoints.getY(), EPS);
	}

	@Test
	public void restoreNormalizesAndReturnsOldLength() {
		Vector2 v = new Vector2(3, 4);
		assertEquals(5, v.restore(), EPS);
		assertEquals(0.6, v.getX(), EPS);
		assertEquals(0.8, v.getY(), EPS);
		assertEquals(1, v.length2(), EPS);
	}

	@Test
	public void restoreOfZeroVectorDoesNothing() {
		Vector2 v = new Vector2(0, 0);
		assertEquals(0, v.restore(), EPS);
		assertEquals(0, v.getX(), EPS);
		assertEquals(0, v.getY(), EPS);
	}

	@Test
	public void computeAngleAgainstNormalDownward() {
		assertEquals(0, new Vector2(0, -1).computeAngle(), EPS);
		assertEquals(Math.PI / 2, new Vector2(1, 0).computeAngle(), EPS);
		assertEquals(-Math.PI / 2, new Vector2(-1, 0).computeAngle(), EPS);
		assertEquals(-Math.PI, new Vector2(0, 1).computeAngle(), EPS);
		assertEquals(0, new Vector2(0, 0).computeAngle(), EPS);
	}

	@Test
	public void computeAngleBetweenVectorsIsWrapped() {
		Vector2 down = new Vector2(0, -1);
		Vector2 right = new Vector2(1, 0);
		assertEquals(Math.PI / 2, down.computeAngle(right), EPS);
		assertEquals(-Math.PI / 2, right.computeAngle(down), EPS);
	}

	@Test
	public void rotateUsesStandardMatrix() {
		Vector2 v = new Vector2(1, 0);
		v.rotate(Math.PI / 2);
		assertEquals(0, v.getX(), EPS);
		assertEquals(1, v.getY(), EPS);
	}

	@Test
	public void turnBackMirrorsBothAxes() {
		Vector2 v = new Vector2(3, 4);
		v.turnBack();
		assertEquals(-3, v.getX(), EPS);
		assertEquals(-4, v.getY(), EPS);
	}

	// Pins the CURRENT behaviour of the in-place 90 degree helpers: they read
	// back the already overwritten coordinate, so (3,4) does not become
	// (-4,3). If these helpers are ever fixed, these golden values change.
	@Test
	public void ninetyDegreeHelpersPinCurrentSelfReferencingBehavior() {
		Vector2 a = new Vector2(3, 4);
		a.toNormal();
		assertEquals(-4, a.getX(), EPS);
		assertEquals(-4, a.getY(), EPS);

		Vector2 b = new Vector2(3, 4);
		b.rotateRightNinety();
		assertEquals(-4, b.getX(), EPS);
		assertEquals(-4, b.getY(), EPS);

		Vector2 c = new Vector2(3, 4);
		c.rotateLeftNinety();
		assertEquals(4, c.getX(), EPS);
		assertEquals(-4, c.getY(), EPS);
	}

	@Test
	public void arithmeticHelpersReturnNewVectors() {
		Vector2 a = new Vector2(1, 2);
		Vector2 b = new Vector2(5, 10);

		Vector2 sum = a.plus(b);
		assertNotSame(a, sum);
		assertEquals(6, sum.getX(), EPS);
		assertEquals(12, sum.getY(), EPS);

		Vector2 diff = b.minus(a);
		assertEquals(4, diff.getX(), EPS);
		assertEquals(8, diff.getY(), EPS);

		Vector2 scaled = a.multi(3);
		assertEquals(3, scaled.getX(), EPS);
		assertEquals(6, scaled.getY(), EPS);
		assertEquals(1, a.getX(), EPS);
	}

	@Test
	public void inPlaceHelpersMutateSelf() {
		Vector2 a = new Vector2(1, 2);
		assertSame(a, a.selfMulti(2));
		assertEquals(2, a.getX(), EPS);
		assertSame(a, a.addict(new Vector2(1, 1)));
		assertEquals(3, a.getX(), EPS);
		assertSame(a, a.decrease(new Vector2(1, 1)));
		assertEquals(2, a.getX(), EPS);
		assertEquals(4, a.getY(), EPS);
	}

	@Test
	public void projections() {
		Vector2 v = new Vector2(3, 4);
		assertEquals(4, v.myProectionOnVector(0, 5), EPS);
		assertEquals(0, v.myProectionOnVector(0, 0), EPS);
		assertEquals(4, v.myProectionOnIdentityVector(new Vector2(0, 1)), EPS);
		assertEquals(4, v.myProectionOnVector(new Vector2(0, 3)), EPS);
		assertEquals(4, v.myAbsProectionOnVector(new Vector2(0, -3)), EPS);

		// Pins CURRENT behavior: the axis is not normalized before scaling,
		// so the returned "projection vector" is the raw axis times the
		// signed scalar projection.
		Vector2 vp = v.myVectorProectionOnVector(new Vector2(0, 10));
		assertEquals(0, vp.getX(), EPS);
		assertEquals(40, vp.getY(), EPS);

		Vector2 zero = v.myVectorProectionOnVector(new Vector2(0, 0));
		assertEquals(0, zero.getX(), EPS);
		assertEquals(0, zero.getY(), EPS);
	}

	@Test
	public void toVectorProjectionOnZeroVectorZeroesSelf() {
		Vector2 v = new Vector2(3, 4);
		v.toVectorProectionOnVector(new Vector2(0, 0));
		assertEquals(0, v.getX(), EPS);
		assertEquals(0, v.getY(), EPS);
	}

	@Test
	public void setPropertyDataMatchesCompositeNames() {
		Vector2 v = new Vector2();
		v.setName("movement");
		assertTrue(v.setPropertyData("movement:x", new DoubleFractional(7)));
		assertTrue(v.setPropertyData("movement:y", new DoubleFractional(9)));
		assertFalse(v.setPropertyData("movement:z", new DoubleFractional(1)));
		assertFalse(v.setPropertyData("other:x", new DoubleFractional(1)));
		assertEquals(7, v.getX(), EPS);
		assertEquals(9, v.getY(), EPS);
	}

	@Test
	public void setPropertyParsesStrings() {
		Vector2 v = new Vector2();
		v.setName("impulse");
		assertTrue(v.setProperty("impulse:x", "1.5"));
		assertTrue(v.setProperty("impulse:y", "-2.5"));
		assertFalse(v.setProperty("impulse:q", "1"));
		assertEquals(1.5, v.getX(), EPS);
		assertEquals(-2.5, v.getY(), EPS);
	}
}
