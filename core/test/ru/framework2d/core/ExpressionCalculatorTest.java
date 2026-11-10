package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExpressionCalculatorTest {

	private static double value(String expression) {
		return Double.parseDouble(ExpressionCalculator.calculate(expression));
	}

	@Test
	public void plainNumbersBecomeDoubles() {
		assertEquals(1024.0, value("1024"), 1e-9);
		assertEquals(12.5, value("12.5"), 1e-9);
	}

	@Test
	public void arithmeticPrecedenceAndParens() {
		assertEquals(3, value("1+2"), 1e-9);
		assertEquals(14, value("2+3*4"), 1e-9);
		assertEquals(20, value("(2+3)*4"), 1e-9);
		assertEquals(2.5, value("10/4"), 1e-9);
		assertEquals(88.88888888888889, value("100*8/9"), 1e-6);
		assertEquals(-3, value("(-5)+2"), 1e-9);
		assertEquals(10, value("(1+2)*(4)-2+1-1"), 1e-9);
	}

	@Test
	public void identifiersAreNotCalculableAndPassThrough() {
		assertEquals("width/24", ExpressionCalculator.calculate("width/24"));
		assertEquals("#FFFFFF", ExpressionCalculator.calculate("#FFFFFF"));
	}

	@Test
	public void literalPrefixForcesString() {
		assertEquals("Sprite Name", ExpressionCalculator.calculate("%sSprite Name"));
	}

	@Test
	public void unbalancedParensPassThrough() {
		assertEquals("(1+2", ExpressionCalculator.calculate("(1+2"));
	}

	@Test
	public void resultsKeepGoingAfterFailedParses() {
		ExpressionCalculator.calculate("width/24");   // leaves internal state cleared
		assertEquals(5.0, value("2+3"), 1e-9);
	}

	@Test
	public void divisionByZeroProducesInfinityText() {
		String result = ExpressionCalculator.calculate("1/0");
		assertTrue(result.contains("Infinity"));
	}
}
