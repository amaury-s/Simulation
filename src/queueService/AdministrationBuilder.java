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
	private int nbGuichets = 5;
	public static int nbPeople = 2400 / 10;

	@Override
	public Context build(Context<Object> context) {
		this.context = context;

		context.setId("queueService");

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);

		this.space = spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.StrictBorders(), 70, 70);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);

		this.grid = gridFactory.createGrid("grid", context,
				new GridBuilderParameters<Object>(new StrictBorders(), new SimpleGridAdder<Object>(), true, 70, 70));

		Parameters params = RunEnvironment.getInstance().getParameters();

		int userCount = (Integer) params.getValue("user_count");

		for (int i = 0; i < userCount; i++) {
			User user = new User(space, grid, ThreadLocalRandom.current().nextInt(300, 1200 + 1), 0);
			context.add(user);
			Administration.waitingQueue.add(user);
		}

		context.add(new Administration(space, grid));
		ArrayList<Guichet> tabGuichets = new ArrayList();
		for (int i = 0; i < nbGuichets; i++) {
			tabGuichets.add(new Guichet(space, grid));
			context.add(tabGuichets.get(i));
			tabGuichets.get(i).space.moveTo(tabGuichets.get(i), space.getDimensions().getWidth() * i / nbGuichets + 1,
					69);
			tabGuichets.get(i).grid.moveTo(tabGuichets.get(i), grid.getDimensions().getWidth() * i / nbGuichets + 1,
					69);
			Administration.listOfGuichet.add(tabGuichets.get(i));
			/*
			 * Guichet aGuichet = new Guichet(space, grid);
			 * 
			 * context.add(aGuichet);
			 * 
			 * aGuichet.space.moveTo(aGuichet, 5+(i*5),69);
			 * aGuichet.grid.moveTo(aGuichet, 5+(i*5),69);
			 * 
			 * Administration.listOfGuichet.add(aGuichet);
			 */
		}

		float[][] affMatrix = getAffMatrix();
		int[][] comingMatrix = getPeopleComingPerHourPerDay(affMatrix);
		commingTicks.addAll(getListOfArrivalTicks(comingMatrix, "tuesday"));

		/*
		 * for(Double aDouble: commingTicks){ System.out.println(aDouble); }
		 */

		return context;
	}

	/****************************************************************************************************/

	public static int getRandArrivalTime(int beginning, int end) {
		final Random random = new Random();
		int arrivingTimeInMs = beginning + random.nextInt(end - beginning + 1);
		return arrivingTimeInMs;
	}

	/* returns a matrix of percentages [day][opening hours] */
	public static float[][] getAffMatrix() {
		/*
		 * this fct generates a matrix giving a percentage for each hour of each
		 * day for caf paris 15
		 */
		/*
		 * to understand my idea: from figures gathered on the CAF website we
		 * can find an average of people coming per day -->around 2400 for caf
		 * paris 15 with the percentages apply to that nb for each hour we can
		 * get random nb of people coming per hour per day we don't care if the
		 * sum for each day is > 100% bc the nb of people coming has to
		 * fluctuate
		 */

		final Random random = new Random();

		float delta = 0.1f;
		float w = 0.08f; // weak affluence, perc. of total nb of people coming
							// per day in pondered average
		float m = 0.15f; // moderate aff
		float s = 0.30f; // strong aff

		/* matrix based on a schedule from caf website giving affluence */

		float[][] aff = { { m, m, s, s, m, m, s, s }, // tab[day][hour]
				{ w, w, m, m, m, m, w, w }, // as we take caf paris 15 as
											// example, only 4 opening days
				{ w, w, w, w, w, w, m, m }, // monday tuesday thursday friday,
											// from 9am to 5pm
				{ m, m, m, m, m, m, s, s }, };

		for (int i = 0; i < aff.length; i++) {
			for (int j = 0; j < aff[i].length; j++) {

				if (aff[i][j] == w)
					aff[i][j] = (w - w * delta) + ((w + w * delta) - (w - w * delta)) * random.nextFloat();// rajouter
																											// +-
																											// 10%

				if (aff[i][j] == m)
					aff[i][j] = (m - m * delta) + ((m + m * delta) - (m - m * delta)) * random.nextFloat();

				if (aff[i][j] == s)
					aff[i][j] = (s - s * delta) + ((s + s * delta) - (s - s * delta)) * random.nextFloat();

				// System.out.print(aff[i][j]+" ");
			}
			// System.out.println();
		}

		return aff;
	}

	/* returns a nb of people coming to the admin per hour based on affMatrix */
	public static int getRandNbPeoplePerHour(float[][] affMatrix, String day, int hour) {
		int averagePeople = nbPeople; // average people per day based on caf's
										// figures
		int nbPeople; // people coming on a given day and time

		String[] days = { "monday", "tuesday", "thursday", "friday" };

		// Real Time
		// int[]
		// hours={0,3600000,7200000,10800000,14400000,18000000,21600000,25200000};
		int[] hours = { 0, 3600, 7200, 10800, 14400, 18000, 21600, 25200 };

		int indexDay = 0;
		int indexHour = 0;

		for (int i = 0; i < days.length; i++) {
			if (days[i] == day)
				indexDay = i;
		}

		for (int j = 0; j < hours.length; j++) {
			if (hours[j] == hour)
				indexHour = j;
		}

		nbPeople = (int) ((affMatrix[indexDay][indexHour]) * averagePeople);

		// System.out.println(nbPeople);

		return nbPeople;
	}

	/*
	 * returns an array of people coming for each time slot during one week
	 * [day][opening hours]
	 */
	public static int[][] getPeopleComingPerHourPerDay(float[][] affMatrix) {
		int[][] coming = { { 0, 0, 0, 0, 0, 0, 0, 0 }, // tab[day][hour]
				{ 17, 17, 32, 37, 34, 35, 17, 18 }, // as we take caf paris 15
													// as example, only 4
													// opening days
				{ 0, 0, 0, 0, 0, 0, 0, 0 }, // monday tuesday thursday friday,
											// from 9am to 5pm
				{ 0, 0, 0, 0, 0, 0, 0, 0 }, };

		// Real Time
		// int[]
		// hours={0,3600000,7200000,10800000,14400000,18000000,21600000,25200000};
		int[] hours = { 0, 3600, 7200, 10800, 14400, 18000, 21600, 25200 };

		String[] days = { "monday", "tuesday", "thursday", "friday" };

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 8; j++) {
				// coming[i][j]=getRandNbPeoplePerHour(affMatrix,days[i],hours[j]);
				// System.out.print(coming[i][j]);
				// System.out.print(" ");
			}
			// System.out.print("\n");
		}

		return coming;
	}

	/* generates a list of users coming during one day, sorted by arrivalTime */
	public static ArrayList<Double> getListOfArrivalTicks(int[][] comingMatrix, String day) {

		double[] comingList = { 399, 1127, 1499, 286, 3355, 892, 2565, 1794, 302, 584, 2379, 1129, 50, 1899, 803, 471,
				1172, 5995, 5353, 5186, 6997, 6320, 5620, 5015, 6781, 6691, 4405, 6181, 4548, 5124, 3659, 5169, 6508,
				4414, 10318, 7699, 9462, 9507, 10058, 8268, 7447, 7798, 9896, 10612, 8018, 8415, 9210, 9042, 10118,
				10234, 9949, 9359, 10148, 9252, 9741, 10509, 7980, 9948, 7479, 10781, 7913, 8975, 7409, 7845, 7456,
				9352, 14113, 11482, 11873, 12816, 10940, 12442, 13482, 12243, 11759, 10809, 12159, 11953, 14296, 12032,
				10957, 13172, 12321, 13984, 14080, 12007, 11285, 12686, 13083, 11763, 11081, 11838, 14125, 13746, 12648,
				11956, 13422, 14361, 10928, 10872, 13227, 12269, 10932, 16342, 16998, 14469, 17037, 16711, 17088, 16270,
				16747, 16346, 17159, 15544, 15227, 15908, 15967, 14879, 17071, 17660, 16312, 17859, 17242, 17370, 16969,
				15831, 16600, 17699, 15534, 16969, 17080, 15520, 16162, 15294, 15988, 17375, 15810, 19407, 19688, 20284,
				19895, 21347, 20803, 20673, 21543, 18851, 20639, 18402, 19441, 18720, 18797, 19606, 20134, 18475, 20450,
				20034, 19014, 20975, 20003, 19471, 18681, 18487, 21129, 20373, 18716, 18256, 20939, 18929, 21396, 19420,
				19472, 20903, 23000, 24042, 23957, 23817, 23752, 24361, 23085, 23843, 24452, 23219, 23664, 25118, 24557,
				23806, 22787, 22309, 23190, 27613, 27083, 26447, 26061, 26725, 27053, 28005, 26653, 25523, 27124, 25596,
				26167, 28773, 26122, 26135, 25785, 25678, 27632 };
		ArrayList<Double> coming = new ArrayList<>();
		for (int i = 0; i < comingList.length; i++) {
			coming.add(comingList[i]);

		}

		String[] days = { "monday", "tuesday", "thursday", "friday" };

		int indexDay = 0;

		for (int i = 0; i < days.length; i++) {
			if (days[i] == day)
				indexDay = i;
		}

		// Real Time
		// int[]
		// hours={0,3600000,7200000,10800000,14400000,18000000,21600000,25200000,28800000};
		int[] hours = { 0, 3600, 7200, 10800, 14400, 18000, 21600, 25200, 28800 }; // en
																					// secondes
																					// x10

		// Double time=0.0;
		// Double oneHour=3600000.0;
		// parcours matrice du nb de gens qui viennent

		/*
		 * for (int j = 0; j < 8; j++){
		 * 
		 * int nb = 0; int max = comingMatrix[indexDay][j];
		 * 
		 * while (nb < max){ Double newTick=
		 * (double)getRandArrivalTime(hours[j], hours[j+1]);
		 * coming.add(newTick); nb++; } }
		 */

		Collections.sort(coming);

		/*
		 * for(Double elem: coming) { System.out.println (elem); }
		 */

		// System.out.println(coming.size());
		return coming;

	}

}