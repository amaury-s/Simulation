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
	
	public double arrivalTick;
	public double endOfWaiting;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public boolean isWaiting;
	public Guichet usedGuichet;
	public int numberOfTickOfService;
	
	public User(ContinuousSpace<Object> space, Grid<Object> grid, int pNumberOfTickOfService, double pArrivalTick) {
		this.arrivalTick = pArrivalTick;
		this.numberOfTickOfService = pNumberOfTickOfService;
		this.isWaiting = true;
		this.space = space;
		this.grid = grid;
		this.usedGuichet = null;
	}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {	
		System.out.println(this.toString());
		moveTowards(pointToGo());
		if(this.usedGuichet != null){
			if(--this.numberOfTickOfService == 0){
				this.usedGuichet.isFree = true;
				this.usedGuichet = null;
			}
		}
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
			pt = new GridPoint(25,25 + (-1)*getPositionIntoWaitingQueue());
		}else if(this.usedGuichet != null){
			pt = new GridPoint(usedGuichet.myLocation().getX(), usedGuichet.myLocation().getY());
		}else{
			pt = new GridPoint(1,1);
		}
		
		return pt;
	}
	
	private int getPositionIntoWaitingQueue(){
		return Administration.waitingQueue.indexOf(this);
	}
	
	private GridPoint myLocation() {
		return grid.getLocation(this);
	}
	
	public String toString(){
		StringBuilder result = new StringBuilder();
		
		result.append("############################## \n");
		result.append("# User id : " + this.arrivalTick + "\n");
		result.append("# User is waiting : " + this.isWaiting + "\n");
		result.append("# Duration of service : " + this.numberOfTickOfService + "\n");
		result.append("# End of waiting : " + this.endOfWaiting + "\n");
		result.append("############################## \n");
		
		return result.toString();
	}

}
