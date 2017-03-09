package queueService;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

public class Administration {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	
	public Administration(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	
	@ScheduledMethod(start = 10, interval = 5)
	public void addUser() {
		System.out.println("User Add");
		User user = new User(space, grid);
		
		Context<Object> context = ContextUtils.getContext(this);

		context.add(user);
		
		space.moveTo(user, 30, 30);
		grid.moveTo(user, 30, 30);
	}
}
