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
		User min = waitingQueue.get(0);
		for(User user:waitingQueue){
			if(min.numberOfTickOfService > user.numberOfTickOfService){
				min.numberOfTickOfService=user.numberOfTickOfService;
			}
		}
		return waitingQueue.indexOf(min);
	}
	
	@ScheduledMethod(start = 10, interval = 1)
	public void addUser() {

			
		Double[][] affMatrix = getAffMatrix();
    	int[][] comingMatrix=getPeopleComingPerHourPerDay(affMatrix);
    	List<Double> ticks=new ArrayList<>();
    	ticks=getListOfArrivalTicks(comingMatrix,"tuesday");
    	
    	/**** là je ne vois comment faire : comment recup les éléments de ticks
    	 * sachant que c'est une scheduled method donc on peut pas faire de boucle **/
    	User user = new User(space, grid, ThreadLocalRandom.current().nextInt(10, 20 + 1),ticks.get(0)); //ce sera pas get(0) evidemment
    	
    	Context<Object> context = ContextUtils.getContext(this);

		if(RunEnvironment.getInstance().getCurrentSchedule().getTickCount() <= 700){
			
			
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
	
	/****************************************************************************************************/
		
	public static Double getRandArrivingTime(Double beginning, Double end){
		final Random random = new Random();
		Double arrivingTimeInMs=((end-beginning+1)+beginning)*random.nextDouble();
		return arrivingTimeInMs;
	}
			
	/* returns a matrix of percentages [day][opening hours] */
	public static Double[][] getAffMatrix(){
	/* this fct generates a matrix giving a percentage for each hour of each day for caf paris 15*/
	/* to understand my idea:
	 *   from figures gathered on the CAF website we can find an average of people coming per day 
	 *   -->around 2400 for caf paris 15
	 *   with the percentages apply to that nb for each hour we can get random nb of people coming per hour per day
	 *   we don't care if the sum for each day is > 100% bc the nb of people coming has to fluctuate 
	 */
				
						
		final Random random = new Random();
						
		Double weak=0.08; //weak affluence, perc. of total nb of people coming per day in pondered average
		Double moderate=0.15; //moderate aff
		Double strong=0.30; //strong aff
		Double delta=0.1;
		Double w=1.0;
		Double m=2.0;
		Double s=3.0;
			
		/*matrix based on a schedule from caf website giving affluence */
			
		Double[][] aff=
			{
		        { m, m, s, s, m, m, s, s } , // tab[day][hour] 
		        { w, w, m, m, m, m, w, w },    //as we take caf paris 15 as example, only 4 opening days
		        { w, w, w, w, w, w, m, m },   //monday tuesday thursday friday, from 9am to 5pm 
		        { m, m, m, m, m, m, s, s },
	        };
				
		for (int i=0;i<aff.length;i++){
			for (int j=0;j<aff[i].length;j++){
								
				if (aff[i][j]==w)
					aff[i][j]=(weak-weak*delta) + ((weak+weak*delta) - (weak-weak*delta)) * random.nextDouble();
				
				if (aff[i][j]== m)
					aff[i][j]=(moderate-moderate*delta) + ((moderate+moderate*delta) - (moderate-moderate*delta)) * random.nextDouble();
						
				if (aff[i][j]==s)
					aff[i][j]=(strong-strong*delta) + ((strong+strong*delta) - (strong-strong*delta)) * random.nextDouble();
						
						
				//System.out.print(aff[i][j]+" ");
			}
			//System.out.println();
		}
				
		return aff;
	}	   
			
	/* returns a nb of people coming to the admin per hour based on affMatrix  */
	public static int getRandNbPeoplePerHour(Double[][] affMatrix, String day, Double hour){
		int averagePeople=2400; //average people per day based on caf's figures
		int nbPeople; //people coming on a given day and time
				
		String[] days={"monday","tuesday","thursday","friday"};
			
		Double[] hours={0.0,360.0,720.0,1080.0,1440.0,1800.0,2160.0,2520.0};
			
		int indexDay=0;
		int indexHour=0;
						
		for (int i=0;i<days.length;i++){
			if (days[i]==day)
				indexDay=i;
		}
			
		for (int j=0;j<hours.length;j++){
			if (hours[j]-hour==0)
				indexHour=j;
		}
			
		nbPeople=(int)((affMatrix[indexDay][indexHour])*averagePeople);
				
		//System.out.println(nbPeople);
				
		return nbPeople;
	}
			
	/* returns an array of people coming for each time slot during one week [day][opening hours] */
	public static int[][] getPeopleComingPerHourPerDay(Double[][] affMatrix){
		int[][] coming =
		    {
	    		{ 0, 0, 0, 0, 0, 0, 0, 0 } , // tab[day][hour] 
		        { 0, 0, 0, 0, 0, 0, 0, 0 },    //as we take caf paris 15 as example, only 4 opening days
		        { 0, 0, 0, 0, 0, 0, 0, 0 },   //monday tuesday thursday friday, from 9am to 5pm 
		        { 0, 0, 0, 0, 0, 0, 0, 0 },
			};
					
		Double[] hours={0.0,360.0,720.0,1080.0,1440.0,1800.0,2160.0,2520.0};
		
		String[] days={"monday","tuesday","thursday","friday"};
		
		for (int i=0;i<4;i++){
			for (int j=0;j<8;j++){
				coming[i][j]=getRandNbPeoplePerHour(affMatrix,days[i],hours[j]);
				//System.out.print(coming[i][j]);
				//System.out.print(" ");
			}
			//System.out.print("\n");
		}

		return coming;
	}
			
	/* generates a list of users coming during one day, sorted by arrivalTime */
	public static ArrayList<Double> getListOfArrivalTicks(int [][] comingMatrix, String day){
		ArrayList<Double> coming=new ArrayList<>();
		String[] days={"monday","tuesday","thursday","friday"};
		int indexDay=-1;
		for (int i=0;i<days.length;i++){
			if (days[i]==day)
				indexDay=i;
		}
							
		Double[] hours={0.0,360.0,720.0,1080.0,1440.0,1800.0,2160.0,2520.0,2880.0};
						
		//Double time=0.0;  
		//Double oneHour=3600000.0;  
		//parcours matrice du nb de gens qui viennent
				
		for (int j=0;j<8;j++){
			int nb=0;
			while (nb<comingMatrix[indexDay][j]){ 				
				coming.add(getRandArrivingTime(hours[j], hours[j+1]));	
				nb++;
				//System.out.println(nb);
			}	
		}
					
		Collections.sort(coming);
					
		/*for(Double elem: coming)
	    {
			System.out.println (elem);
	    }*/
					
		return coming;
				
	}	

}


	

