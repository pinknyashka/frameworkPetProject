package ru.framework2d.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ru.framework2d.core.DataContext;

public class ScalarDataTest {

	@Test
	public void fractionalAcceptsEveryNumericSource() {
		Fractional f = new Fractional();
		f.setValue(new Numeral(7));
		assertEquals(7f, f.value, 0f);
		f.setValue(new Fractional(2.5f));
		assertEquals(2.5f, f.value, 0f);
		f.setValue(new DoubleFractional(0.125));
		assertEquals(0.125f, f.value, 0f);
		f.setValue(new Text("3.75"));
		assertEquals(3.75f, f.value, 0f);
		f.setValue(new Bool(true));
		assertEquals(1f, f.value, 0f);
	}

	@Test
	public void numeralTruncatesAndParses() {
		Numeral n = new Numeral();
		n.setValue(new Fractional(9.9f));
		assertEquals(9, n.value);
		n.setValue(new Text("12.8"));
		assertEquals(12, n.value);
		n.setValue(new Bool(true));
		assertEquals(1, n.value);
		n.setValue(new Bool(false));
		assertEquals(0, n.value);
	}

	@Test
	public void boolAcceptsTextNumeralsAndNumbers() {
		Bool b = new Bool();
		b.setValue(new Text("true"));
		assertTrue(b.value);
		b.setValue(new Text("False"));
		assertFalse(b.value);
		b.setValue(new Text("1"));
		assertTrue(b.value);
		b.setValue(new Text("0"));
		assertFalse(b.value);
		b.setValue(new Numeral(1));
		assertTrue(b.value);
		b.setValue(new Numeral(5));
		assertFalse(b.value); // only exactly 1 counts as true
		b.setValue(new Fractional(1.0f));
		assertTrue(b.value);
	}

	@Test
	public void textStringifiesAnyScalar() {
		Text t = new Text();
		t.setValue(new Numeral(3));
		assertEquals("3", t.value);
		t.setValue(new Bool(false));
		assertEquals("false", t.value);
		t.setValue(new Text("hello"));
		assertEquals("hello", t.value);
	}

	@Test
	public void doubleFractionalHighPrecision() {
		DoubleFractional d = new DoubleFractional();
		d.setValue(new Text("0.1"));
		assertEquals(0.1, d.value, 0.0);
		d.setValue(new Bool(true));
		assertEquals(1.0, d.value, 0.0);
		d.setValue(new Fractional(1.5f));
		assertEquals(1.5, d.value, 1e-7);
	}

	@Test
	public void setPropertyByNameOnlyWhenNameMatches() {
		Fractional f = new Fractional();
		f.setName("radius");
		assertTrue(f.setProperty("radius", "4.5"));
		assertEquals(4.5f, f.value, 0f);
		assertFalse(f.setProperty("weight", "1"));

		Numeral n = new Numeral();
		n.setName("columnsNumber");
		assertTrue(n.setProperty("columnsNumber", "8.9"));
		assertEquals(8, n.value);

		Bool b = new Bool();
		b.setName("isStatic");
		assertTrue(b.setProperty("isStatic", "true"));
		assertTrue(b.value);
		assertTrue(b.setProperty("isStatic", "0"));
		assertFalse(b.value);

		Text t = new Text();
		t.setName("spriteName");
		assertTrue(t.setProperty("spriteName", "board"));
		assertEquals("board", t.value);
	}

	@Test
	public void setPropertyDataUsesExactNameMatch() {
		Fractional weight = new Fractional(10);
		weight.setName("weight");
		assertTrue(weight.setPropertyData("weight", new Numeral(25)));
		assertEquals(25f, weight.value, 0f);
		assertFalse(weight.setPropertyData("weight2", new Numeral(1)));
	}

	@Test
	public void bindingValueHooksApplyTyped() {
		Numeral n = new Numeral();
		n.applyBindingValue(3.9);
		assertEquals(3, n.value);

		Fractional f = new Fractional();
		f.applyBindingValue(2.5);
		assertEquals(2.5f, f.value, 0f);

		Bool b = new Bool();
		b.applyBindingValue(1);
		assertTrue(b.value);
		b.applyBindingValue(0.5);
		assertFalse(b.value);

		DoubleFractional d = new DoubleFractional();
		d.applyBindingValue(-1.25);
		assertEquals(-1.25, d.value, 0.0);
	}

	@Test
	public void setDataCopiesValueContextAndName() {
		Fractional src = new Fractional(6.5f);
		src.setName("zoom");
		DataContext ctx = new DataContext("camera", null);
		src.setContext(ctx);

		Fractional dst = new Fractional();
		dst.setData(src);
		assertEquals(6.5f, dst.value, 0f);
		assertEquals("zoom", dst.getName());
		assertEquals(ctx, dst.getContext());
	}

	@Test
	public void triggerActsAsAValuelessSignal() {
		Trigger t = new Trigger();
		assertTrue(t.setProperty("collision", "whatever"));
		assertTrue(t.setProperty("collision", "")); // accepts any property as a signal
		t.setValue(new Bool(true)); // no-op, must not throw
	}

	@Test
	public void defaultsMatchEngineAssumptions() {
		assertEquals(0f, new Fractional().value, 0f);
		assertEquals(0, new Numeral().value);
		assertFalse(new Bool().value);
		assertEquals("", new Text().value);
		assertEquals(0.0, new DoubleFractional().value, 0.0);
		assertEquals("Fractional", new Fractional().getName());
	}
}
