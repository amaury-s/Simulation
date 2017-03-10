package queueService;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class Guichet {
	
	public ContinuousSpace<Object> space;
	public Grid<Object> grid;
	public boolean isFree;
	
	public Guichet(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.isFree = true;
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {		
		//System.out.println(this.myLocation());
	}
	
	public GridPoint myLocation() {
		return grid.getLocation(this);
	}

}
