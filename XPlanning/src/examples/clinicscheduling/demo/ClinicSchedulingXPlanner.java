package examples.clinicscheduling.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import examples.clinicscheduling.metrics.IdleTimeQFunction;
import examples.clinicscheduling.metrics.LeadTimeQFunction;
import examples.clinicscheduling.metrics.OvertimeQFunction;
import examples.clinicscheduling.metrics.RevenueQFunction;
import examples.clinicscheduling.metrics.SwitchABPQFunction;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.UtilityWeightPlanner;
import examples.common.XPlannerOutDirectories;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.policy.Decision;
import prism.PrismException;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;

public class ClinicSchedulingXPlanner {

	public static final String PROBLEMS_PATH = "/data/clinicscheduling/missions";
	public static final int DEFAULT_BRANCH_FACTOR = 3;

	private UtilityWeightPlanner mXPlanner;

	public ClinicSchedulingXPlanner(int branchFactor, XPlannerOutDirectories outputDirs,
			VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new ClinicSchedulingXMDPLoader(branchFactor);
		mXPlanner = new UtilityWeightPlanner(xmdpLoader, outputDirs, getVocabulary(), verbalizerSettings);
	}

	public PolicyInfo runXPlanning(File problemFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return mXPlanner.runXPlanning(problemFile, CostCriterion.AVERAGE_COST);
	}

	public PolicyInfo runPlanning(File problemFile) throws DSMException, XMDPException, PrismException, IOException,
			ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(problemFile, CostCriterion.AVERAGE_COST);
	}
	

	public PolicyInfo runPlanning(int revenuePPatient, int overtimeCostPPatient, int idleTimeCostPPatient, int leadTimeCostFactor, int switchABPCostFactor, File problemFile) throws DSMException, XMDPException, PrismException, IOException,
			ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(problemFile, revenuePPatient, overtimeCostPPatient, idleTimeCostPPatient, leadTimeCostFactor, switchABPCostFactor);
	}
	
	public static void main(String[] args)
			throws PrismException, XMDPException, IOException, GRBException, DSMException, PrismConnectorException {
		String problemFilename = args[0];
		File problemFile = new File(PROBLEMS_PATH, problemFilename);

		Path policiesOutputPath = Paths.get(XPlannerOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlannerOutDirectories.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		ClinicSchedulingXPlanner xplanner = new ClinicSchedulingXPlanner(DEFAULT_BRANCH_FACTOR, outputDirs,
				defaultVerbalizerSettings);
		xplanner.runXPlanning(problemFile);
		
		//--capacity=2 --maxABP=2 --maxQueueSize=4 --revenuePerPatient=20 --overtimeCostPerPatient=10 --idleTimeCostPerPatient=0
		//		--leadTimeCostFactor=0 --switchABPCostFactor=10 --iniABP=1 --iniABCount=0 --iniNewClientCount=2 --clientArrivalRate=2
		ArrayList<String> states = new ArrayList<String>();
		//PolicyInfo infs = xplanner.runPlanning(20, 10, 0, 0, 10, problemFile);
		//states = printToExcel("tmpdata/sample_"+(new Date())+".csv", infs, states, 1);

		for (int revenuePPatient = 1; revenuePPatient < 30; revenuePPatient+=2) {
			for (int overtimeCostPPatient = 1; overtimeCostPPatient < 30; overtimeCostPPatient+=2) {
				for (int idleTimeCostPPatient = 1; idleTimeCostPPatient < 15; idleTimeCostPPatient+=2) {
					for (int leadTimeCostFactor=1; leadTimeCostFactor < 20; leadTimeCostFactor+=2) {
						for (int switchABPCostFactor = 1; switchABPCostFactor < 30; switchABPCostFactor+=2) {
							PolicyInfo inf = xplanner.runPlanning(revenuePPatient, overtimeCostPPatient, idleTimeCostPPatient, leadTimeCostFactor, switchABPCostFactor, problemFile);
							states = printToExcel("tmpdata/sample_"+(new Date())+".csv", inf, states, 1);
						}
					}
				}
			}
		}

	}

	private static ArrayList<String> printToExcel(String fileName, PolicyInfo inf, ArrayList<String> states, int number) {
		ArrayList<String> arrayList = new ArrayList<String>();
		HashMap<String, String> decisions = new HashMap<>();		
		if (inf.getPolicy() == null)
			return states;
		Iterator<Decision> itr = inf.getPolicy().iterator();
		int numberSteps = 0;
		int numberMoveTo = 0;
		int numberAdjustSpeed = 0;
		while(itr.hasNext()) {
			Decision element = itr.next();
			String action= element.getAction().toString();
			String state = element.getState().toString();
			numberSteps++;
			if (element.getAction().getNamePrefix().equals("moveTo"))
				numberMoveTo++;
			else if (element.getAction().getNamePrefix().equals("setSpeed"))
				numberAdjustSpeed++;
			//for (IStateVarValue i : element.getAction().getParameters())
			//	i.getAttributeValue(fileName)
			if (!states.contains(state))
				states.add(state.replace(",", "&"));
			decisions.put(state.replace(",", "&"), action.replace(",", "&"));
		}
		List<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> scalingQAs = new ArrayList<>();
		scalingQAs.addAll(inf.getXMDP().getCostFunction().getAttributeCostFunctions());
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> i : scalingQAs)
			arrayList.add(Double.toString(inf.getXMDP().getCostFunction().getScalingConstant(i)));
		Iterator<IQFunction<IAction, ITransitionStructure<IAction>>> qSpaceItr = inf.getXMDP().getQSpace().iterator();
		while(qSpaceItr.hasNext()) {
			IQFunction<IAction, ITransitionStructure<IAction>> element = qSpaceItr.next();
			double d = inf.getScaledQACost(element);
			arrayList.add(Double.toString(d));
		}
		arrayList.add(Double.toString(inf.getObjectiveCost()));
		arrayList.add(Integer.toString(numberSteps));
		arrayList.add(Integer.toString(numberMoveTo));
		arrayList.add(Integer.toString(numberAdjustSpeed));
		for (String v : decisions.values())
			arrayList.add(v);
		try{
			FileWriter file = new FileWriter(fileName, true);
			PrintWriter write = new PrintWriter(file);
			if (number == 0) {
				for (int i = 0; i < scalingQAs.size(); i++) {
					write.append("w_"+scalingQAs.get(i).getName());
					write.append(",");
				}
				for (int i = 0; i < scalingQAs.size(); i++) {
					write.append(scalingQAs.get(i).getName());
					write.append(",");
				}
				write.append("Total Cost");
				write.append(", NumberSteps");
				write.append(", NumberMoveTo");
				write.append(", NumberAdjustSpeed");
				for (String v : states) {
					write.append(",");
					write.append(v);
				}
				write.println();
			}
			else {
				String c = "";
				for (String name: arrayList){
					write.append(c);
					write.append(name);
					c = ",";
				}
				write.println();
			}
			write.close();
		}
		catch(IOException exe){
			System.out.println("Cannot create file");
		}		
		return states;
	}
	
	public static Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(RevenueQFunction.NAME, "revenue");
		vocab.putVerb(RevenueQFunction.NAME, "have");
		vocab.putUnit(RevenueQFunction.NAME, "dollar in revenue", "dollars in revenue");
		vocab.putNoun(OvertimeQFunction.NAME, "overtime cost");
		vocab.putVerb(OvertimeQFunction.NAME, "have");
		vocab.putUnit(OvertimeQFunction.NAME, "dollar in overtime cost", "dollars in overtime cost");
		vocab.putNoun(IdleTimeQFunction.NAME, "idle time cost");
		vocab.putVerb(IdleTimeQFunction.NAME, "have");
		vocab.putUnit(IdleTimeQFunction.NAME, "dollar in idle time cost", "dollars in idle time cost");
		vocab.putNoun(LeadTimeQFunction.NAME, "appointment lead time cost");
		vocab.putVerb(LeadTimeQFunction.NAME, "have");
		vocab.putUnit(LeadTimeQFunction.NAME, "dollar in appointment lead time cost",
				"dollars in appointment lead time cost");
		vocab.putNoun(SwitchABPQFunction.NAME, "switching ABP cost");
		vocab.putVerb(SwitchABPQFunction.NAME, "have");
		vocab.putUnit(SwitchABPQFunction.NAME, "dollar in switching ABP cost", "dollars in switching ABP cost");
		vocab.setPeriodUnit("day");
		return vocab;
	}

}
