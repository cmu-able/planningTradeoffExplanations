package examples.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import explanation.analysis.PolicyInfo;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ResultParsingException;

public class UtilityWeightPlanner extends XPlanner {

	public UtilityWeightPlanner(IXMDPLoader xmdpLoader, XPlannerOutDirectories outputDirs, Vocabulary vocabulary,
			VerbalizerSettings verbalizerSettings) {
		super(xmdpLoader, outputDirs, vocabulary, verbalizerSettings);
	}

	
	public static PrismConnectorSettings createPrismConnectorSettings(File problemFile,
			XPlannerOutDirectories outputDirs) {
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		Path modelOutputPath = outputDirs.getPrismModelsOutputPath().resolve(problemName);
		Path advOutputPath = outputDirs.getPrismAdvsOutputPath().resolve(problemName);
		return new PrismConnectorSettings(modelOutputPath.toString(), advOutputPath.toString());
	}


	/**
	 * run planning method for Mobile Robot Planning
	 * @param the mission file
	 * @param totalCost cost criterion (currently only supported for total cost)
	 * @return
	 * @throws XMDPException 
	 * @throws DSMException 
	 * @throws PrismException 
	 * @throws IOException 
	 * @throws ResultParsingException 
	 */
	public PolicyInfo runPlanning(File missionJsonFile, CostCriterion totalCost, double w_travelTime, double w_intru,
			double w_collision) throws DSMException, XMDPException, PrismException, ResultParsingException, IOException {
		PrismConnectorSettings prismConnSettings = createPrismConnectorSettings(missionJsonFile, mOutputDirs);
		XMDP xmdp = mXMDPLoader.loadXMDP(missionJsonFile, w_travelTime, w_intru, w_collision);

		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM
		prismConnector.terminate();

		return policyInfo;
	}

	/**
	 * run planning method for DARTSim
	 * @param the mission file
	 * @param the first utility function weight
	 * @param the second utility function weight
	 * @param totalCost cost criterion (currently only supported for total cost)
	 * @return
	 * @throws XMDPException 
	 * @throws DSMException 
	 * @throws PrismException 
	 * @throws IOException 
	 * @throws ResultParsingException 
	 */
	public PolicyInfo runPlanning(File file, double i, double j, CostCriterion totalCost) throws DSMException, XMDPException, PrismException, ResultParsingException, IOException {
		PrismConnectorSettings prismConnSettings = createPrismConnectorSettings(file, mOutputDirs);
		XMDP xmdp = mXMDPLoader.loadXMDP(file, i, j);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM
		prismConnector.terminate();

		return policyInfo;
		
	}

	public PolicyInfo runPlanning(File problemFile, int revenuePPatient, int overtimeCostPPatient,
			int idleTimeCostPPatient, int leadTimeCostFactor, int switchABPCostFactor) throws DSMException, XMDPException, PrismException, ResultParsingException, IOException {
		PrismConnectorSettings prismConnSettings = createPrismConnectorSettings(problemFile, mOutputDirs);
		XMDP xmdp = mXMDPLoader.loadXMDP(problemFile, revenuePPatient, overtimeCostPPatient,
				idleTimeCostPPatient, leadTimeCostFactor, switchABPCostFactor);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM
		prismConnector.terminate();

		return policyInfo;

	}

}
