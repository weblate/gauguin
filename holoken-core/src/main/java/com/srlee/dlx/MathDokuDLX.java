package com.srlee.dlx;

import com.holokenmod.creation.cage.GridSingleCageCreator;
import com.holokenmod.grid.Grid;
import com.holokenmod.grid.GridCage;
import com.holokenmod.options.CurrentGameOptionsVariant;
import com.holokenmod.options.DigitSetting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class MathDokuDLX extends DLX {
	private final static Logger LOGGER = LoggerFactory.getLogger(MathDokuDLX.class);
	
	public MathDokuDLX(final Grid grid) {
		
		// Number of columns = number of constraints =
		//		BOARD * BOARD (for columns) +
		//		BOARD * BOARD (for rows)	+
		//		Num cages (each cage has to be filled once and only once)
		// Number of rows = number of "moves" =
		//		Sum of all the possible cage combinations
		// Number of nodes = sum of each move:
		//      num_cells column constraints +
		//      num_cells row constraints +
		//      1 (cage constraint)
		int total_nodes = 0;
		
		final Collection<GridSingleCageCreator> creators = new ArrayList<>();
		
		for (final GridCage cage : grid.getCages()) {
			creators.add(new GridSingleCageCreator(grid, cage));
		}
		
		for (final GridSingleCageCreator creator : creators) {
			total_nodes += creator.getPossibleNums().size() * (2 * creator.getNumberOfCells() + 1);
		}
		init(2 * grid.getGridSize().getSurfaceArea() + creators.size(), total_nodes);
		
		int currentCombination = 0;
		
		DigitSetting digitSetting = CurrentGameOptionsVariant.getInstance().getDigitSetting();
		
		for (final GridSingleCageCreator creator : creators) {
			for (final int[] possibleCageCombination : creator.getPossibleNums()) {
				//LOGGER.info("cage " + creator.getCage() + " - " + Arrays.toString(onemove));
				
				for (int i = 0; i < possibleCageCombination.length; i++) {
					int indexOfDigit = digitSetting.indexOf(possibleCageCombination[i]);
					
					// Column constraint
					addNode(grid.getGridSize().getWidth() * indexOfDigit + creator.getCell(i).getColumn() + 1,
							currentCombination);
					
					// Row constraint
					addNode(grid.getGridSize().getSurfaceArea() + grid.getGridSize().getWidth() * indexOfDigit + creator.getCell(i).getRow() + 1,
							currentCombination);
				}
				
				// Cage constraint
				addNode(2 * grid.getGridSize().getSurfaceArea() + creator.getId() + 1,
						currentCombination);

				currentCombination++;
			}
		}
	}
}