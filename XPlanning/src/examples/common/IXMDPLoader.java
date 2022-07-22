package examples.common;

import java.io.File;

import language.exceptions.XMDPException;
import language.mdp.XMDP;

public interface IXMDPLoader {
	public XMDP loadXMDP(File problemFile) throws DSMException, XMDPException;

	public XMDP loadXMDP(File problemFile, double w_travelTime, double w_intru, double w_collision) throws DSMException, XMDPException;

	public XMDP loadXMDP(File problemFile, int revenuePPatient, int overtimeCostPPatient, int idleTimeCostPPatient,
			int leadTimeCostFactor, int switchABPCostFactor);

	public XMDP loadXMDP(File file, double i, double j) throws XMDPException, DSMException;
}
