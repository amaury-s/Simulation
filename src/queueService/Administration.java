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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.sql.Time;

public class Administration {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public static List<User> waitingQueue = new ArrayList<>();
	public static List<Guichet> listOfGuichet = new ArrayList<>();
	public final static int kindOfQueueSorting = (Integer) RunEnvironment.getInstance().getParameters().getValue("kind_of_queue_sorting");
	public int c=0;
	
	
	public Administration(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}
	
	@ScheduledMethod(start = 20, interval = 1)
	public void step() {
		List<Guichet> listOfFreeGuichet = getFreeGuichets();
		
		for(Guichet aFreeGuichet: listOfFreeGuichet){
			if (!waitingQueue.isEmpty()){
				User firstUser = new User();
				switch (kindOfQueueSorting){
	            case 1:
	            	firstUser = waitingQueue.get(0);
	            	break;
	            case 2:
	            	firstUser = waitingQueue.get (FindNextUser());
	            	break;
				}
				waitingQueue.remove(firstUser);
				firstUser.usedGuichet = aFreeGuichet;
				firstUser.isWaiting = false;
				aFreeGuichet.isFree = false;
				firstUser.endOfWaiting = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
			}
		}
	}
	
	public int FindNextUser(){
		int min = 0;
		int i = 0;
		//User min = waitingQueue.get(0);
		for(User user:waitingQueue){
			if(waitingQueue.get(min).numberOfTickOfService > waitingQueue.get(i).numberOfTickOfService){
				min=i;
			}
			i++;
		}
		return min;
	}
	
	@ScheduledMethod(start = 0, interval = 1)
	public void addUser() {
		Random random = new Random();
		if (AdministrationBuilder.commingTicks.contains(RunEnvironment.getInstance().getCurrentSchedule().getTickCount())){
			//System.out.println(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
			// correspond entre 10 et 20 minutes en secondes x10
	    	User user = new User(space, grid, 300 + random.nextInt(1200-300+ 1), RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
	    	//ThreadLocalRandom.current().
	    	c+=1;
	    	//System.out.println(c);
	    	
	    	Context<Object> context = ContextUtils.getContext(this);
	    	
	    	context.add(user);
			
			waitingQueue.add(user);
					
			space.moveTo(user, 25, 0);
			grid.moveTo(user, 25, 0);
		}    	
    	
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


	

