package ru.framework2d.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.framework2d.data.Bool;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.graphics.OutputGraphicalInterface;
import ru.framework2d.logic.fields.LogicalField;
import ru.framework2d.logic.LogicalInterface;
import ru.framework2d.physics.PhysicalInterface;
import ru.framework2d.testsupport.TestSupport;
import ru.framework2d.touches.InputTouchInterface;
import ru.framework2d.touches.screen.TouchFinger;

/**
 * Prototype marking and the clone path of {@code new Entity(prototype)}: {@code createClone}
 * first, {@code createAutoClone} as the fallback, and the reflective field copy of
 * {@code Component.setPrototypePropertys}.
 *
 * Entities are built by hand here (no map, no handlers except one local factory), so every
 * counter, list and field copy of the clone is directly observable.
 */
public class EntityCloneTest {

	private EntityStorage storage;

	@Before
	public void setUp() {
		storage = TestSupport.bootstrapEngine();
	}

	@After
	public void tearDown() {
		storage = null;
	}

	private TagOptions tagOptions(String tag, Property... properties) {
		TagOptions options = new TagOptions(tag);
		for (Property property : properties) {
			options.lstPropertys.add(property);
		}
		return options;
	}

	private Entity prototype(String name) {
		Entity proto = new Entity(tagOptions("prototype", new Property("name", new Text(name))));
		return proto;
	}

	private Entity cloneOf(Entity prototype) {
		return new Entity(prototype);
	}

	@SuppressWarnings("unchecked")
	private <T extends Component> T onlyComponent(Entity entity, Class <T> type) {
		assertEquals("expected exactly one component", 1, entity.lstComponents.size());
		return type.cast(entity.lstComponents.get(0));
	}

	@Test
	public void prototypeTagMarksTheEntity() {
		Entity proto = new Entity(new TagOptions("prototype"));

		assertTrue(proto.isPrototype);
		assertEquals("", proto.name);
		assertEquals(0, proto.implementationsNum);
	}

	@Test
	public void anyOtherTagNameLeavesTheEntityPlain() {
		// Entity.java:61 matches the tag name against "prototype" exactly
		assertFalse(new Entity(new TagOptions("proto")).isPrototype);
		assertFalse(new Entity(new TagOptions("prototypes")).isPrototype);
		assertFalse(new Entity(new TagOptions("entity")).isPrototype);
		assertFalse(new Entity("prototype").isPrototype);
	}

	@Test
	public void entityTagOptionsFillNameAndSingleAndIgnoreTheRest() {
		TagOptions options = new TagOptions("prototype");
		options.lstPropertys.add(new Property("NAME", new Text("checker")));
		options.lstPropertys.add(new Property("single", new Numeral(1)));
		options.lstPropertys.add(new Property("unknown", new Text("x")));

		Entity entity = new Entity(options);

		// Property.isNamed is case insensitive (Property.java:20), unlike component tags
		assertEquals("checker", entity.name);
		assertTrue(entity.isSingle.value);
		assertTrue(entity.lstComponents.isEmpty());
	}

	@Test
	public void cloneNameIsThePrototypeNamePlusTheCounter() {
		Entity proto = prototype("checker");

		Entity first = cloneOf(proto);
		Entity second = cloneOf(proto);

		assertEquals("checker1", first.name);
		assertEquals("checker2", second.name);
		assertEquals(2, proto.implementationsNum);
		assertFalse(first.isPrototype);
		assertFalse(second.isPrototype);
		assertEquals(0, first.implementationsNum);
		assertNotSame(proto.lstComponents, first.lstComponents);
	}

	@Test
	public void anUnnamedPrototypeGivesCounterOnlyNames() {
		Entity proto = new Entity(new TagOptions("prototype"));

		assertEquals("1", cloneOf(proto).name);
		assertEquals("2", cloneOf(proto).name);
	}

	@Test
	public void pins_anyEntityCanActAsAPrototype() {
		// Entity.java:64-68 never checks prototype.isPrototype
		Entity plain = new Entity("plain");

		Entity child = cloneOf(plain);

		assertEquals("plain1", child.name);
		assertEquals(1, plain.implementationsNum);
		assertFalse(child.isPrototype);
	}

	@Test
	public void clonedComponentsAreNewInstancesOfTheSameClass() {
		Entity proto = prototype("checker");
		Plain source = new Plain();
		proto.addComponent(source);

		Entity clone = cloneOf(proto);

		assertEquals(1, clone.lstComponents.size());
		Component copy = clone.lstComponents.get(0);
		assertNotSame(source, copy);
		assertSame(Plain.class, copy.getClass());
		// createAutoClone assigns the master itself (Component.java:96-97)
		assertSame(clone, copy.entity);
		assertEquals("Plain", copy.getName());
		assertEquals(1, clone.lstComponents.size());
	}

	@Test
	public void typedListsOfTheCloneAreRepopulated() {
		Entity proto = prototype("checker");
		proto.addComponent(new Brain());
		proto.addComponent(new Body());
		proto.addComponent(new Art());
		proto.addComponent(new Tap());

		assertEquals(4, proto.lstComponents.size());
		assertEquals(1, proto.lstLogicals.size());

		Entity clone = cloneOf(proto);

		assertEquals(4, clone.lstComponents.size());
		assertEquals(1, clone.lstLogicals.size());
		assertEquals(1, clone.lstPhysicals.size());
		assertEquals(1, clone.lstGraphicals.size());
		assertEquals(1, clone.lstTouches.size());
		assertSame(Brain.class, clone.lstLogicals.get(0).getClass());
		assertNotSame(proto.lstLogicals.get(0), clone.lstLogicals.get(0));
		assertNotSame(proto.lstPhysicals.get(0), clone.lstPhysicals.get(0));
		assertNotSame(proto.lstGraphicals.get(0), clone.lstGraphicals.get(0));
		assertNotSame(proto.lstTouches.get(0), clone.lstTouches.get(0));
	}

	@Test
	public void dataFieldsBecomeNewDataObjectsWithCopiedValues() {
		Entity proto = prototype("checker");
		Plain source = new Plain();
		source.radius.value = 6;
		source.flag.value = false;
		source.label.value = "src";
		source.count.value = 11;
		proto.addComponent(source);

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);

		assertNotSame(source.radius, copy.radius);
		assertNotSame(source.flag, copy.flag);
		assertNotSame(source.label, copy.label);
		assertNotSame(source.count, copy.count);
		assertEquals(6f, copy.radius.value, 0f);
		assertFalse(copy.flag.value);
		assertEquals("src", copy.label.value);
		assertEquals(11, copy.count.value);

		copy.radius.value = 9;
		assertEquals(6f, source.radius.value, 0f);
	}

	@Test
	public void inheritedDataFieldsAreCopiedToo() {
		Entity proto = prototype("checker");
		Body source = new Body();
		source.isEnabled.value = false;
		source.movement.currentSpeed.value = 3.5;
		proto.addComponent(source);

		Body copy = onlyComponent(cloneOf(proto), Body.class);

		assertNotSame(source.position, copy.position);
		assertNotSame(source.movement, copy.movement);
		assertFalse(copy.isEnabled.value);
		assertEquals(3.5, copy.movement.currentSpeed.value, 0d);

		copy.position.setX(12);
		copy.movement.currentSpeed.value = 7;
		assertEquals(0d, source.position.getX(), 0d);
		assertEquals(3.5, source.movement.currentSpeed.value, 0d);
	}

	@Test
	public void clonedDataIsRenamedAfterTheFieldAndNotAfterThePrototype() {
		// Component.java:158-159: setData copies the prototype name, setName(field) wins
		Entity proto = prototype("checker");
		Plain source = new Plain();
		source.radius.setName("custom");
		proto.addComponent(source);

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);

		assertEquals("custom", source.radius.getName());
		assertEquals("radius", copy.radius.getName());
	}

	@Test
	public void pins_objectAndListFieldsAreSharedByReference() {
		// Component.java:129-141 copies every non private, non final reference field with
		// field.set, so the clone receives the very same objects as its prototype
		Entity proto = prototype("checker");
		Plain source = new Plain();
		source.tags.add("one");
		proto.addComponent(source);

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);

		assertSame(source.tags, copy.tags);
		assertSame(source.marker, copy.marker);

		copy.tags.add("two");
		assertEquals(2, source.tags.size());
		copy.marker.id = 42;
		assertEquals(42, source.marker.id);
	}

	@Test
	public void pins_finalAndPrivateFieldsAreNeverCopied() {
		Entity proto = prototype("checker");
		Plain source = new Plain();
		source.frozen.add("prototype only");
		source.secret = 99;
		source.mutable = 5;
		proto.addComponent(source);

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);

		// Component.java:129 skips final fields, Component.java:123 skips private ones
		assertNotSame(source.frozen, copy.frozen);
		assertTrue(copy.frozen.isEmpty());
		assertEquals(20, copy.secretValue());
		// primitives are copied by value
		assertEquals(5, copy.mutable);
	}

	@Test
	public void pins_cloneSharesTheBookkeepingListsOfItsPrototype() {
		// the ArrayLists declared by Component itself (Component.java:55-59) are ordinary
		// non final reference fields, so they are copied by reference as well
		Entity proto = prototype("checker");
		Plain source = new Plain();
		proto.addComponent(source);

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);

		assertSame(source.lstData, copy.lstData);
		assertSame(source.lstLinkedHandlers, copy.lstLinkedHandlers);
		assertSame(source.lstLinkedSubHandlers, copy.lstLinkedSubHandlers);
	}

	@Test
	public void pins_setPropertyDataOnACloneWritesThroughToThePrototype() {
		PlainHandler handler = new PlainHandler();
		Entity proto = prototype("checker");

		Plain source = (Plain) handler.createComponent(proto, new TagOptions(Plain.SHORT_CLASS_NAME));
		assertNotNull(source);
		// setContextToComponent subscribed every DataInterface field of Plain and Component
		assertEquals(5, source.lstData.size());

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);
		assertSame(source.lstData, copy.lstData);

		// the shared list still holds the prototype data objects
		copy.setOptions(tagOptions("plain", new Property("radius", new Fractional(99))));

		assertEquals(99f, source.radius.value, 0f);
		assertEquals(4f, copy.radius.value, 0f);
	}

	@Test
	public void pins_createCloneReplacesTheAutoCloneEntirely() {
		// Entity.java:69-76: a non null createClone result is taken as is. No field copy, no
		// entity assignment and no addComponent are done by the engine for that component
		Entity proto = prototype("checker");
		HandCloned source = new HandCloned();
		source.power.value = 42;
		source.tags.add("prototype only");
		proto.addComponent(source);

		Entity clone = cloneOf(proto);

		assertEquals(1, clone.lstComponents.size());
		HandCloned copy = (HandCloned) clone.lstComponents.get(0);
		assertNotSame(source, copy);
		assertEquals(1f, copy.power.value, 0f);
		// createAutoClone would have handed over the very same list
		assertNotSame(source.tags, copy.tags);
		assertNull(copy.entity);
	}

	@Test
	public void pins_createCloneThatSkipsAddComponentLosesTheComponent() {
		Entity proto = prototype("checker");
		proto.addComponent(new HandClonedDetached());

		Entity clone = cloneOf(proto);

		assertTrue(clone.lstComponents.isEmpty());
		assertTrue(clone.lstLogicals.isEmpty());
		assertEquals(1, proto.implementationsNum);
	}

	@Test
	public void pins_componentWithoutANoArgConstructorIsDroppedFromTheClone() {
		// Component.java:84-92: newInstance() throws, the stack trace is printed and the
		// clone stays null, so the entity silently loses that component
		Entity proto = prototype("checker");
		NoDefaultCtor source = new NoDefaultCtor(7);
		proto.addComponent(source);

		Entity clone = cloneOf(proto);

		assertEquals(1, proto.lstComponents.size());
		assertTrue(clone.lstComponents.isEmpty());
		// the implementation counter was increased before the copy even started
		assertEquals(1, proto.implementationsNum);
		assertNull(source.createAutoClone(new Entity("target")));
	}

	@Test
	public void pins_touchFingerNeverSurvivesAPrototypeClone() {
		// touches/screen/TouchFinger.java:31 declares TouchFinger(int) only, so a cloned
		// entity has no touch component at all
		Entity proto = prototype("cursor");
		TouchFinger finger = new TouchFinger(2);
		proto.addComponent(finger);
		assertEquals("InputTouchFinger", finger.getName());
		assertEquals(1, proto.lstTouches.size());

		Entity clone = cloneOf(proto);

		assertTrue(clone.lstTouches.isEmpty());
		assertTrue(clone.lstComponents.isEmpty());
	}

	@Test
	public void anEmptyPrototypeStillCountsAndProducesAnEmptyEntity() {
		Entity proto = prototype("nothing");

		Entity clone = cloneOf(proto);

		assertEquals("nothing1", clone.name);
		assertTrue(clone.lstComponents.isEmpty());
		assertTrue(clone.lstGraphicals.isEmpty());
		assertEquals(1, proto.implementationsNum);
	}

	@Test
	public void prototypeBindingStringIsInheritedAndReboundInTheClone() {
		// DataInterface.java:25: setData copies bindingString, setBinding then builds a fresh
		// Expression over the currently injected entity storage
		Entity target = new Entity("target");
		LogicalField field = new LogicalField();
		field.radius.value = 5;
		target.addComponent(field);
		storage.lstEntities.add(target);

		Entity proto = prototype("checker");
		Plain source = new Plain();
		source.radius.value = 7;
		source.radius.setBinding("$target.LogicalField.radius*2");
		proto.addComponent(source);

		Plain copy = onlyComponent(cloneOf(proto), Plain.class);
		assertEquals(7f, copy.radius.value, 0f);

		copy.radius.commit(null, copy.entity);

		// the binding, not the copied value, produced 5 * 2
		assertEquals(10f, copy.radius.value, 0f);
		assertEquals(7f, source.radius.value, 0f);
	}

	@Test
	public void pins_handlerCannotCreateAComponentWithoutShortClassName() {
		// ComponentsHandler.java:394-408: the whole creation block sits inside the try that
		// swallows NoSuchFieldException, so such a class can never be instantiated from a map
		PlainHandler handler = new PlainHandler();
		handler.addProcessingComponent((Class <Component>) Brain.class.asSubclass(Component.class));

		assertTrue(handler.canCreateThisComponent("Brain"));
		assertNull(handler.createComponent(new Entity("x"), new TagOptions("Brain")));
	}

	// ---------------------------------------------------------------- test fixtures

	/** Plain data holder with every field shape {@code setMatchingFields} distinguishes. */
	public static class Plain extends Component {

		public final static String SHORT_CLASS_NAME = "plain";

		public Fractional radius = new Fractional(4);
		public Bool flag = new Bool(true);
		public Text label = new Text("source");
		public Numeral count = new Numeral(0);

		public Marker marker = new Marker(1);
		public ArrayList <String> tags = new ArrayList <String>();
		public final ArrayList <String> frozen = new ArrayList <String>();

		public int mutable = 1;
		private int secret = 20;

		public int secretValue() {
			return secret;
		}
	}

	public static class Marker {

		public int id;

		public Marker(int id) {
			this.id = id;
		}
	}

	public static class Brain extends LogicalInterface { }

	public static class Body extends PhysicalInterface { }

	public static class Art extends OutputGraphicalInterface { }

	public static class Tap extends InputTouchInterface { }

	/** Overrides the clone hook and adds itself to the master, but copies nothing. */
	public static class HandCloned extends Component {

		public Fractional power = new Fractional(1);
		public ArrayList <String> tags = new ArrayList <String>();

		@Override
		public Component createClone(Entity master) {
			HandCloned clone = new HandCloned();
			clone.tags.add("clone made by hand");
			master.addComponent(clone);
			return clone;
		}
	}

	/** Overrides the clone hook and forgets to register the result anywhere. */
	public static class HandClonedDetached extends Component {

		@Override
		public Component createClone(Entity master) {
			return new HandClonedDetached();
		}
	}

	/** No no-arg constructor, so {@code createAutoClone} cannot reflect it. */
	public static class NoDefaultCtor extends Component {

		public NoDefaultCtor(int marker) { }
	}

	/** Factory used only to give a component a real, context filled {@code lstData}. */
	public static class PlainHandler extends ComponentsHandler {

		@SuppressWarnings("unchecked")
		public PlainHandler() {
			super(1, (Class <Component>) Plain.class.asSubclass(Component.class));
			getContexts();
		}
	}
}
