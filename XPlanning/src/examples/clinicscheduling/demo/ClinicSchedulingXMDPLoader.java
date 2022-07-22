package examples.clinicscheduling.demo;

import java.io.File;

import org.apache.commons.cli.Options;

import examples.clinicscheduling.dsm.ClinicCostProfile;
import examples.clinicscheduling.dsm.ClinicSchedulingXMDPBuilder;
import examples.clinicscheduling.dsm.SchedulingContext;
import examples.common.CommandLineXMDPLoader;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import language.exceptions.IncompatibleActionException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class ClinicSchedulingXMDPLoader implements IXMDPLoader {

	private static final String CAPACITY_PARAM = "capacity";
	private static final String MAX_ABP_PARAM = "maxABP";
	private static final String MAX_QUEUE_SIZE_PARAM = "maxQueueSize";
	private static final String REVENUE_PARAM = "revenuePerPatient";
	private static final String OVERTIME_COST_PARAM = "overtimeCostPerPatient";
	private static final String IDLE_TIME_COST_PARAM = "idleTimeCostPerPatient";
	private static final String LEAD_TIME_COST_PARAM = "leadTimeCostFactor";
	private static final String SWITCH_ABP_COST_PARAM = "switchABPCostFactor";
	private static final String INI_ABP_PARAM = "iniABP";
	private static final String INI_AB_COUNT_PARAM = "iniABCount";
	private static final String INI_NEW_CLIENT_COUNT_PARAM = "iniNewClientCount";
	private static final String CLIENT_ARRIVAL_RATE_PARAM = "clientArrivalRate";

	private ClinicSchedulingXMDPBuilder mXMDPBuilder;
	private CommandLineXMDPLoader mCLLoader;

	public ClinicSchedulingXMDPLoader(int branchFactor) {
		mXMDPBuilder = new ClinicSchedulingXMDPBuilder(branchFactor);

		Options options = new Options();
		options.addOption("C", CAPACITY_PARAM, true, "Capacity of the clinic");
		options.addOption("M", MAX_ABP_PARAM, true, "Maximum ABP");
		options.addOption("N", MAX_QUEUE_SIZE_PARAM, true, "Maximum queue size");
		options.addOption("R", REVENUE_PARAM, true, "Revenue per patient");
		options.addOption("O", OVERTIME_COST_PARAM, true, "Overtime cost per patient");
		options.addOption("I", IDLE_TIME_COST_PARAM, true, "Idle time cost per patient");
		options.addOption("L", LEAD_TIME_COST_PARAM, true, "Lead time cost factor");
		options.addOption("S", SWITCH_ABP_COST_PARAM, true, "Switching ABP cost factor");
		options.addOption("w", INI_ABP_PARAM, true, "Initial ABP");
		options.addOption("x", INI_AB_COUNT_PARAM, true, "Initial number of advance-booking patients");
		options.addOption("y", INI_NEW_CLIENT_COUNT_PARAM, true, "Initial number of new patients");
		options.addOption("l", CLIENT_ARRIVAL_RATE_PARAM, true, "Average patient arrival rate");
		mCLLoader = new CommandLineXMDPLoader(options);
	}

	@Override
	public XMDP loadXMDP(File problemFile) throws XMDPException, DSMException {
		mCLLoader.loadCommandLineFromFile(problemFile);

		int capacity = mCLLoader.getIntArgument(CAPACITY_PARAM);
		int maxABP = mCLLoader.getIntArgument(MAX_ABP_PARAM);
		int maxQueueSize = mCLLoader.getIntArgument(MAX_QUEUE_SIZE_PARAM);
		double revenuePerPatient = mCLLoader.getDoubleArgument(REVENUE_PARAM);
		double overtimeCostPerPatient = mCLLoader.getDoubleArgument(OVERTIME_COST_PARAM);
		double idleTimeCostPerPatient = mCLLoader.getDoubleArgument(IDLE_TIME_COST_PARAM);
		double leadTimeCostFactor = mCLLoader.getDoubleArgument(LEAD_TIME_COST_PARAM);
		double switchABPCostFactor = mCLLoader.getDoubleArgument(SWITCH_ABP_COST_PARAM);
		int iniABP = mCLLoader.getIntArgument(INI_ABP_PARAM);
		int iniABCount = mCLLoader.getIntArgument(INI_AB_COUNT_PARAM);
		int iniNewClientCount = mCLLoader.getIntArgument(INI_NEW_CLIENT_COUNT_PARAM);
		double clientArrivalRate = mCLLoader.getDoubleArgument(CLIENT_ARRIVAL_RATE_PARAM);

		ClinicCostProfile clinicCostProfile = new ClinicCostProfile(revenuePerPatient, overtimeCostPerPatient,
				idleTimeCostPerPatient, leadTimeCostFactor, switchABPCostFactor);
		SchedulingContext schedulingContext = new SchedulingContext(capacity, maxABP, maxQueueSize, clinicCostProfile);

		return mXMDPBuilder.buildXMDP(schedulingContext, iniABP, iniABCount, iniNewClientCount, clientArrivalRate);
	}

	public XMDP loadXMDP(File problemFile, int revenuePPatient, int overtimeCostPPatient, int idleTimeCostPPatient,
			int leadTimeCostFactor, int switchABPCostFactor) {
		try {
			mCLLoader.loadCommandLineFromFile(problemFile);
		} catch (DSMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//capacity=2 --maxABP=2 --maxQueueSize=4 --revenuePerPatient=20 --overtimeCostPerPatient=10 --idleTimeCostPerPatient=0
				//		--leadTimeCostFactor=0 --switchABPCostFactor=10 --iniABP=1 --iniABCount=0 --iniNewClientCount=2 --clientArrivalRate=2

		int capacity = 2;//mCLLoader.getIntArgument(CAPACITY_PARAM);
		int maxABP = 2;// mCLLoader.getIntArgument(MAX_ABP_PARAM);
		int maxQueueSize = 4;//mCLLoader.getIntArgument(MAX_QUEUE_SIZE_PARAM);
		double revenuePerPatient = revenuePPatient;
		double overtimeCostPerPatient = overtimeCostPPatient;
		double idleTimeCostPerPatient = idleTimeCostPPatient;
		int iniABP = 1;//mCLLoader.getIntArgument(INI_ABP_PARAM);
		int iniABCount = 0;//mCLLoader.getIntArgument(INI_AB_COUNT_PARAM);
		int iniNewClientCount = 2;//mCLLoader.getIntArgument(INI_NEW_CLIENT_COUNT_PARAM);
		double clientArrivalRate = 2;//mCLLoader.getDoubleArgument(CLIENT_ARRIVAL_RATE_PARAM);

		ClinicCostProfile clinicCostProfile = new ClinicCostProfile(revenuePerPatient, overtimeCostPerPatient,
				idleTimeCostPerPatient, leadTimeCostFactor, switchABPCostFactor);
		SchedulingContext schedulingContext = new SchedulingContext(capacity, maxABP, maxQueueSize, clinicCostProfile);

		try {
			return mXMDPBuilder.buildXMDP(schedulingContext, iniABP, iniABCount, iniNewClientCount, clientArrivalRate);
		} catch (IncompatibleActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public XMDP loadXMDP(File problemFile, double d1, double d2, double d3) throws DSMException, XMDPException {
		// TODO Auto-generated method stub
		return loadXMDP(problemFile);
	}

	@Override
	public XMDP loadXMDP(File file, double i, double j) throws XMDPException, DSMException {
		// TODO Auto-generated method stub
		return null;
	}

}
