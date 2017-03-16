package queueService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;

public class AdministrationBuilder implements ContextBuilder<Object> {
	
	private Context<Object> context;
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public static List<Double> commingTicks = new ArrayList<>();
	
	@Override
	public Context build(Context<Object> context) {
		this.context = context;
		
		context.setId("queueService");
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		
		this.space = spaceFactory.createContinuousSpace("space", context,
				new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.StrictBorders(),
				70, 70);
					
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		
		this.grid = gridFactory.createGrid("grid", context, 
				new GridBuilderParameters<Object>(new StrictBorders(),
						new SimpleGridAdder<Object>(),
						true, 70, 70));
		
		Parameters params = RunEnvironment.getInstance().getParameters();
		
		int userCount = (Integer)params.getValue("user_count");
		
		for(int i = 0; i < userCount; i++) {
			User user = new User(space, grid, ThreadLocalRandom.current().nextInt(10, 20 + 1), 0);
			context.add(user);
			Administration.waitingQueue.add(user);
		}
		
		context.add(new Administration(space, grid));
		
		Guichet aGuichet = new Guichet(space, grid);

		context.add(aGuichet);
		
		aGuichet.space.moveTo(aGuichet, 35,69);
		aGuichet.grid.moveTo(aGuichet, 35,69);
		
		Administration.listOfGuichet.add(aGuichet);
		
		Double[][] affMatrix = getAffMatrix();
    	int[][] comingMatrix = getPeopleComingPerHourPerDay(affMatrix);
    	commingTicks.addAll(getListOfArrivalTicks(comingMatrix,"tuesday"));

		return context;
	}
	
	/****************************************************************************************************/
	
	public static Double getRandArrivingTime(Double beginning, Double end){
		final Random random = new Random();
		int arrivingTimeInMs = (int) (((end-beginning + 1) + beginning) * random.nextDouble());
		return (double)arrivingTimeInMs;
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
					aff[i][j]=(weak-weak*delta) + ((weak+weak*delta) - (weak-weak*delta)) * random.nextDouble();// rajouter +- 10%
				
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
		int averagePeople=200; //average people per day based on caf's figures
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
		
		ArrayList<Double> coming = new ArrayList<>();
		
		String[] days = {"monday","tuesday","thursday","friday"};
		int indexDay = -1;
		
		for (int i = 0; i < days.length; i++){
			if (days[i] == day)
				indexDay = i;
		}
							
		Double[] hours = {0.0,360.0,720.0,1080.0,1440.0,1800.0,2160.0,2520.0,2880.0};
						
		//Double time=0.0;  
		//Double oneHour=3600000.0;  
		//parcours matrice du nb de gens qui viennent
				
		for (int j = 0; j < 8; j++){
			int nb = 0;
			while (nb < comingMatrix[indexDay][j]){ 				
				coming.add(getRandArrivingTime(hours[j], hours[j+1]));	
				nb++;
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
