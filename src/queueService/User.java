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

public class User {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public boolean isWaiting;
	
	public User(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.isWaiting = true;
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {		
		moveTowards(pointToGo());
	}
	
	public void moveTowards(GridPoint pt) {
		if(!pt.equals(myLocation())) {
			NdPoint myPoint  = space.getLocation(this);
			NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
			double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
			space.moveByVector(this, 1, angle, 0);
			myPoint = space.getLocation(this);
			grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
		}
	}
	
	private GridPoint pointToGo() {
		
		GridPoint pt = new GridPoint();
		
		if(isWaiting){
			pt = new GridPoint(25,25 + getPositionIntoWaitingQueue());
		}else{
			pt = new GridPoint(25,49);
		}
		
		return pt;
	}
	
	private int getPositionIntoWaitingQueue(){
		return Administration.waitingQueue.indexOf(this);
	}
	
	private GridPoint myLocation() {
		return grid.getLocation(this);
	}

}
