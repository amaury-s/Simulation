package queueService;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;
import java.util.concurrent.ThreadLocalRandom;

public class Administration {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public static List<User> waitingQueue = new ArrayList<>();
	public static List<Guichet> listOfGuichet = new ArrayList<>();
	
	public Administration(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 20, interval = 1)
	public void step() {
		
		List<Guichet> listOfFreeGuichet = getFreeGuichets();
		
		for(Guichet aFreeGuichet: listOfFreeGuichet){
			if (waitingQueue.get(0) != null){
				User firstUser = waitingQueue.get(0);
				waitingQueue.remove(0);
				firstUser.usedGuichet = aFreeGuichet;
				firstUser.isWaiting = false;
				aFreeGuichet.isFree = false;
				firstUser.endOfWaiting = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			}
		}
		
	}
	
	@ScheduledMethod(start = 10, interval = 10)
	public void addUser() {
		User user = new User(space, grid, ThreadLocalRandom.current().nextInt(10, 20 + 1), RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
		
		Context<Object> context = ContextUtils.getContext(this);

		context.add(user);
		
		waitingQueue.add(user);
		
		space.moveTo(user, 25, 0);
		grid.moveTo(user, 25, 0);
	}
	
	private List<Guichet> getFreeGuichets(){
		List<Guichet> listOfGuichet = new ArrayList<>();
		
		for(Guichet aGuichet: this.listOfGuichet){
			if(aGuichet.isFree){
				listOfGuichet.add(aGuichet);
			}
		}
		
		return listOfGuichet;
	}
	
}
