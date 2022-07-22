package examples.mobilerobot.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.util.Precision;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.UtilityWeightPlanner;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.QADecimalFormatter;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.policy.Decision;
import prism.PrismException;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;

public class MobileRobotXPlanner {
	public static final String MAPS_PATH = "./data/mobilerobot/maps";
	public static final String MISSIONS_PATH = "./data/mobilerobot/missions";

	private UtilityWeightPlanner mXPlanner;

	private final IXMDPLoader mXmdpLoader;

	public MobileRobotXPlanner(File mapsJsonDir, XPlannerOutDirectories outputDirs,
			VerbalizerSettings verbalizerSettings) {
		mXmdpLoader = new MobileRobotXMDPExampleLoader(mapsJsonDir);
		// change the following to XPlanner if you don't want to use the planner with adjustable utility function weights.
		mXPlanner = new UtilityWeightPlanner(mXmdpLoader, outputDirs, getVocabulary(), verbalizerSettings);
	}

	public XMDP loadXMDPFromMissionFile(File missionJsonFile) throws DSMException, XMDPException {
		return mXPlanner.loadXMDPFromProblemFile(missionJsonFile);
	}

	public PolicyInfo runXPlanning(File missionJsonFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return mXPlanner.runXPlanning(missionJsonFile, CostCriterion.TOTAL_COST);
	}

	public PolicyInfo runPlanning(File missionJsonFile, double w_travelTime, double w_intru, double w_collision) throws DSMException, XMDPException, PrismException, IOException,
	ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(missionJsonFile, CostCriterion.TOTAL_COST, w_travelTime, w_intru, w_collision);
	}

	public PolicyInfo runPlanning(File missionJsonFile) throws DSMException, XMDPException, PrismException, IOException,
	ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(missionJsonFile, CostCriterion.TOTAL_COST);
	}

	public void runPlanningWithRL(File missionJsonFile) throws XMDPException, DSMException, ResultParsingException, ExplicitModelParsingException, PrismException, IOException, GRBException, InterruptedException {
		MiniGridConnection conn = new MiniGridConnection(runPlanning(missionJsonFile));
		for (var i = 0; i < 100 && conn.hasNext(); i++) {
			conn.step();
			TimeUnit.SECONDS.sleep(1);
		}

		System.out.println("done!");
		TimeUnit.SECONDS.sleep(30);
		conn.close();
	}

	public static void runGenerator(File missionJsonFile, String prefix) throws Exception {
		File mapsJsonDir = new File(MAPS_PATH);
		Path policiesOutputPath = Paths.get(XPlannerOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlannerOutDirectories.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);
		ArrayList<String> states = new ArrayList<String>();
		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		MobileRobotXPlanner xplanner = new MobileRobotXPlanner(mapsJsonDir, outputDirs, defaultVerbalizerSettings);
		FileWriter writer = new FileWriter(prefix + missionJsonFile.getName() + ".csv");
		writer.flush();
		writer.close();

		//xplanner.runPlanningWithRL(missionJsonFile);
		//printToExcel(prefix + missionJsonFile.getName() + ".csv", inf, states);

		
		for (double i = 0; i <=1; i+=0.05) {
			for (double j = 0; j <= 1; j+=0.05) {
					double k = 1-i-j;
					try {
							PolicyInfo inf = xplanner.runPlanning(missionJsonFile, i, j, k);
							states = printToExcel(prefix + missionJsonFile.getName() + ".csv", inf, states);
					} catch (Exception e) {
						System.out.println(e);
						break;
					}
				}
		}
	}

	public static void main(String[] args)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
	      long start1 = System.nanoTime();

		String missionFilename = "mission64"; // or "mission64"
		File missionJsonFile = new File(MISSIONS_PATH, "small_map_mission.json");
		try {
			if (missionFilename.contains("small"))
				runGenerator(missionJsonFile, "./tmpdata/");
			else if (missionFilename.contains("mission64")) {
				for (int i = 0; i < 1; i++)
				runGenerator(new File(MISSIONS_PATH, "mission64-map" + i), "./tmpdata/");
			      long end1 = System.nanoTime();      
			      System.out.println("Elapsed Time in nano seconds: "+ (end1-start1));      
			}
			else if (missionFilename.contains("GHC")) {
				for (int i = 0; i < 2; i++)
				runGenerator(new File(MISSIONS_PATH, "GHC7-map" + i), "./tmpdata/");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<String> printToExcel(String fileName, PolicyInfo inf, ArrayList<String> states) {
		ArrayList<String> arrayList = new ArrayList<String>();
		int index = 10 + (int)(Math.random() * ((20 - 10) + 1));
		boolean addedFirstRow = false;

		LinkedHashMap<String, String> decisions = new LinkedHashMap<>();
		for (String s : states) {
			decisions.put(s, "");
		}

		Iterator<Decision> itr = inf.getPolicy().iterator();
		int numberSteps = 0;
		int numberMoveTo = 0;
		int numberAdjustSpeed = 0;
		int numberIncreaseSpeed = 0;
		int numberLowerSpeed = 0;

		while(itr.hasNext()) {
			Decision element = itr.next();
			String action= element.getAction().toString();
			String state = element.getState().toString();
			System.out.println(state);
			System.out.println(action);
			String speed = "";
			String loc = "";
			if (!state.startsWith("rLoc")) {
				Iterator<StateVar<IStateVarValue>> stateSpaceIterator = element.getState().iterator();
				while (stateSpaceIterator.hasNext()) {
					StateVar<IStateVarValue> next = stateSpaceIterator.next();
					if (next.getName().startsWith("rSpeed")) {
						speed = "rSpeed" + "=" + next.getValue();
					}
					if (next.getName().startsWith("rLoc")) {
						loc = "rLoc" + "=" + next.getValue();
					}
				}						
				state = loc + "," + speed;
			}
			numberSteps++;
			if (element.getAction().getNamePrefix().equals("moveTo"))
				numberMoveTo++;
			else if (element.getAction().getNamePrefix().equals("setSpeed")) {
				numberAdjustSpeed++;
				try {
					IStateVarValue i = element.getAction().getParameters().get(0);
					if (i.toString().equals("0.7"))
						numberIncreaseSpeed++;
					else
						numberLowerSpeed++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			String stateName = state.replace(",", "&").replace(".", "");
			if (stateName.equals(""))
				break;
			String actionName = action.replace(",", "&");
			if (decisions.get(stateName) == null || decisions.get(stateName).equals("")) {
				decisions.put(stateName, actionName);
			}
			else {
				decisions.put(stateName + index, actionName);
				index++;
			}
		}
		if (new ArrayList<>(decisions.keySet()).equals(states))
			addedFirstRow = true;
		else
			addedFirstRow = false;
		List<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> scalingQAs = new ArrayList<>();
		scalingQAs.addAll(inf.getXMDP().getCostFunction().getAttributeCostFunctions());
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> i : scalingQAs) { 
			String weight = Double.toString(Precision.round(inf.getXMDP().getCostFunction().getScalingConstant(i), 3));
			arrayList.add(weight);	
		}
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> i : scalingQAs) { 
			String name = i.getName();			
			double d = inf.getQAValue(i.getQFunction());
			arrayList.add(Double.toString(Precision.round(d, 3)));
		}
		
		arrayList.add(Double.toString(inf.getObjectiveCost()));
		arrayList.add(Integer.toString(numberSteps));
		arrayList.add(Integer.toString(numberMoveTo));
		arrayList.add(Integer.toString(numberAdjustSpeed));
		arrayList.add(Integer.toString(numberLowerSpeed));
		arrayList.add(Integer.toString(numberIncreaseSpeed));

		for (String s : arrayList) {
			System.out.println(s);
		}

		System.out.println(decisions);


		for (String v : decisions.keySet())
			if (decisions.get(v) != null)
				arrayList.add(decisions.get(v));
			else
				arrayList.add("");
		try {
			FileWriter file = new FileWriter(fileName, true);
			PrintWriter write = new PrintWriter(file);
			if (addedFirstRow == false) {
			    File myFile = new File(fileName);
			    ArrayList<String> fileContent = new ArrayList<String>();
			    Scanner myReader = new Scanner(myFile);
			    while (myReader.hasNextLine()) {
			        fileContent.add(myReader.nextLine());
			    }
			    myReader.close();
			    
				String newString = "";
				for (int i = 0; i < scalingQAs.size(); i++) {
					newString+=("w_"+scalingQAs.get(i).getName());
					newString+=(",");
				}
				for (int i = 0; i < scalingQAs.size(); i++) {
					newString+=(scalingQAs.get(i).getName());
					newString+=(",");
				}
				newString+=("Total Cost");
				newString+=(", NumberSteps");
				newString+=(", NumberMoveTo");
				newString+=(", NumberAdjustSpeed");
				newString+=(", NumberLowerSpeed");
				newString+=(", NumberIncreaseSpeed");
				for (String v : decisions.keySet()) {
					newString+=(",");
					newString+=(v);
				}
				if (fileContent.size() > 0) {
				    fileContent.remove(0);
				    fileContent.add(0, newString);
				}
				else
					fileContent.add(newString);
			    // Writes the new content to file
			    FileWriter myWriter = new FileWriter(fileName);
			    for (String eachLine : fileContent) {
			        myWriter.write(eachLine + "\n");
			    }
			    myWriter.close();
			    addedFirstRow = true;
			}
		
			for (String name: arrayList){
				write.append(name);
				if (arrayList.indexOf(name) != arrayList.size()-1)
				write.append(",");
			}
			
			write.println();
			write.close();
		}
		catch(IOException exe){
			System.out.println("Cannot create file");
		}		
		return new ArrayList<>(decisions.keySet());
	}

	public static Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(TravelTimeQFunction.NAME, "travel time");
		vocab.putVerb(TravelTimeQFunction.NAME, "take");
		vocab.putUnit(TravelTimeQFunction.NAME, "second", "seconds");
		vocab.putNoun(CollisionEvent.NAME, "collision");
		vocab.putVerb(CollisionEvent.NAME, "have");
		vocab.putUnit(CollisionEvent.NAME, "expected collision", "expected collisions");
		vocab.setOmitUnitWhenNounPresent(CollisionEvent.NAME);
		vocab.putNoun(IntrusiveMoveEvent.NAME, "intrusiveness");
		vocab.putVerb(IntrusiveMoveEvent.NAME, "be");
		vocab.putCategoricalValue(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.NON_INTRUSIVE_EVENT_NAME,
				"non-intrusive");
		vocab.putCategoricalValue(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.SOMEWHAT_INTRUSIVE_EVENT_NAME,
				"somewhat-intrusive");
		vocab.putCategoricalValue(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.VERY_INTRUSIVE_EVENT_NAME,
				"very-intrusive");
		vocab.putPreposition(IntrusiveMoveEvent.NAME, "at");
		vocab.putUnit(IntrusiveMoveEvent.NAME, "location", "locations");
		return vocab;
	}

	public static QADecimalFormatter getQADecimalFormatter() {
		QADecimalFormatter decimalFormatter = new QADecimalFormatter();
		decimalFormatter.putDecimalFormat(TravelTimeQFunction.NAME, "#");
		decimalFormatter.putDecimalFormat(CollisionEvent.NAME, "#.#");
		decimalFormatter.putDecimalFormat(IntrusiveMoveEvent.NAME, "#");
		return decimalFormatter;
	}

	public static void setVerbalizerOrdering(VerbalizerSettings verbalizerSettings) {
		verbalizerSettings.appendQFunctionName(TravelTimeQFunction.NAME);
		verbalizerSettings.appendQFunctionName(CollisionEvent.NAME);
		verbalizerSettings.appendQFunctionName(IntrusiveMoveEvent.NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.NON_INTRUSIVE_EVENT_NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.SOMEWHAT_INTRUSIVE_EVENT_NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.VERY_INTRUSIVE_EVENT_NAME);
	}

}