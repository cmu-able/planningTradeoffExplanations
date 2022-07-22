package solver.prismconnector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.ModelType;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismSettings;
import prism.Result;
import solver.prismconnector.PrismConfiguration.PrismEngine;
import solver.prismconnector.PrismConfiguration.PrismMDPMultiSolutionMethod;
import solver.prismconnector.PrismConfiguration.PrismMDPSolutionMethod;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;

/**
 * References: https://github.com/prismmodelchecker/prism-api/blob/master/src/demos/MDPAdversaryGeneration.java,
 * https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/Prism.java
 * https://github.com/prismmodelchecker/prism/blob/master/prism/src/prism/PrismSettings.java
 * 
 * @author rsukkerd
 *
 */
public class PrismAPIWrapper {

	private static final String FLOATING_POINT_RESULT_PATTERN = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
	private static final String INFINITY_RESULT_PATTERN = "Infinity";
	private static final String NAN_RESULT_PATTERN = "NaN";

	private PrismConfiguration mPrismConfig;
	private Prism mPrism;

	public PrismAPIWrapper() throws PrismException {
		mPrismConfig = new PrismConfiguration(); // set to default PRISM configuration initially
		initializePrism();
	}

	private void initializePrism() throws PrismException {
		// Create a log for PRISM output (stdout)
		PrismLog mainLog = new PrismFileLog("stdout");

		// Initialize PRISM engine
		mPrism = new Prism(mainLog);
		mPrism.initialise();
	}

	/**
	 * Close down PRISM. Only invoke this method when finishing using this {@link PrismAPIWrapper}.
	 */
	public void terminatePrism() {
		mPrism.closeDown();
	}

	/**
	 * Configure PRISM for model-checking steady-state property.
	 */
	public void configureForSteadySteadProperty() {
		// Use Sparse engine for model-checking steady-state property
		mPrismConfig.setEngine(PrismEngine.SPARSE);
	}

	/**
	 * Configure PRISM for multi-objective strategy synthesis.
	 * 
	 * @param prodStaOutputFile
	 *            : Product states output file
	 * @throws PrismException
	 */
	public void configureForMultiObjectiveStrategySynthesis(File prodStaOutputFile) throws PrismException {
		// Export the product states of the generated adversary to a file
		mPrism.setExportProductStates(true);
		mPrism.setExportProductStatesFilename(prodStaOutputFile.getPath());

		// Use transition rewards for multi-objective adversary synthesis
		// PrismRewardTranslator already uses transition rewards

		// Use Sparse engine and linear-programming solution method for multi-objective adversary synthesis
		mPrismConfig.setEngine(PrismEngine.SPARSE);
		mPrismConfig.setMDPMultiSolutionMethod(PrismMDPMultiSolutionMethod.LINEAR_PROGRAMMING);
	}

	/**
	 * Export the explicit model files from a given PRISM MDP String. The explicit model files include: states file
	 * (.sta), transitions file (.tra), labels file (.lab), and either state rewards file (.srew) or transition rewards
	 * file (.trew).
	 * 
	 * @param mdpStr
	 *            : PRISM MDP String
	 * @param outputExplicitModelPointer
	 *            : Pointer to the directory to which to export the explicit model files
	 * @throws PrismException
	 * @throws FileNotFoundException
	 */
	public ModulesFile exportExplicitModelFiles(String mdpStr, PrismExplicitModelPointer outputExplicitModelPointer)
			throws PrismException, FileNotFoundException {
		File staOutputFile = outputExplicitModelPointer.getStatesFile();
		File traOutputFile = outputExplicitModelPointer.getTransitionsFile();
		File labOutputFile = outputExplicitModelPointer.getLabelsFile();
		PrismRewardType prismRewardType = outputExplicitModelPointer.getPrismRewardType();

		// Parse and load a PRISM MDP model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(mdpStr, ModelType.MDP);
		mPrism.loadPRISMModel(modulesFile);

		// Export the states of the model to a file (.sta)
		mPrism.exportStatesToFile(Prism.EXPORT_PLAIN, staOutputFile);

		// Export the transitions of the model to a file, in a row form (.tra)
		mPrism.exportTransToFile(true, Prism.EXPORT_ROWS, traOutputFile);

		// Export the labels (including "init" and "deadlock" -- these are important!) of the model to a file (.lab)
		mPrism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, labOutputFile);

		// Export the reward structure to a file
		// Note: This needs to be set after setting PRISM engine
		if (prismRewardType == PrismRewardType.STATE_REWARD) {
			// Export the state rewards to a file (.srew)
			File srewOutputFile = outputExplicitModelPointer.getStateRewardsFile();
			mPrism.exportStateRewardsToFile(Prism.EXPORT_PLAIN, srewOutputFile);
		} else if (prismRewardType == PrismRewardType.TRANSITION_REWARD) {
			// Export of transition rewards not yet supported by explicit engine
			switchEngineFromExplicitToSparse();

			// Export the transition rewards to a file (.trew)
			File trewOutputFile = outputExplicitModelPointer.getTransitionRewardsFile();
			mPrism.exportTransRewardsToFile(true, Prism.EXPORT_PLAIN, trewOutputFile);
		}

		return modulesFile;
	}

	/**
	 * Export the PRISM MDP model file (.mdp) -- for debugging purposes.
	 * 
	 * @param mdpStr
	 *            : : PRISM MDP String
	 * @param outputExplicitModelPointer
	 *            : PRISM MDP file will be in the same directory as the corresponding explicit model files
	 * @throws IOException
	 */
	public void exportMDPModelFile(String mdpStr, PrismExplicitModelPointer outputExplicitModelPointer)
			throws IOException {
		File mdpOutputFile = outputExplicitModelPointer.getMDPFile();
		FileWriter fileWriter = new FileWriter(mdpOutputFile);
		try (BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
			bufferedWriter.write(mdpStr);
		}
	}

	/**
	 * Generate an optimal adversary of a MDP in the form of an explicit model of DTMC. The output explicit model files
	 * include: states file (.sta), transitions file (.tra), labels file (.lab), state rewards file (.srew), and
	 * adversary file (adv.tra).
	 * 
	 * @param mdpStr
	 *            : MDP translation
	 * @param propertyStr
	 *            : Property containing a goal, a function minimization (which can be the cost function or other
	 *            objective function), and optionally a constraint
	 * @param outputPath
	 *            : Output directory for the explicit model files
	 * @return Expected total objective value of the generated optimal policy. If the value is infinity, it means that
	 *         the probability of reaching the goal is < 1. If the value is NaN, it means that there is no solution
	 *         found.
	 * @throws PrismException
	 * @throws FileNotFoundException
	 * @throws ResultParsingException
	 */
	public double generateMDPAdversary(String mdpStr, String propertyStr,
			PrismExplicitModelPointer outputExplicitModelPointer)
			throws PrismException, FileNotFoundException, ResultParsingException {
		File advOutputFile = outputExplicitModelPointer.getAdversaryFile();

		// Export explicit model files: .sta, .tra, .lab, and .srew/.trew
		ModulesFile modulesFile = exportExplicitModelFiles(mdpStr, outputExplicitModelPointer);

		// At this point, if the property is multi-objective strategy synthesis, then the method
		// configureForMultiObjectiveStrategySynthesis() is assumed to be invoked already.

		// Configure PRISM to export an optimal adversary to a file when model checking an MDP
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
		mPrism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, advOutputFile.getPath());

		// Select PRISM engine
		PrismEngine engine = mPrismConfig.getEngine();
		mPrism.getSettings().set(PrismSettings.PRISM_ENGINE, engine.toString());

		// Select PRISM MDP solution method
		PrismMDPSolutionMethod mdpSolutionMethod = PrismMDPSolutionMethod.POLICY_ITERATION;
		mPrism.getSettings().set(PrismSettings.PRISM_MDP_SOLN_METHOD, mdpSolutionMethod.toString());

		// Select PRISM MDP solution method for multi-objective properties
		PrismMDPMultiSolutionMethod mdpMultiSolutionMethod = mPrismConfig.getMDPMultiSolutionMethod();
		mPrism.getSettings().set(PrismSettings.PRISM_MDP_MULTI_SOLN_METHOD, mdpMultiSolutionMethod.toString());

		try {
			return queryPropertyHelper(modulesFile, propertyStr, 0);
		} catch (PrismException e) {
			// This maybe because the selected MDP solution method (Value iteration or Gauss-Seidel) did not converge
			// within 10000 iterations.
			// Change PRISM MDP solution method to "Policy iteration"
			PrismMDPSolutionMethod policyIteration = PrismMDPSolutionMethod.POLICY_ITERATION;
			mPrism.getSettings().set(PrismSettings.PRISM_MDP_SOLN_METHOD, policyIteration.toString());
			return queryPropertyHelper(modulesFile, propertyStr, 0);
		}
	}

	/**
	 * Query quantitative property of a DTMC -- from the explicit model files.
	 * 
	 * @param rawRewardPropertyStr
	 *            : Raw reward query property
	 * @param explicitModelPointer
	 *            : Pointer to the explicit model
	 * @param rewardStructIndex
	 *            : Index of the reward structure representing the quantity to be queried
	 * @return Quantitative result of the given query property of the DTMC
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double queryPropertyFromExplicitDTMC(String rawRewardPropertyStr,
			PrismExplicitModelPointer explicitModelPointer, int rewardStructIndex)
			throws PrismException, ResultParsingException {
		File staFile = explicitModelPointer.getStatesFile();
		File advFile = explicitModelPointer.getAdversaryFile();
		File labFile = explicitModelPointer.getLabelsFile();
		// Only .srew are allowed to be loaded from explicit files
		File srewFile = explicitModelPointer.getIndexedStateRewardsFile(rewardStructIndex);

		// Load modules from .sta, adv.tra, .lab, and .srew files (.lab file contains at least "init" and "deadlock"
		// labels -- important!)
		mPrism.loadModelFromExplicitFiles(staFile, advFile, labFile, srewFile, ModelType.DTMC);
		ModulesFile modulesFile = new ModulesFile();
		try {
			modulesFile = mPrism.parseModelFile(explicitModelPointer.getExplicitModelDirectory());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PrismLangException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return queryPropertyHelper(modulesFile, rawRewardPropertyStr, 0);
	}

	/**
	 * Query multiple quantitative properties of a DTMC -- from the explicit model files.
	 * 
	 * @param rawRewardPropertyStr
	 *            : Raw reward query property
	 * @param explicitModelPointer
	 *            : Pointer to the explicit model
	 * @return List of the results in the order of the .srew input filenames
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public List<Double> queryPropertiesFromExplicitDTMC(String rawRewardPropertyStr,
			PrismExplicitModelPointer explicitModelPointer) throws PrismException, ResultParsingException {
		List<Double> results = new ArrayList<>();
		for (int i = 1; i <= explicitModelPointer.getNumRewardStructs(); i++) {
			double result = queryPropertyFromExplicitDTMC(rawRewardPropertyStr, explicitModelPointer, i);
			results.add(result);
		}
		return results;
	}

	/**
	 * Query quantitative property of a DTMC -- from the model and property strings.
	 * 
	 * @param dtmcModelStr
	 *            : DTMC translation
	 * @param propertyStr
	 *            : Single property to be queried
	 * @return Quantitative result of the given query property of the DTMC
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double queryPropertyFromDTMC(String dtmcModelStr, String propertyStr)
			throws PrismException, ResultParsingException {
		// Parse and load a PRISM DTMC model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(dtmcModelStr, ModelType.DTMC);
		mPrism.loadPRISMModel(modulesFile);

		// PrismRewardTranslator only uses transition rewards
		// Explicit engine does not yet handle transition rewards for D/CTMCs
		switchEngineFromExplicitToSparse();

		return queryPropertyHelper(modulesFile, propertyStr, 0);
	}

	/**
	 * Query multiple quantitative properties of a DTMC -- from the model and properties strings.
	 * 
	 * @param dtmcModelStr
	 *            : DTMC translation
	 * @param propertiesStr
	 *            : Multiple properties to be queried
	 * @return Mapping from each property to the result
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public Map<String, Double> queryPropertiesFromDTMC(String dtmcModelStr, String propertiesStr)
			throws PrismException, ResultParsingException {
		// Parse and load a PRISM DTMC model from a model string
		ModulesFile modulesFile = mPrism.parseModelString(dtmcModelStr, ModelType.DTMC);
		mPrism.loadPRISMModel(modulesFile);

		// PrismRewardTranslator only uses transition rewards
		// Explicit engine does not yet handle transition rewards for D/CTMCs
		switchEngineFromExplicitToSparse();

		return queryPropertiesHelper(modulesFile, propertiesStr);
	}

	private Map<String, Double> queryPropertiesHelper(ModulesFile modulesFile, String propertiesStr)
			throws PrismException, ResultParsingException {
		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertiesStr);

		// Get number of properties
		int numProperties = propertiesFile.getNumProperties();

		Map<String, Double> results = new HashMap<>();
		String[] propertiesArray = propertiesStr.split("\n");

		// Query result of each property
		for (int i = 0; i < numProperties; i++) {
			String propertyStr = propertiesArray[i];
			double result = queryPropertyHelper(propertiesFile, i);
			results.put(propertyStr, result);
		}

		return results;
	}

	private double queryPropertyHelper(ModulesFile modulesFile, String propertiesStr, int propertyIndex)
			throws PrismException, ResultParsingException {
		// Parse and load property from a property string
		PropertiesFile propertiesFile = mPrism.parsePropertiesString(modulesFile, propertiesStr);
		return queryPropertyHelper(propertiesFile, propertyIndex);
	}

	private double queryPropertyHelper(PropertiesFile propertiesFile, int propertyIndex)
			throws PrismException, ResultParsingException {
		// Model check the property at the given index
		Property property = propertiesFile.getPropertyObject(propertyIndex);
		Result result = mPrism.modelCheck(propertiesFile, property);

		// Parse result double from result string
		// The result string may be a floating-point value, "Infinity", or "NaN"
		String resultStr = result.getResultString();
		Pattern valuePattern = Pattern.compile(FLOATING_POINT_RESULT_PATTERN);
		Pattern infinityPattern = Pattern.compile(INFINITY_RESULT_PATTERN);
		Pattern nanPattern = Pattern.compile(NAN_RESULT_PATTERN);
		Matcher valueMatcher = valuePattern.matcher(resultStr);
		Matcher infinityMatcher = infinityPattern.matcher(resultStr);
		Matcher nanMatcher = nanPattern.matcher(resultStr);

		if (valueMatcher.find()) {
			return Double.parseDouble(valueMatcher.group(0));
		}
		if (infinityMatcher.find()) {
			// The probability of reaching goal is < 1
			return Double.POSITIVE_INFINITY;
		}
		if (nanMatcher.find()) {
			// Using linear programming solution method: LP problem solution not found
			return Double.NaN;
		}
		throw new ResultParsingException(resultStr, FLOATING_POINT_RESULT_PATTERN, INFINITY_RESULT_PATTERN,
				NAN_RESULT_PATTERN);
	}

	private void switchEngineFromExplicitToSparse() throws PrismException {
		if (mPrismConfig.getEngine() == PrismEngine.EXPLICIT) {
			// Switch to sparse engine
			PrismEngine sparseEngine = PrismEngine.SPARSE;
			mPrism.getSettings().set(PrismSettings.PRISM_ENGINE, sparseEngine.toString());
		}
	}
}
