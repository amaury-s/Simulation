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
	public final static int kindOfQueueSorting = (Integer) RunEnvironment.getInstance().getParameters()
			.getValue("kind_of_queue_sorting");
	public int c = 0;
	public int UserId = 0;
	public int[] UserNumberOfTickOfService = { 815, 704, 1056, 1081, 1052, 845, 923, 824, 358, 1041, 563, 617, 627,
			1015, 718, 728, 592, 759, 1090, 845, 1070, 907, 719, 410, 824, 726, 452, 379, 1014, 1170, 363, 755, 956,
			554, 1152, 369, 845, 733, 1142, 557, 1049, 587, 582, 628, 602, 968, 1028, 309, 1060, 979, 752, 498, 813,
			892, 826, 1045, 987, 701, 512, 380, 701, 334, 897, 1151, 560, 716, 1092, 1079, 914, 1084, 1147, 304, 897,
			1182, 495, 637, 545, 1195, 352, 388, 1143, 498, 734, 936, 1142, 802, 1179, 874, 304, 567, 582, 500, 416,
			783, 594, 1111, 662, 498, 868, 818, 304, 781, 496, 758, 946, 596, 559, 346, 804, 1145, 556, 1000, 514, 484,
			1069, 460, 535, 344, 303, 651, 414, 1029, 879, 898, 1082, 430, 537, 344, 702, 1064, 521, 983, 899, 730, 735,
			1102, 592, 519, 917, 916, 815, 670, 648, 457, 1188, 726, 673, 809, 415, 683, 562, 619, 385, 697, 1030, 322,
			319, 800, 880, 583, 1112, 554, 841, 304, 979, 490, 929, 635, 765, 705, 1186, 690, 735, 1193, 1055, 1152,
			885, 941, 594, 1003, 544, 823, 364, 1104, 618, 333, 534, 1045, 356, 964, 718, 1069, 675, 626, 577, 878, 318,
			321, 965, 832, 384, 720, 482, 398, 415, 727, 508 };

	public Administration(ContinuousSpace<Object> space, Grid<Object> grid) {
		this.space = space;
		this.grid = grid;
	}

	@ScheduledMethod(start = 20, interval = 1)
	public void step() {
		List<Guichet> listOfFreeGuichet = getFreeGuichets();

		for (Guichet aFreeGuichet : listOfFreeGuichet) {
			if (!waitingQueue.isEmpty()) {
				User firstUser = new User();
				switch (kindOfQueueSorting) {
				case 1:
					firstUser = waitingQueue.get(0);
					break;
				case 2:
					firstUser = waitingQueue.get(FindNextUser());
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

	/*
	 * public int FindNextUser(){ int min = 0; int i = 0; for(User
	 * user:waitingQueue){ if(waitingQueue.get(min).numberOfTickOfService >
	 * waitingQueue.get(i).numberOfTickOfService){ min=i; } i++; } return min; }
	 * public int FindNextUser(){ int min = 0; int i = 0; double ratio=3;
	 * boolean isRatio=false; for(User user:waitingQueue){
	 * if(((RunEnvironment.getInstance().getCurrentSchedule().getTickCount()-
	 * waitingQueue.get(i).arrivalTick)/waitingQueue.get(i).
	 * numberOfTickOfService)>ratio){ isRatio=true;
	 * ratio=((RunEnvironment.getInstance().getCurrentSchedule().getTickCount()-
	 * waitingQueue.get(i).arrivalTick)/waitingQueue.get(i).
	 * numberOfTickOfService); min=i; }else{
	 * if(waitingQueue.get(min).numberOfTickOfService >
	 * waitingQueue.get(i).numberOfTickOfService && isRatio==false){ min=i; } }
	 * i++; } return min; }
	 */
	public static int maxWaitingQueue(List<User> aWaitingQueue) {

		int max = 0;

		int i = 0;

		for (User user : waitingQueue) {

			if (waitingQueue.get(max).numberOfTickOfService <= user.numberOfTickOfService) {

				max = i;

			}

			i++;

		}

		return max;

	}

	public int FindNextUser() {

		int min = 0;

		int i = 0;

		int time = 0;

		int inter = 30000;

		for (User user : waitingQueue) {

			time += user.numberOfTickOfService;

			if (time > inter) {

				return maxWaitingQueue(waitingQueue);

			} else {

				if (waitingQueue.get(min).numberOfTickOfService >= user.numberOfTickOfService) {

					min = i;

				}

			}

			i++;

		}

		return min;

	}

	@ScheduledMethod(start = 0, interval = 1)
	public void addUser() {
		Random random = new Random();
		if (AdministrationBuilder.commingTicks
				.contains(RunEnvironment.getInstance().getCurrentSchedule().getTickCount())) {
			// System.out.println(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
			// correspond entre 10 et 20 minutes en secondes x10
			User user = new User(space, grid, UserNumberOfTickOfService[UserId],
					RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
			UserId += 1;
			// ThreadLocalRandom.current().
			c += 1;
			// System.out.println(c);

			Context<Object> context = ContextUtils.getContext(this);

			context.add(user);

			waitingQueue.add(user);

			space.moveTo(user, 25, 0);
			grid.moveTo(user, 25, 0);
		}

	}

	private List<Guichet> getFreeGuichets() {
		List<Guichet> listOfGuichet = new ArrayList<>();

		for (Guichet aGuichet : this.listOfGuichet) {
			if (aGuichet.isFree) {
				listOfGuichet.add(aGuichet);
			}
		}

		return listOfGuichet;
	}

}
