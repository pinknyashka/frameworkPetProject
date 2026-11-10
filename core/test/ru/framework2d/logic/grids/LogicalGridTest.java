package ru.framework2d.logic.grids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import ru.framework2d.core.Property;
import ru.framework2d.core.TagOptions;
import ru.framework2d.data.Fractional;
import ru.framework2d.data.Numeral;
import ru.framework2d.data.Text;
import ru.framework2d.testsupport.TestSupport;

/**
 * Geometry and attribute handling of LogicalGrid / CellGrid.
 */
public class LogicalGridTest {

	private HandlerGrids handler;

	@Before
	public void setUp() {

		TestSupport.bootstrapEngine();
		TestSupport.resetIntervalPhysics();
		handler = new HandlerGrids();
	}

	private static LogicalGrid bareGrid(double centerX, double centerY, double sizeX, double sizeY,
			int columns, int rows) {

		LogicalGrid grid = new LogicalGrid();
		grid.setPropertyData(new Property("columnsNumber", new Numeral(columns)));
		grid.setPropertyData(new Property("rowsNumber", new Numeral(rows)));
		grid.position.x.value = centerX;
		grid.position.y.value = centerY;
		grid.size.set(sizeX, sizeY);
		return grid;
	}

	@Test
	public void defaultGridIsOneByOne() {

		LogicalGrid grid = new LogicalGrid();

		assertEquals(1, grid.columnsNumber.value);
		assertEquals(1, grid.rowsNumber.value);
		assertEquals(1, grid.cells.length);
		assertEquals(1, grid.cells[0].length);
		assertNull(grid.cells[0][0]);
	}

	@Test
	public void cellCountPropertiesReallocateTheCellMatrix() {

		LogicalGrid grid = new LogicalGrid();

		grid.setPropertyData(new Property("columnsNumber", new Numeral(3)));
		assertEquals(3, grid.cells.length);
		assertEquals(1, grid.cells[0].length);

		grid.setPropertyData(new Property("rowsNumber", new Numeral(2)));
		assertEquals(3, grid.cells.length);
		assertEquals(2, grid.cells[0].length);

		// the numbers are read through toString(), so any numeric data type works (:50, :57)
		grid.setPropertyData(new Property("columnsNumber", new Fractional(4.5f)));
		assertEquals(4, grid.columnsNumber.value);
	}

	@Test
	public void pins_changingCellCountsDiscardsAlreadyBuiltCells() {

		LogicalGrid grid = bareGrid(0, 0, 8, 8, 2, 2);
		grid.setCellsParams();

		CellGrid built = grid.cells[0][0];
		assertNotNull(built);
		assertEquals(0, built.column);

		grid.setPropertyData(new Property("columnsNumber", new Numeral(4)));

		// a fresh matrix is allocated, so items keep dangling cell references
		assertNull(grid.cells[0][0]);
		assertEquals(0, built.column);
	}

	@Test
	public void pins_detachedGridIgnoresEveryAttributeButTheCellCounts() {

		LogicalGrid grid = new LogicalGrid();
		grid.setPropertyData(new Property("size:x", new Fractional(80)));
		grid.setPropertyData(new Property("name", new Text("board")));
		grid.setPropertyData(new Property("respawnRow", new Numeral(3)));

		// LogicalGrid has no explicit branch for these and lstData is still empty
		assertEquals(0.0, grid.size.getX(), 0.0001);
		assertEquals("", grid.name.value);
		assertEquals(0, grid.respawnRow.value);
	}

	@Test
	public void setContextToComponentMakesTheOtherAttributesAssignable() {

		LogicalGrid grid = new LogicalGrid();
		handler.setContextToComponent(grid);

		TagOptions options = new TagOptions("grid");
		options.lstPropertys.add(new Property("name", new Text("board")));
		options.lstPropertys.add(new Property("size:x", new Fractional(80)));
		options.lstPropertys.add(new Property("size:y", new Fractional(80)));
		options.lstPropertys.add(new Property("respawnRow", new Numeral(3)));
		grid.setOptions(options);

		assertEquals("board", grid.name.value);
		assertEquals(80.0, grid.size.getX(), 0.0001);
		assertEquals(3, grid.respawnRow.value);
	}

	@Test
	public void cellCentresAreLaidOutAroundTheGridPosition() {

		LogicalGrid grid = bareGrid(0, 0, 8, 8, 8, 8);
		grid.setCellsParams();

		assertEquals(1.0f, grid.cells[0][0].width, 0.0001f);
		assertEquals(1.0f, grid.cells[0][0].height, 0.0001f);

		assertEquals(-3.5, grid.cells[0][0].position.getX(), 0.0001);
		assertEquals(-3.5, grid.cells[0][0].position.getY(), 0.0001);
		assertEquals(3.5, grid.cells[7][7].position.getX(), 0.0001);
		assertEquals(3.5, grid.cells[7][7].position.getY(), 0.0001);

		LogicalGrid moved = bareGrid(10, 0, 8, 8, 8, 8);
		moved.setCellsParams();
		assertEquals(6.5, moved.cells[0][0].position.getX(), 0.0001);
	}

	@Test
	public void cellsAreAddressedAsColumnThenRow() {

		LogicalGrid grid = bareGrid(0, 0, 8, 4, 8, 4);
		grid.setCellsParams();

		assertEquals(2, grid.cells[2][3].column);
		assertEquals(3, grid.cells[2][3].row);
		assertEquals(1.0f, grid.cells[0][0].width, 0.0001f);
		assertEquals(1.0f, grid.cells[0][0].height, 0.0001f);
	}

	@Test
	public void pins_zeroSizedGridLeavesEveryCellNull() {

		LogicalGrid grid = bareGrid(0, 0, 0, 8, 3, 3);
		grid.setCellsParams();

		for (int c = 0; c < 3; c++) {
			for (int r = 0; r < 3; r++) {
				assertNull(grid.cells[c][r]);
			}
		}
	}

	@Test
	public void pins_layoutIsNotRefreshedByPropertyChangesAlone() {

		LogicalGrid grid = bareGrid(0, 0, 8, 8, 8, 8);
		grid.setCellsParams();

		double before = grid.cells[0][0].position.getX();
		grid.size.set(16, 16);
		assertEquals(before, grid.cells[0][0].position.getX(), 0.0001);

		grid.setCellsParams();
		assertEquals(-7.0, grid.cells[0][0].position.getX(), 0.0001);
	}

	@Test
	public void localOffsetIsRotatedByTheGridAlpha() {

		LogicalGrid grid = bareGrid(0, 0, 8, 8, 8, 8);
		grid.localPos.x.value = 1;
		grid.position.setAlpha(Math.PI / 2);
		grid.setCellsParams();

		// toLocal (1,0) rotated by alpha becomes (0,1); toCell (-3.5,-3.5) becomes (3.5,-3.5)
		assertEquals(3.5, grid.cells[0][0].position.getX(), 0.0001);
		assertEquals(-2.5, grid.cells[0][0].position.getY(), 0.0001);
	}

	@Test
	public void cellAlphaIsTheSumOfGridAndLocalAlpha() {

		LogicalGrid grid = bareGrid(0, 0, 8, 8, 8, 8);
		grid.position.setAlpha(0.25);
		grid.localPos.setAlpha(0.5);
		grid.setCellsParams();

		assertEquals(0.75, grid.cells[3][4].position.getAlpha(), 0.0001);
	}

	@Test
	public void pins_cellPositionsIgnoreTheGridZCoordinate() {

		LogicalGrid grid = bareGrid(0, 0, 8, 8, 4, 4);
		grid.position.z.value = 7;
		grid.setCellsParams();

		// LogicalGrid:99-100 uses the two-argument position setter, so the cell keeps
		// its own z instead of inheriting the grid's
		assertEquals(0.0, grid.cells[0][0].position.z.value, 0.0001);
	}
}
