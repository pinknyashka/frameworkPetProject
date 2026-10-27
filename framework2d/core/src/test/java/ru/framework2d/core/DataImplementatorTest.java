package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ru.framework2d.data.Bool;
import ru.framework2d.data.DataInterface;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;

public class DataImplementatorTest {

	private static DataInterface data(String rawValue) {
		return DataImplementator.toProperty("probe", rawValue).data;
	}

	@Test
	public void integerStaysInteger() {
		DataInterface d = data("123");
		assertTrue(d instanceof Numeral);
		assertEquals(123, ((Numeral) d).value);
	}

	@Test
	public void fractionalStaysFractional() {
		DataInterface d = data("12.5");
		assertTrue(d instanceof Fractional);
		assertEquals(12.5f, ((Fractional) d).value, 0f);
	}

	@Test
	public void arithmeticKeepsTheMostPreciseOperandType() {
		DataInterface sum = data("1+2");
		assertTrue(sum instanceof Numeral);
		assertEquals(3, ((Numeral) sum).value);

		DataInterface div = data("10/4");
		assertTrue(div instanceof Fractional);
		assertEquals(2.5f, ((Fractional) div).value, 0f);

		DataInterface exactDiv = data("10/5");
		assertTrue(exactDiv instanceof Numeral);
		assertEquals(2, ((Numeral) exactDiv).value);

		DataInterface mixed = data("2*3.5");
		assertTrue(mixed instanceof Fractional);
		assertEquals(7f, ((Fractional) mixed).value, 0f);
	}

	@Test
	public void parenthesesAndUnaryMinus() {
		assertEquals(12, ((Numeral) data("(1+2)*4")).value);
		assertEquals(5, ((Numeral) data("-5+10")).value);
	}

	@Test
	public void comparisonsProduceBools() {
		assertTrue(((Bool) data("3>2")).value);
		assertTrue(((Bool) data("5>=5")).value);
		assertFalseData("5<3");
		assertTrue(((Bool) data("5=5")).value);
	}

	private void assertFalseData(String raw) {
		DataInterface d = data(raw);
		assertTrue(d instanceof Bool);
		assertTrue(!((Bool) d).value);
	}

	@Test
	public void booleanOperators() {
		assertTrue(!((Bool) data("true&&false")).value);
		assertTrue(((Bool) data("true||false")).value);
		assertTrue(((Bool) data("true=true")).value);
	}

	@Test
	public void stringForcePrefix() {
		DataInterface d = data("%sSprite Name");
		assertTrue(d instanceof Text);
		assertEquals("Sprite Name", ((Text) d).value);
	}

	@Test
	public void identifiersFallBackToText() {
		DataInterface d = data("someName");
		assertTrue(d instanceof Text);
		assertEquals("someName", ((Text) d).value);

		DataInterface color = data("#FFFFFF");
		assertTrue(color instanceof Text);
		assertEquals("#FFFFFF", ((Text) color).value);
	}

	@Test
	public void propertyKeepsItsName() {
		Property p = DataImplementator.toProperty("radius", "10");
		assertEquals("radius", p.name);
		assertTrue(p.isNamed("RADIUS"));
	}

	@Test
	public void consecutiveCallsAreIndependent() {
		assertEquals(3, ((Numeral) data("1+2")).value);
		assertTrue(data("someName") instanceof Text);
		assertEquals(4, ((Numeral) data("2*2")).value);
	}
}
