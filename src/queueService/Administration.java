package queueService;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

public class Administration {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public static List<User> waitingQueue = new ArrayList<>();
	
	public Administration(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 30, interval = 4)
	public void getFirstUserInTheList() {
		User firstUser = waitingQueue.get(0);
		waitingQueue.remove(0);
		firstUser.isWaiting = false;
	}
	
	
	@ScheduledMethod(start = 10, interval = 10)
	public void addUser() {
		User user = new User(space, grid);
		
		Context<Object> context = ContextUtils.getContext(this);

		context.add(user);
		
		waitingQueue.add(user);
		
		space.moveTo(user, 25, 0);
		grid.moveTo(user, 25, 0);
	}
	
}
