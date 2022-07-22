package examples.dart.demo;

import java.io.File;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.dart.dsm.DartMission;
import examples.dart.dsm.DartMissionReader;
import examples.dart.dsm.DartXMDPBuilder;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class DartXMDPLoader implements IXMDPLoader {

	private DartXMDPBuilder mXMDPBuilder = new DartXMDPBuilder();
	private DartMissionReader mMissionReader = new DartMissionReader();

	public DartXMDPLoader() {
		// This constructor can take any DART domain configuration parameters as arguments
	}

	@Override
	public XMDP loadXMDP(File problemFile) throws DSMException, XMDPException {
		DartMission mission = mMissionReader.readDartMission(problemFile);
		return mXMDPBuilder.buildXMDP(mission);
	}

	@Override
	public XMDP loadXMDP(File problemFile, double d1, double d2, double d3) throws DSMException, XMDPException {
		// TODO Auto-generated method stub
		return loadXMDP(problemFile);
	}

	@Override
	public XMDP loadXMDP(File problemFile, int revenuePPatient, int overtimeCostPPatient, int idleTimeCostPPatient,
			int leadTimeCostFactor, int switchABPCostFactor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XMDP loadXMDP(File file, double i, double j) throws XMDPException, DSMException {
		DartMission mission = null;
		try {
			mission = mMissionReader.readDartMission(file, i, j);
		} catch (DSMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mXMDPBuilder.buildXMDP(mission);
	}

}
