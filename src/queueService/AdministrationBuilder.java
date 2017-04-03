package queueService;

import java.sql.Time;
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
		int nbGuichets = 20;
		ArrayList<Guichet> tabGuichets = new ArrayList();
		for(int i=0; i < nbGuichets; i++){
			tabGuichets.add(new Guichet(space, grid));
			context.add(tabGuichets.get(i));
			tabGuichets.get(i).space.moveTo(tabGuichets.get(i), space.getDimensions().getWidth()*i/nbGuichets + 1,69);
			tabGuichets.get(i).grid.moveTo(tabGuichets.get(i), grid.getDimensions().getWidth()*i/nbGuichets + 1,69);
			Administration.listOfGuichet.add(tabGuichets.get(i));
			/*
			Guichet aGuichet = new Guichet(space, grid);

			context.add(aGuichet);
		
			aGuichet.space.moveTo(aGuichet, 5+(i*5),69);
			aGuichet.grid.moveTo(aGuichet, 5+(i*5),69);
		
			Administration.listOfGuichet.add(aGuichet);*/
		}
				
		float[][] affMatrix = getAffMatrix();
    	int[][] comingMatrix = getPeopleComingPerHourPerDay(affMatrix);
    	commingTicks.addAll(getListOfArrivalTicks(comingMatrix,"tuesday"));
    	
    	for(Double aDouble: commingTicks){
    		System.out.println(aDouble);
    	}
    	
    	
		return context;
	}
	
	/****************************************************************************************************/
	
	public static int getRandArrivalTime(int beginning, int end){
		final Random random = new Random();
		int arrivingTimeInMs = random.nextInt((end-beginning + 1) + beginning);
		return arrivingTimeInMs;
	}
	
	/* returns a matrix of percentages [day][opening hours] */
	public static float[][] getAffMatrix(){
	/* this fct generates a matrix giving a percentage for each hour of each day for caf paris 15*/
	/* to understand my idea:
	 *   from figures gathered on the CAF website we can find an average of people coming per day 
	 *   -->around 2400 for caf paris 15
	 *   with the percentages apply to that nb for each hour we can get random nb of people coming per hour per day
	 *   we don't care if the sum for each day is > 100% bc the nb of people coming has to fluctuate 
	 */
				
						
		final Random random = new Random();
					
		float delta=0.1f;
		float w=0.08f;   //weak affluence, perc. of total nb of people coming per day in pondered average
		float m=0.15f;   //moderate aff
		float s=0.30f;   //strong aff
			
		/*matrix based on a schedule from caf website giving affluence */
			
		float[][] aff=
			{
		        { m, m, s, s, m, m, s, s } , // tab[day][hour] 
		        { w, w, m, m, m, m, w, w },    //as we take caf paris 15 as example, only 4 opening days
		        { w, w, w, w, w, w, m, m },   //monday tuesday thursday friday, from 9am to 5pm 
		        { m, m, m, m, m, m, s, s },
	        };
				
		for (int i=0;i<aff.length;i++){
			for (int j=0;j<aff[i].length;j++){
			
				if (aff[i][j]==w)
					aff[i][j]=(w-w*delta) + ((w+w*delta) - (w-w*delta)) * random.nextFloat();// rajouter +- 10%
				
				if (aff[i][j]== m)
					aff[i][j]=(m-m*delta) + ((m+m*delta) - (m-m*delta)) * random.nextFloat();
						
				if (aff[i][j]==s)
					aff[i][j]=(s-s*delta) + ((s+s*delta) - (s-s*delta)) * random.nextFloat();
						
						
				//System.out.print(aff[i][j]+" ");
			}
			//System.out.println();
		}
				
		return aff;
	}	   
			
	/* returns a nb of people coming to the admin per hour based on affMatrix  */
	public static int getRandNbPeoplePerHour(float[][] affMatrix, String day, int hour){
		int averagePeople=2400; //average people per day based on caf's figures
		int nbPeople; //people coming on a given day and time
				
		String[] days={"monday","tuesday","thursday","friday"};
			
		// Real Time
		//int[] hours={0,3600000,7200000,10800000,14400000,18000000,21600000,25200000};
		int[] hours={0,3600,7200,10800,14400,18000,21600,25200};
			
		int indexDay=0;
		int indexHour=0;
						
		for (int i=0;i<days.length;i++){
			if (days[i]==day)
				indexDay=i;
		}
			
		for (int j=0;j<hours.length;j++){
			if (hours[j]==hour)
				indexHour=j;
		}
			
		nbPeople=(int)((affMatrix[indexDay][indexHour])*averagePeople);
				
		//System.out.println(nbPeople);
				
		return nbPeople;
	}
			
	/* returns an array of people coming for each time slot during one week [day][opening hours] */
	public static int[][] getPeopleComingPerHourPerDay(float[][] affMatrix){
		int[][] coming =
		    {
	    		{ 0, 0, 0, 0, 0, 0, 0, 0 } , // tab[day][hour] 
		        { 0, 0, 0, 0, 0, 0, 0, 0 },    //as we take caf paris 15 as example, only 4 opening days
		        { 0, 0, 0, 0, 0, 0, 0, 0 },   //monday tuesday thursday friday, from 9am to 5pm 
		        { 0, 0, 0, 0, 0, 0, 0, 0 },
			};
		
		//Real Time		
		//int[] hours={0,3600000,7200000,10800000,14400000,18000000,21600000,25200000};
		int[] hours={0,3600,7200,10800,14400,18000,21600,25200};
		
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
		
		// Real Time
		//int[] hours={0,3600000,7200000,10800000,14400000,18000000,21600000,25200000,28800000};
		int[] hours={0,3600,7200,10800,14400,18000,21600,25200,28800}; // en secondes x10
						
		//Double time=0.0;  
		//Double oneHour=3600000.0;  
		//parcours matrice du nb de gens qui viennent
				
		for (int j = 0; j < 8; j++){
			int nb = 0;
			while (nb < comingMatrix[indexDay][j]){ 				
				coming.add((double)getRandArrivalTime(hours[j], hours[j+1]));	
				nb++;
			}	
		}
					
		Collections.sort(coming);
					
		/*for(Double elem: coming)
	    {
			System.out.println (elem);
	    }*/
		
		//System.out.println(coming.size());
		return coming;
				
	}	
	

}
