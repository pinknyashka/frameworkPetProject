package ru.framework2d.logic.grids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Entity;
import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Position3;
import ru.framework2d.data.Text;
import ru.framework2d.testsupport.TestSupport;

/**
 * Registration and data-transfer behaviour of HandlerGrids: grid layout, group cell
 * strings, item placement and the row-shift (leaderRow / respawnRow) paths.
 */
public class HandlerGridsTest {

	private HandlerGrids handler;

	@Before
	public void setUp() {

		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		handler = new HandlerGrids();
	}

	private LogicalGrid createGrid(Entity entity, String name, double size, int columns, int rows) {

		TagOptions options = new TagOptions("grid");
		options.lstPropertys.add(new Property("name", new Text(name)));
		options.lstPropertys.add(new Property("position:x", new Fractional(0)));
		options.lstPropertys.add(new Property("position:y", new Fractional(0)));
		options.lstPropertys.add(new Property("size:x", new Fractional((float) size)));
		options.lstPropertys.add(new Property("size:y", new Fractional((float) size)));
		options.lstPropertys.add(new Property("columnsNumber", new Numeral(columns)));
		options.lstPropertys.add(new Property("rowsNumber", new Numeral(rows)));

		return (LogicalGrid) handler.createComponent(entity, options);
	}

	private LogicalCellGroup createGroup(Entity entity, String name, String gridName, String itemName,
			String relativeCells, int leaderColumn, int leaderRow) {

		TagOptions options = new TagOptions("cell_group");
		options.lstPropertys.add(new Property("name", new Text(name)));
		options.lstPropertys.add(new Property("gridName", new Text(gridName)));
		options.lstPropertys.add(new Property("itemName", new Text(itemName)));
		options.lstPropertys.add(new Property("relativeCells", new Text(relativeCells)));
		options.lstPropertys.add(new Property("leaderColumn", new Numeral(leaderColumn)));
		options.lstPropertys.add(new Property("leaderRow", new Numeral(leaderRow)));

		return (LogicalCellGroup) handler.createComponent(entity, options);
	}

	private LogicalCellItem createItem(Entity entity, String name) {

		TagOptions options = new TagOptions("item");
		options.lstPropertys.add(new Property("name", new Text(name)));

		return (LogicalCellItem) handler.createComponent(entity, options);
	}

	private static Numeral numeralInContextOf(Numeral source, int value) {

		Numeral data = new Numeral(value);
		data.setName(source.getName());
		data.setContext(source.getContext());
		return data;
	}

	// ------------------------------------------------------------------- creation

	@Test
	public void handlerAnswersGridItemAndCellGroupTags() {

		assertTrue(handler.canCreateThisComponent("grid"));
		assertTrue(handler.canCreateThisComponent("LogicalGrid"));
		assertTrue(handler.canCreateThisComponent("item"));
		assertTrue(handler.canCreateThisComponent("cell_group"));
		assertTrue(handler.canCreateThisComponent("LogicalCellGroup"));

		// tag matching is contentEquals -> case sensitive
		assertFalse(handler.canCreateThisComponent("Grid"));
		assertFalse(handler.canCreateThisComponent("cellgroup"));
	}

	@Test
	public void gridRegistrationLaysOutItsCells() {

		LogicalGrid grid = createGrid(new Entity("grid1"), "board", 100, 8, 8);

		assertEquals(1, handler.lstGrids.size());
		assertSame(grid, handler.lstGrids.get(0));
		assertEquals(8, grid.cells.length);
		assertEquals(8, grid.cells[0].length);
		assertNotNull(grid.cells[7][7]);

		// cellSize 12.5, first centre = -50 + 6.25
		assertEquals(-43.75, grid.cells[0][0].position.getX(), 0.0001);
		assertEquals(43.75, grid.cells[7][7].position.getY(), 0.0001);
	}

	// --------------------------------------------------------------- grid transfers

	@Test
	public void pins_gridPositionTransferDoesNotLayTheCellsOutAgain() {

		Entity entity = new Entity("grid1");
		LogicalGrid grid = createGrid(entity, "board", 8, 8, 8);

		grid.position.x.value = 10;
		grid.position.y.value = 5;
		assertTrue(handler.transferData(entity, grid.position));

		assertEquals(10.0, grid.position.getX(), 0.0001);
		// :200-204 sets the grid position but never calls setCellsParams(), so the
		// cell centres keep their old world coordinates
		assertEquals(-3.5, grid.cells[0][0].position.getX(), 0.0001);
	}

	@Test(expected = NullPointerException.class)
	public void pins_transferDataRequiresDataThatAlreadyGotAContext() {

		Entity entity = new Entity("grid1");
		createGrid(entity, "board", 8, 8, 8);

		handler.transferData(entity, new Position3());
	}

	@Test
	public void respawnRowTransferShiftsTheLocalPositionAndRebuildsTheCells() {

		Entity entity = new Entity("grid1");
		LogicalGrid grid = createGrid(entity, "board", 8, 8, 8);

		assertTrue(handler.transferData(entity, numeralInContextOf(grid.respawnRow, 2)));

		assertEquals(2, grid.respawnRow.value);
		assertEquals(-2.0, grid.localPos.y.value, 0.0001);
		assertEquals(-5.5, grid.cells[0][0].position.getY(), 0.0001);
	}

	@Test(expected = NullPointerException.class)
	public void pins_respawnRowTransferThrowsWhenCellsWereNeverBuilt() {

		Entity entity = new Entity("grid1");
		LogicalGrid grid = createGrid(entity, "board", 0, 3, 3);

		// setCellsParams is skipped for a zero-sized grid, while the commit loop still
		// walks the whole matrix (:226-233)
		handler.transferData(entity, numeralInContextOf(grid.respawnRow, 1));
	}

	// ------------------------------------------------------------------ groups

	@Test
	public void groupBindsToAGridByNameAndParsesItsCellString() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		LogicalCellGroup group = createGroup(new Entity("group1"), "white_row", "board", "checker",
				"0,0,1,0,2,0", 0, 0);

		assertEquals(1, handler.lstGroups.size());
		assertSame(handler.lstGrids.get(0), group.grid);
		assertEquals(3, group.lstCells.size());
		assertSame(handler.lstGrids.get(0).cells[0][0], group.lstCells.get(0));
		assertSame(handler.lstGrids.get(0).cells[2][0], group.lstCells.get(2));
	}

	@Test
	public void groupCoordinatesAreRelativeToLeaderColumnAndLeaderRow() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		LogicalCellGroup group = createGroup(new Entity("group1"), "black_row", "board", "checker",
				"0,-1,1,0", 0, 7);

		assertEquals(2, group.lstCells.size());
		assertEquals(0, group.lstCells.get(0).column);
		assertEquals(6, group.lstCells.get(0).row);
		assertEquals(1, group.lstCells.get(1).column);
		assertEquals(7, group.lstCells.get(1).row);
	}

	@Test
	public void pins_shortTailOfTheCellStringIsIgnored() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		LogicalCellGroup group = createGroup(new Entity("group1"), "g", "board", "checker",
				"0,0,1", 0, 0);

		// the loop needs more than two characters left (:83), so the dangling "1" is lost
		assertEquals(1, group.lstCells.size());
	}

	@Test
	public void pins_duplicatedCoordinatesPutTheSameCellInTwice() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		LogicalCellGroup group = createGroup(new Entity("group1"), "g", "board", "checker",
				"0,0,0,0", 0, 0);

		assertEquals(2, group.lstCells.size());
		assertSame(group.lstCells.get(0), group.lstCells.get(1));
	}

	@Test(expected = ArrayIndexOutOfBoundsException.class)
	public void pins_cellCoordinatesAreNotRangeChecked() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		createGroup(new Entity("group1"), "g", "board", "checker", "8,0", 0, 0);
	}

	@Test(expected = NumberFormatException.class)
	public void pins_malformedCellStringThrowsAtRegistration() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		createGroup(new Entity("group1"), "g", "board", "checker", ",0,0", 0, 0);
	}

	@Test
	public void groupWithoutGridNameIsRegisteredButGetsNoCells() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		LogicalCellGroup group = createGroup(new Entity("group1"), "g", "", "checker", "0,0", 0, 0);

		assertEquals(1, handler.lstGroups.size());
		assertNull(group.grid);
		assertTrue(group.lstCells.isEmpty());
	}

	@Test
	public void pins_groupIsBoundToItsGridOnlyAtRegistrationTime() {

		LogicalCellGroup group = createGroup(new Entity("group1"), "g", "board", "checker", "0,0", 0, 0);
		createGrid(new Entity("grid1"), "board", 8, 8, 8);

		// the grid lookup only runs when the group is registered (:75-77)
		assertNull(group.grid);
		assertTrue(group.lstCells.isEmpty());
		assertEquals(1, handler.lstGrids.size());
	}

	@Test
	public void pins_groupSkipsNullCellsOfAnUnlaidOutGrid() {

		createGrid(new Entity("grid1"), "board", 0, 3, 3);
		LogicalCellGroup group = createGroup(new Entity("group1"), "g", "board", "checker", "0,0,1,1", 0, 0);

		assertSame(handler.lstGrids.get(0), group.grid);
		assertTrue(group.lstCells.isEmpty());
	}

	// -------------------------------------------------------------------- items

	@Test
	public void itemGoesToTheFirstEmptyCellOfItsGroup() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		createGroup(new Entity("group1"), "g", "board", "checker", "0,0,1,0", 0, 0);

		LogicalCellItem item = createItem(new Entity("checker1"), "checker");

		CellGrid cell = handler.lstGrids.get(0).cells[0][0];
		assertEquals(1, handler.lstItems.size());
		assertSame(cell, item.cell);
		assertTrue(cell.lstItems.contains(item));
		assertEquals(-3.5, item.cellPosition.getX(), 0.0001);
		assertEquals(-3.5, item.position.getY(), 0.0001);
	}

	@Test
	public void secondItemTakesTheNextFreeCellOfTheGroup() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		createGroup(new Entity("group1"), "g", "board", "checker", "0,0,1,0", 0, 0);

		createItem(new Entity("checker1"), "checker");
		LogicalCellItem second = createItem(new Entity("checker2"), "checker");

		assertSame(handler.lstGrids.get(0).cells[1][0], second.cell);
		assertEquals(-2.5, second.cellPosition.getX(), 0.0001);
	}

	@Test
	public void itemWithoutMatchingGroupNameGetsNoCell() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		createGroup(new Entity("group1"), "g", "board", "other", "0,0", 0, 0);

		LogicalCellItem item = createItem(new Entity("checker1"), "checker");

		assertEquals(1, handler.lstItems.size());
		assertNull(item.cell);
		assertTrue(handler.lstGroups.get(0).lstItems.isEmpty());
	}

	@Test
	public void pins_itemGridReferenceIsNeverAssigned() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		createGroup(new Entity("group1"), "g", "board", "checker", "0,0", 0, 0);
		createGroup(new Entity("group2"), "g2", "board", "other", "0,0", 0, 0);

		LogicalCellItem item = createItem(new Entity("checker1"), "checker");
		item.gridName.value = "board";

		// LogicalCellItem.grid is declared but never written anywhere, so the guard at
		// :161-163 always fires and the <item gridName=...> attribute stays decorative
		assertNull(item.grid);
	}

	// ----------------------------------------------------------- group row shifts

	@Test
	public void leaderRowTransferMovesGroupCellsAndTheirItems() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		Entity groupEntity = new Entity("group1");
		LogicalCellGroup group = createGroup(groupEntity, "g", "board", "checker", "0,0,1,0", 0, 0);
		LogicalCellItem item = createItem(new Entity("checker1"), "checker");

		CellGrid oldCell = group.lstCells.get(0);
		CellGrid targetCell = handler.lstGrids.get(0).cells[0][3];

		assertTrue(handler.transferData(groupEntity, numeralInContextOf(group.leaderRow, 3)));

		assertEquals(3, group.leaderRow.value);
		assertSame(targetCell, group.lstCells.get(0));
		assertTrue(oldCell.lstItems.isEmpty());
		assertTrue(targetCell.lstItems.contains(item));
		assertEquals(-0.5, item.cellPosition.getY(), 0.0001);
		assertEquals(-0.5, item.position.getY(), 0.0001);
	}

	@Test
	public void pins_leaderRowShiftLeavesLeaderColumnAndCellStringStale() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		Entity groupEntity = new Entity("group1");
		LogicalCellGroup group = createGroup(groupEntity, "g", "board", "checker", "0,0,1,0", 0, 0);

		handler.transferData(groupEntity, numeralInContextOf(group.leaderRow, 3));

		// only the cell references and leaderRow move: leaderColumn and relativeCells
		// are never re-applied, so they no longer describe the group (:250-264)
		assertEquals(3, group.lstCells.get(0).row);
		assertEquals(0, group.leaderColumn.value);
		assertEquals("0,0,1,0", group.relativeCells.value);
	}

	@Test
	public void pins_leaderRowTransferOfAnUnboundGroupThrows() {

		Entity groupEntity = new Entity("group1");
		LogicalCellGroup group = createGroup(groupEntity, "g", "", "checker", "0,0", 0, 0);

		try {
			handler.transferData(groupEntity, numeralInContextOf(group.leaderRow, 3));
		} catch (NullPointerException expected) {
			// the log line dereferences group.grid before any null check (:242)
			assertNull(group.grid);
			return;
		}

		fail("expected NullPointerException for a group without a grid");
	}

	@Test
	public void nonNumericDataReachesTheGroupWithoutChangingIt() {

		createGrid(new Entity("grid1"), "board", 8, 8, 8);
		Entity groupEntity = new Entity("group1");
		LogicalCellGroup group = createGroup(groupEntity, "g", "board", "checker", "0,0", 0, 0);

		// group.name is a contexted Text, so transferData can log it (:176-177)
		assertFalse(handler.transferData(groupEntity, group.name));
		assertEquals(0, group.leaderRow.value);
	}

	@Test
	public void handlerNeverSchedulesOwnWork() {

		assertFalse(handler.startWork(0));
		assertFalse(handler.haveWork());
	}
}
