package queueService;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
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

public class User{
	
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
		this.endOfWaiting = 0;
	}
	
	public User(){}
	
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {	
		System.out.println(this.toString());
		moveTowards(pointToGo());
		if(this.usedGuichet != null){
			if(this.numberOfTickOfService <= RunEnvironment.getInstance().getCurrentSchedule().getTickCount() - this.endOfWaiting){
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
	
	/*private GridPoint pointToGo() {
		
		GridPoint pt = new GridPoint();
		
		if(isWaiting){
			int x=35;
			int y=60;
			if((60+(-1)*getPositionIntoWaitingQueue())<10){
				y=110;
				x=45;
			}
			pt = new GridPoint(x,y+(-1)*getPositionIntoWaitingQueue());
		}else if(this.usedGuichet != null){
			pt = new GridPoint(usedGuichet.myLocation().getX(), usedGuichet.myLocation().getY());
		}else{
			pt = new GridPoint(1,1);
		}
		
		return pt;
	}*/
	private GridPoint pointToGo() {
		
		GridPoint pt = new GridPoint();
		
		if(isWaiting){
			int nbrow = getPositionIntoWaitingQueue()/50;
			int y = getPositionIntoWaitingQueue()%50;
			pt = new GridPoint(5+5*nbrow,60+(-1)*y);
		}else if(this.usedGuichet != null){
			pt = new GridPoint(usedGuichet.myLocation().getX(), usedGuichet.myLocation().getY());
		}else{
			pt = new GridPoint(69,1); 
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
		result.append("# User arrival : " + this.arrivalTick + "\n");
		result.append("# User is waiting : " + this.isWaiting + "\n");
		result.append("# Duration of service : " + this.numberOfTickOfService + "\n");
		result.append("# End of waiting : " + this.endOfWaiting + "\n");
		result.append("############################## \n");
		
		return result.toString();
	}
	
	public double getWaitingTime(){
		return (this.endOfWaiting == 0 ? 0 : (this.endOfWaiting - this.arrivalTick));
	}
	
	public double getNumberOfTickOfService(){
		return (this.numberOfTickOfService);
	}
	
	public double getArrivalTick(){
		return (this.arrivalTick);
	}

}
