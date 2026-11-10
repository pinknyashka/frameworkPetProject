package ru.framework2d.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MovementAndVector3Test {

	private static final double EPS = 1e-9;

	@Test
	public void vector3LengthAndRestore() {
		Vector3 v = new Vector3(2, 3, 6);
		assertEquals(7, v.length3(), EPS);
		assertEquals(7, v.restore(), EPS);
		assertEquals(1, v.length3(), EPS);
		assertEquals(2.0 / 7.0, v.getX(), EPS);
	}

	@Test
	public void vector3SetAndArithmetic() {
		Vector3 a = new Vector3(1, 2, 3);
		Vector3 b = new Vector3(4, 6, 8);

		Vector3 sum = a.plus(b);
		assertEquals(5, sum.getX(), EPS);
		assertEquals(8, sum.getY(), EPS);
		assertEquals(11, sum.getZ(), EPS);

		Vector3 diff = b.minus(a);
		assertEquals(3, diff.getX(), EPS);
		assertEquals(4, diff.getY(), EPS);
		assertEquals(5, diff.getZ(), EPS);

		a.selfMulti(2);
		assertEquals(2, a.getX(), EPS);
		assertEquals(6, a.getZ(), EPS);

		Vector3 fromPoints = new Vector3(new Point3(10, 10, 10), new Point3(1, 2, 3));
		assertEquals(9, fromPoints.getX(), EPS);
		assertEquals(8, fromPoints.getY(), EPS);
		assertEquals(7, fromPoints.getZ(), EPS);
	}

	// Pins CURRENT behaviour: the helper divides by the length of THIS vector,
	// not by the length of the axis vector, so a non-unit axis scales the result.
	@Test
	public void projectionHelperPinsCurrentNormalization() {
		Vector3 v = new Vector3(3, 4, 0);
		assertEquals(8, v.myProectionOnVector(0, 10, 0), EPS); // (3*0+4*10+0*0)/5
		assertEquals(0, new Vector3(0, 0, 0).myProectionOnVector(1, 0, 0), EPS);
	}

	@Test
	public void movement3IsMovingCoversLinearAndAngularParts() {
		Movement3 m = new Movement3(1, 0, 0);
		assertFalse(m.isMoving());

		m.currentSpeed.value = 3;
		assertTrue(m.isMoving());

		m.stop();
		assertFalse(m.isMoving());

		m.alphaRotation.value = 0.1;
		assertTrue(m.isMoving());
		m.stop();

		m.betaRotation.value = -1;
		assertTrue(m.isMoving());
		m.stop();

		m.gammaRotation.value = 1;
		assertTrue(m.isMoving());
		m.stop();
		assertFalse(m.isMoving());
	}

	@Test
	public void movement3SetMovementCopiesAllChannels() {
		Movement3 src = new Movement3(1, 2, 3);
		src.currentSpeed.value = 8;
		src.alphaRotation.value = 0.5;
		src.betaRotation.value = 0.25;
		src.gammaRotation.value = 0.125;

		Movement3 dst = new Movement3();
		dst.setMovement(src);

		assertEquals(1, dst.getX(), EPS);
		assertEquals(2, dst.getY(), EPS);
		assertEquals(3, dst.getZ(), EPS);
		assertEquals(8, dst.currentSpeed.value, EPS);
		assertEquals(0.5, dst.alphaRotation.value, EPS);
		assertEquals(0.25, dst.betaRotation.value, EPS);
		assertEquals(0.125, dst.gammaRotation.value, EPS);
	}

	@Test
	public void movement3PropertyPathsForInnerChannels() {
		Movement3 m = new Movement3();
		m.setName("movement");
		assertTrue(m.setPropertyData("movement:currentSpeed", new DoubleFractional(5)));
		assertTrue(m.setPropertyData("movement:alphaRotation", new DoubleFractional(-2)));
		assertEquals(5, m.currentSpeed.value, EPS);
		assertEquals(-2, m.alphaRotation.value, EPS);

		// Pins CURRENT behaviour: the String-based property path is not
		// overridden by Movement3, so channel names are not reachable there.
		assertFalse(m.setProperty("movement:currentSpeed", "11"));
		assertEquals(5, m.currentSpeed.value, EPS);
	}

	@Test
	public void movement3CopyConstructors() {
		Movement3 src = new Movement3(1, 2, 3);
		src.currentSpeed.value = 4;

		Movement3 byMovement = new Movement3(src);
		assertEquals(4, byMovement.currentSpeed.value, EPS);

		Movement3 byVector = new Movement3(new Vector3(5, 6, 7));
		assertEquals(5, byVector.getX(), EPS);
		assertEquals(0, byVector.currentSpeed.value, EPS);
	}
}
