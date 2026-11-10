package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.logic.fields.HandlerFields;
import ru.framework2d.logic.fields.LogicalField;
import ru.framework2d.logic.grids.LogicalCellGroup;
import ru.framework2d.testsupport.TestSupport;

/**
 * The reference half of {@link Expression}: how {@code $entity.Component.field} is resolved
 * against {@link EntityStorage}, when a reference is read again, and what each failure mode
 * answers. The pure calculator is covered by {@code ExpressionCalculatorTest}.
 *
 * Entities are assembled by hand, so resolution is exercised without a map.
 */
public class ExpressionReferenceTest {

	private EntityStorage storage;

	@Before
	public void setUp() {
		storage = TestSupport.bootstrapEngine();
	}

	@After
	public void tearDown() {
		storage = null;
	}

	/** Registers a fresh entity and one component in it, returning the component. */
	private <T extends Component> T in(String entityName, T component) {
		Entity entity = new Entity(entityName);
		entity.addComponent(component);
		storage.lstEntities.add(entity);
		return component;
	}

	private Expression accepted(String text) {
		Expression expression = new Expression(storage);
		assertTrue("expression must be accepted: " + text, expression.setExpression(null, text));
		return expression;
	}

	private double value(String text) {
		return accepted(text).calculate();
	}

	@Test
	public void fractionReferenceIsReReadOnEveryCalculate() {
		LogicalField field = in("field", new LogicalField());
		field.radius.value = 4;

		Expression expression = accepted("$field.LogicalField.radius+0");
		assertEquals(4.0, expression.calculate(), 0.0001);

		field.radius.value = 9;
		assertEquals(9.0, expression.calculate(), 0.0001);
	}

	@Test
	public void boolReferenceReadsAsOneOrZero() {
		LogicalField field = in("field", new LogicalField());
		field.gotMovingUnits.value = true;

		assertEquals(1.0, value("$field.LogicalField.gotMovingUnits+0"), 0.0001);

		field.gotMovingUnits.value = false;
		assertEquals(0.0, value("$field.LogicalField.gotMovingUnits+0"), 0.0001);
	}

	@Test
	public void pins_bareReferenceIsFrozenAtZero() {
		// Expression.java:518-521 builds the primary node without marking it as a data
		// reference, so Expression.java:285-292 hands back the untouched d_value instead of
		// reading the data the reference already resolved to
		LogicalField field = in("field", new LogicalField());
		field.radius.value = 4;

		Expression expression = accepted("$field.LogicalField.radius");
		assertEquals(0.0, expression.calculate(), 0.0001);

		field.radius.value = 9;
		assertEquals(0.0, expression.calculate(), 0.0001);
	}

	@Test
	public void pins_onlyBoolAndFractionalReferencesContributeAValue() {
		// Expression.java:70-87 converts only Bool and Fractional operands, so the value of a
		// Numeral, Text or DoubleFractional field is always read as 0
		LogicalField field = in("field", new LogicalField());
		field.name.value = "board";
		field.radius.value = 6;

		LogicalCellGroup group = in("group", new LogicalCellGroup());
		group.leaderRow.value = 5;

		assertEquals(0.0, value("$group.LogicalCellGroup.leaderRow+0"), 0.0001);
		assertEquals(0.0, value("$field.LogicalField.name+0"), 0.0001);
		assertEquals(6.0, value("$field.LogicalField.radius+0"), 0.0001);
	}

	@Test
	public void colonChainReachesTheInnerFieldButItsValueIsIgnored() {
		// Expression.java:689-717 walks the inner data of a compound field, yet the inner
		// DoubleFractional of Position3 is invisible to the operand conversion
		LogicalField field = in("field", new LogicalField());
		field.position.setX(123);

		assertEquals(1.0, value("$field.LogicalField.position:x+1"), 0.0001);
	}

	@Test
	public void pins_unresolvedReferenceAnswersZeroAndStillReportsSuccess() {
		// Expression.java:723 turns every miss into an UNREACHABLE_DATA variable, which
		// Expression.java:284-294 answers as 0 while setExpression still returns true
		in("field", new LogicalField());

		assertTrue(new Expression(storage).setExpression(null, "$ghost.LogicalField.radius+0"));
		assertTrue(new Expression(storage).setExpression(null, "$field.NoSuchComponent.radius+0"));
		assertTrue(new Expression(storage).setExpression(null, "$field.LogicalField.nosuchfield+0"));

		assertEquals(0.0, value("$ghost.LogicalField.radius+0"), 0.0001);
		assertEquals(0.0, value("$field.NoSuchComponent.radius+0"), 0.0001);
		assertEquals(0.0, value("$field.LogicalField.nosuchfield+0"), 0.0001);
	}

	@Test
	public void pins_aReferenceResolvesAgainstTheFirstEntityWithThatName() {
		// Expression.java:651-652 walks lstEntities in insertion order and returns on the
		// first entity+component+field match, and EntityStorage keeps duplicate names
		in("dup", new LogicalField()).radius.value = 3;
		in("dup", new LogicalField()).radius.value = 8;

		assertEquals(3.0, value("$dup.LogicalField.radius+0"), 0.0001);
	}

	@Test
	public void pins_componentMatchingIsContainsNotEquals() {
		// Expression.java:660 tests componentName.contains(component.getName())
		in("field", new LogicalField()).radius.value = 4;

		assertEquals(4.0, value("$field.LogicalFieldExtra.radius+0"), 0.0001);
	}

	@Test
	public void pins_entityNameMatchingIsExact() {
		in("field", new LogicalField()).radius.value = 4;

		assertEquals(0.0, value("$fieldx.LogicalField.radius+0"), 0.0001);
	}

	@Test
	public void pins_referenceToANonDataFieldRaisesClassCastException() {
		// Expression.java:674 casts the field value to DataInterface inside a try that only
		// handles IllegalArgumentException and IllegalAccessException
		in("field", new LogicalField());

		try {
			value("$field.LogicalField.lstUnitsInField+0");
			fail("an ArrayList field must not be treated as data");
		} catch (ClassCastException expected) {
			// the reference is rejected before any value is produced
		}
	}

	@Test
	public void pins_incompleteReferencePathThrowsStringIndexOutOfBounds() {
		// Expression.java:649 assumes a dot, Expression.java:654-655 assumes a second one
		in("field", new LogicalField());

		try {
			value("$field+0");
			fail("a reference without any component segment must not pass");
		} catch (StringIndexOutOfBoundsException expected) {
			// "field".indexOf('.') is -1
		}

		try {
			value("$field.LogicalField+0");
			fail("a reference without a field segment must not pass");
		} catch (StringIndexOutOfBoundsException expected) {
			// substring(begin, begin - 1) because the second dot is missing
		}
	}

	@Test
	public void pins_brokenColonChainRaisesNullPointerException() {
		// Expression.java:695 dereferences currentData before checking that the outer
		// segment matched any field at all
		in("field", new LogicalField());

		try {
			value("$field.LogicalField.nosuchfield:x+1");
			fail("a colon chain starting at an unknown field must not pass");
		} catch (NullPointerException expected) {
			// the outer segment left currentData null
		}
	}

	@Test
	public void deferredReferenceIsResolvedOnALaterCalculate() {
		// the shipped binding map relies on this (assets/xml/binding_test.xml:80-88): a
		// reference to an entity parsed later is retried inside Operation.calculate (:63-67)
		Expression expression = accepted("$later.LogicalField.radius+0");
		assertEquals(0.0, expression.calculate(), 0.0001);

		in("later", new LogicalField()).radius.value = 7;

		assertEquals(7.0, expression.calculate(), 0.0001);
	}

	@Test
	public void bindingIsReappliedOnEveryCommit() {
		LogicalField field = in("field", new LogicalField());
		field.radius.value = 4;

		Fractional target = new Fractional(1);
		target.setBinding("$field.LogicalField.radius*2");

		target.commit(null, null);
		assertEquals(8f, target.value, 0.001f);

		field.radius.value = 5;
		target.commit(null, null);
		assertEquals(10f, target.value, 0.001f);
	}

	@Test
	public void pins_unresolvableBindingOverwritesTheValueWithZero() {
		Fractional target = new Fractional(4);
		target.setBinding("$ghost.LogicalField.radius*2");

		target.commit(null, null);

		assertEquals(0f, target.value, 0f);
	}

	@Test
	public void constantBindingIsAppliedOnEveryCommit() {
		Fractional target = new Fractional(1);
		target.setBinding("2.5");

		target.commit(null, null);
		assertEquals(2.5f, target.value, 0.001f);

		target.commit(null, null);
		assertEquals(2.5f, target.value, 0.001f);
	}

	@Test
	public void pins_emptyAndNonExpressionBindingsLeaveTheTargetUntouched() {
		// DataInterface.java:102 returns on an empty string; a bare word makes
		// Expression.java:344 give up, so setExpression answers false and no binding is set
		Fractional empty = new Fractional(4);
		empty.setBinding("");
		empty.commit(null, null);
		assertEquals(4f, empty.value, 0f);

		Fractional word = new Fractional(4);
		word.setBinding("keepme");
		word.commit(null, null);
		assertEquals(4f, word.value, 0f);
	}

	@Test
	public void pins_dataWithoutABindingOverrideSwallowsTheEvaluatedValue() {
		// DataInterface.java:109 is empty and Position3 never overrides it
		in("field", new LogicalField()).radius.value = 4;

		Position3 target = new Position3(1, 2, 0);
		target.setBinding("$field.LogicalField.radius*2");
		target.commit(null, null);

		assertEquals(1d, target.getX(), 0d);
		assertEquals(2d, target.getY(), 0d);
	}

	@Test
	public void applyBindingValueConvertsTheDoublePerTargetType() {
		in("field", new LogicalField()).radius.value = 4;

		Bool flag = new Bool(false);
		flag.setBinding("$field.LogicalField.radius>0");
		flag.commit(null, null);
		assertTrue(flag.value);

		Numeral number = new Numeral(0);
		number.setBinding("$field.LogicalField.radius*2.4");
		number.commit(null, null);
		assertEquals(9, number.value);

		Text text = new Text("keep");
		text.setBinding("$field.LogicalField.radius*2");
		text.commit(null, null);
		assertEquals("8.0", text.value);
	}

	@Test
	public void pins_boolTargetNeedsTheExpressionToBeExactlyOne() {
		// Bool.java:51-53 tests value == 1, so any other non zero result stays false
		in("field", new LogicalField()).radius.value = 2;

		Bool flag = new Bool(true);
		flag.setBinding("$field.LogicalField.radius+0");
		flag.commit(null, null);

		assertFalse(flag.value);
	}

	@Test
	public void bindingAttributeFromTagOptionsBindsTheDeclaredFieldAndStaysLive() {
		// the map path of Component.java:196-208, reproduced through a real handler so that
		// the component really owns a context filled lstData
		HandlerFields handler = new HandlerFields();

		LogicalField other = in("other", new LogicalField());
		other.radius.value = 4;

		TagOptions options = new TagOptions(LogicalField.SHORT_CLASS_NAME);
		options.lstPropertys.add(new Property("radius", new Numeral(1)));
		options.lstPropertys.add(new Property("radius_binding", new Text("$other.LogicalField.radius+1")));

		LogicalField bound = (LogicalField) handler.createComponent(new Entity("watch"), options);

		// creating the component only stores the binding, the first commit applies it
		assertEquals(1f, bound.radius.value, 0f);

		bound.radius.commit(handler, bound.entity);
		assertEquals(5f, bound.radius.value, 0.001f);

		other.radius.value = 7;
		bound.radius.commit(handler, bound.entity);
		assertEquals(8f, bound.radius.value, 0.001f);
	}
}
