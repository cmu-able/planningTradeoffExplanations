package examples.dart.demo;

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

import org.apache.commons.math3.util.Precision;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.UtilityWeightPlanner;
import examples.common.XPlannerOutDirectories;
import examples.dart.metrics.DestroyedProbabilityQFunction;
import examples.dart.metrics.MissTargetEvent;
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

public class DartXPlanner {

	private static final String PROBLEMS_PATH = "./data/dart/missions";

	private UtilityWeightPlanner mXPlanner;

	public DartXPlanner(XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new DartXMDPLoader();
		mXPlanner = new UtilityWeightPlanner(xmdpLoader, outputDirs, getVocabulary(), verbalizerSettings);
	}

	public PolicyInfo runXPlanning(File problemFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return mXPlanner.runXPlanning(problemFile, CostCriterion.TOTAL_COST);
	}

	public PolicyInfo runPlanning(File problemFile) throws DSMException, XMDPException, PrismException, IOException,
			ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(problemFile, CostCriterion.TOTAL_COST);
	}

	public static void main(String[] args)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		String problemFilename = args[0];

		//File missionJsonFile = new File(MISSIONS_PATH, missionFilename);
		try {
			for (int i = 0; i < 1; i++)
				runGenerator(new File(PROBLEMS_PATH, problemFilename));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	private static void runGenerator(File file) throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		// TODO Auto-generated method stub
		Path policiesOutputPath = Paths.get(XPlannerOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlannerOutDirectories.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		DartXPlanner xplanner = new DartXPlanner(outputDirs, defaultVerbalizerSettings);
		ArrayList<String> states = new ArrayList<String>();
	
		FileWriter writer = new FileWriter("tmpdata/runDart/sample_" + file.getName() + ".csv");
		writer.flush();
		writer.close();
		int rowNumber = 0;
		for (double i = 0; i <= 1; i+=0.005) { // 34. is start value
				try {
						double j = 1-i;
						if (i + j == 1) {
						PolicyInfo inf = xplanner.runPlanning(file, i, j);
						states = printToExcel("tmpdata/runDart/sample_" + file.getName() + ".csv", inf, states, rowNumber++);
						}
					} catch (Exception e) {
						break;
					}
			}

		xplanner.runXPlanning(file);
	}

	private PolicyInfo runPlanning(File file, double i, double j) throws DSMException, XMDPException, PrismException, IOException,
	ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(file, i, j, CostCriterion.TOTAL_COST);
	}

	private static ArrayList<String> printToExcel(String fileName, PolicyInfo inf, ArrayList<String> states, int rowNumber) {
		ArrayList<String> arrayList = new ArrayList<String>();
		int index = 10 + (int)(Math.random() * ((20 - 10) + 1));
		int number = 0;
		//myStates.addAll(states);
		LinkedHashMap<String, String> decisions = new LinkedHashMap<>();
		for (String s : states) {
			decisions.put(s, "");
		}

		Iterator<Decision> itr = inf.getPolicy().iterator();
		/**StateVarTuple s = inf.getXMDP().getInitialState();
		StateSpace st = inf.getXMDP().getStateSpace();
		for (StateVarDefinition l : st) {
			l.getPossibleValues();
		}*/
		int numberSteps = 0;
		int numberInc = 0;
		int numberDec = 0;
		int numberTick = 0;
		int numberFly = 0;

		while(itr.hasNext()) {
			Decision element = itr.next();
			String action= element.getAction().toString();
			String state = element.getState().toString();
			numberSteps++;
			switch (element.getAction().getNamePrefix()) {
				case "fly":
				numberFly++;
				break;
				case "incAlt":
				numberInc++;
				break;
				case "decAlt":
					numberDec++;
					break;
				case "tick":
					numberTick++;
					break;
			}
			//for (IStateVarValue i : element.getAction().getParameters())
			//	i.getAttributeValue(fileName)
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
			number = 1;
		else
			number = 0;
		List<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> scalingQAs = new ArrayList<>();
		scalingQAs.addAll(inf.getXMDP().getCostFunction().getAttributeCostFunctions());
		arrayList.add(""+rowNumber);
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> i : scalingQAs) { 
			String weight = Double.toString(Precision.round(inf.getXMDP().getCostFunction().getScalingConstant(i), 3));
			arrayList.add(weight);	
		}
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> i : scalingQAs) { 
			double d = inf.getQAValue(i.getQFunction());
			arrayList.add(Double.toString(Precision.round(d, 3)));
		}
		arrayList.add(Double.toString(inf.getObjectiveCost()));
		arrayList.add(Integer.toString(numberSteps));
		arrayList.add(Integer.toString(numberFly));
		arrayList.add(Integer.toString(numberInc));
		arrayList.add(Integer.toString(numberDec));
		arrayList.add(Integer.toString(numberTick));

		for (String v : decisions.keySet())
			if (decisions.get(v) != null)
				arrayList.add(decisions.get(v));
			else
				arrayList.add("");
		try {
			FileWriter file = new FileWriter(fileName, true);
			PrintWriter write = new PrintWriter(file);
			if (number == 0) {
			    File myFile = new File(fileName);
			    // An array to store each line in the file
			    ArrayList<String> fileContent = new ArrayList<String>();
			    Scanner myReader = new Scanner(myFile);
			    while (myReader.hasNextLine()) {
			        // Reads the file content into an array
			        fileContent.add(myReader.nextLine());
			    }
			    myReader.close();
			    
				String newString = ",";
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
				newString+=(", NumberFly");
				newString+=(", NumberIncAlt");
				newString+=(", NumberDecAlt");
				newString+=(", NumberTick");
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
				number = 1;
			}
		
			for (String name: arrayList){
				write.append(name);
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
		// E[#targets missed]
		vocab.putNoun(MissTargetEvent.NAME, "expected number of targets missed");
		vocab.putVerb(MissTargetEvent.NAME, "miss");
		vocab.putUnit(MissTargetEvent.NAME, "expected target", "expected targets");
		vocab.setOmitUnitWhenNounPresent(MissTargetEvent.NAME);

		// Prob(being destroyed)
		vocab.putNoun(DestroyedProbabilityQFunction.NAME, "probability of being destroyed");
		vocab.putVerb(DestroyedProbabilityQFunction.NAME, "have");
		vocab.putUnit(DestroyedProbabilityQFunction.NAME, "probaility of being destroyed", null);
		vocab.setOmitUnitWhenNounPresent(DestroyedProbabilityQFunction.NAME);
		return vocab;
	}
}
