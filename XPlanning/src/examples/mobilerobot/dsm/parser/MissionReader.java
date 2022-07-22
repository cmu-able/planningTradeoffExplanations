package examples.mobilerobot.dsm.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Mission;
import examples.mobilerobot.dsm.PreferenceInfo;
import uiconnector.JSONSimpleParserUtils;

public class MissionReader {

	private JSONParser mParser = new JSONParser();

	public Mission readMission(File missionJsonFile, double a, double b, double c) throws IOException, ParseException {
		FileReader reader = new FileReader(missionJsonFile);
		Object object = mParser.parse(reader);
		JSONObject jsonObject = (JSONObject) object;

		String startNodeID = (String) jsonObject.get("start-id");
		String goalNodeID = (String) jsonObject.get("goal-id");
		String mapJsonFilename = (String) jsonObject.get("map-file");
		JSONArray prefInfoJsonArray = (JSONArray) jsonObject.get("preference-info");
		PreferenceInfo prefInfo = readPreferenceInfo(prefInfoJsonArray, a, b, c);
		return new Mission(startNodeID, goalNodeID, mapJsonFilename, prefInfo);
	}
	public Mission readMission(File missionJsonFile) throws IOException, ParseException {
		FileReader reader = new FileReader(missionJsonFile);
		Object object = mParser.parse(reader);
		JSONObject jsonObject = (JSONObject) object;

		String startNodeID = (String) jsonObject.get("start-id");
		String goalNodeID = (String) jsonObject.get("goal-id");
		String mapJsonFilename = (String) jsonObject.get("map-file");
		JSONArray prefInfoJsonArray = (JSONArray) jsonObject.get("preference-info");
		PreferenceInfo prefInfo = readPreferenceInfo(prefInfoJsonArray);
		return new Mission(startNodeID, goalNodeID, mapJsonFilename, prefInfo);
	}
	/**
	 * set preference info. Scaling costs will be value a for the first,
	 * value b for the second, and 1-a-b for the third.
	 * @param prefInfoJsonArray
	 * @param a
	 * @param b
	 * @return
	 */
	private PreferenceInfo readPreferenceInfo(JSONArray prefInfoJsonArray, double a, double b, double c) {
		PreferenceInfo prefInfo = new PreferenceInfo();
		double scaling = a;
		int i = 0;
		for (Object obj : prefInfoJsonArray) {
			JSONObject jsonObject = (JSONObject) obj;
			String qaName = (String) jsonObject.get("objective");
			double minStepValue = JSONSimpleParserUtils.parseDouble(jsonObject, "min-step-value");
			double maxStepValue = JSONSimpleParserUtils.parseDouble(jsonObject, "max-step-value");
			//double scalingConst = JSONSimpleParserUtils.parseDouble(jsonObject, "scaling-const");
			prefInfo.putMinStepQAValue(qaName, minStepValue);
			prefInfo.putMaxStepQAValue(qaName, maxStepValue);
			prefInfo.putScalingConst(qaName, scaling);
			if (i == 0)
				scaling = b;
			else
				scaling = c;
			i++;
		}
		return prefInfo;
	}
	
	private PreferenceInfo readPreferenceInfo(JSONArray prefInfoJsonArray) {
		PreferenceInfo prefInfo = new PreferenceInfo();
		for (Object obj : prefInfoJsonArray) {
			JSONObject jsonObject = (JSONObject) obj;
			String qaName = (String) jsonObject.get("objective");
			double minStepValue = JSONSimpleParserUtils.parseDouble(jsonObject, "min-step-value");
			double maxStepValue = JSONSimpleParserUtils.parseDouble(jsonObject, "max-step-value");
			double scalingConst = JSONSimpleParserUtils.parseDouble(jsonObject, "scaling-const");
			prefInfo.putMinStepQAValue(qaName, minStepValue);
			prefInfo.putMaxStepQAValue(qaName, maxStepValue);
			prefInfo.putScalingConst(qaName, scalingConst);
		}
		return prefInfo;
	}
}
