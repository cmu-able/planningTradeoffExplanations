package examples.mobilerobot.demo;

import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.RobotSpeed;
import examples.mobilerobot.models.SetSpeedAction;
import explanation.analysis.PolicyInfo;
import language.domain.models.IAction;
import language.domain.models.StateVarDefinition;
import language.exceptions.ActionNotFoundException;
import language.exceptions.StateNotFoundException;
import language.mdp.StateVarTuple;
import language.policy.Policy;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.util.Optional;
import java.util.function.Predicate;

public final class MiniGridConnection {

    private final StateVarTuple mCurrent;
    private final StateVarTuple mGoal;

    private Optional<Optional<IAction>> mNextAction = Optional.empty();

    private final Policy mPolicy;

    private final StateVarDefinition<RobotSpeed> mrSpeedDef;
    private final StateVarDefinition<Location> mrLocDef;

    private final int MAGIC = 104;
    private final BufferedReader mPyReader;
    private final BufferedWriter mPyWriter;

    private static final String[] cmd = {
            "venv/bin/python3",
            "-m", "scripts.serve",
            "--env", "Seams-InteractiveEnv",
            "--model", "storage/room2room",
            "--seed", "1337"
    };

    public MiniGridConnection(PolicyInfo policyInfo) throws IOException {
        mPolicy = policyInfo.getPolicy();

        var xmdp = policyInfo.getXMDP();

        mCurrent = xmdp.getInitialState();
        mGoal = xmdp.getGoal();

        var stateSpace = xmdp.getStateSpace();

        mrSpeedDef = stateSpace.getStateVarDefinition("rSpeed");
        mrLocDef = stateSpace.getStateVarDefinition("rLoc");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("../../seams22-minigridenv"));
        pb.redirectErrorStream(true);
        Process mPyProcess = pb.start();
        mPyReader = new BufferedReader(new InputStreamReader(mPyProcess.getInputStream()));
        mPyWriter = new BufferedWriter(new OutputStreamWriter(mPyProcess.getOutputStream()));

        readNextObjectUntil(o -> testValueAtKey(o, "type", String.class, it -> it.equals("ready")));
    }

    private Optional<IAction> findNextAction() {
        if (mNextAction.isPresent()) return mNextAction.get();
        for (var decision : mPolicy) {
            if (decision.getState().equals(mCurrent)) {
                mNextAction = Optional.of(Optional.of(decision.getAction()));
                return mNextAction.get();
            }
        }
        mNextAction = Optional.of(Optional.empty());
        return mNextAction.get();
    }

    public boolean hasFinished() {
        return mCurrent.contains(mGoal);
    }

    public boolean hasNext() {
        return !hasFinished() && findNextAction().isPresent();
    }

    public void step() throws StateNotFoundException, ActionNotFoundException, IOException {
        var action = findNextAction();
        if (action.isEmpty()) throw new StateNotFoundException(mCurrent);
        step(action.get());
        mNextAction = Optional.empty();
    }

    private void step(IAction action) throws ActionNotFoundException, IOException {
        if (action instanceof SetSpeedAction) {
            setSpeed((SetSpeedAction) action);
        } else if (action instanceof MoveToAction) {
            moveTo((MoveToAction) action);
        } else {
            throw new ActionNotFoundException(action);
        }
    }

    private void setSpeed(SetSpeedAction action) {
        mCurrent.addStateVar(mrSpeedDef.getStateVar(action.getTargetSpeed()));
    }

    private void moveTo(MoveToAction action) throws IOException {
        // mCurrent.addStateVar(mrLocDef.getStateVar(action.getDestination()));
        var cmd = String.format("goto %s", action.getDestination().getId().charAt(1));
        var o = sendCommand(cmd);
        if (testValueAtKey(o, "type", String.class, it -> it.equals("success")))
            updateRoom();
        else if (testValueAtKey(o, "type", String.class, it -> it.equals("failure")))
            throw new IllegalStateException("Movement failed.\n" + o);
        else
            throw new IOException("Unexpected response.\n" + o);
    }

    private void updateRoom() throws IOException {
        var o = sendCommand("room");
        if (testValueAtKey(o, "type", String.class, it -> it.equals("success"))) {
            var p = o.get("payload");
            if (p != null) {
                var roomNum = p.toString();
                for (var val : mrLocDef.getPossibleValues()) {
                    if (val.getId().substring(1).equals(roomNum)) {
                        mCurrent.addStateVar(mrLocDef.getStateVar(val));
                        return;
                    }
                }
            }
        }

        throw new IOException("Unexpected response.\n" + o);
    }

    private Optional<JSONObject> readNextObject() throws IOException {
       String line;
       while ((line = mPyReader.readLine()) != null) {
           var v = JSONValue.parse(line);
           if (!(v instanceof JSONObject)) continue;

           var o = (JSONObject) v;
           if (!testValueAtKey(o, "magic", Integer.class, it -> it.equals(MAGIC)))
               return Optional.of(o);
       }

       return Optional.empty();
    }

    private Optional<JSONObject> readNextObjectUntil(Predicate<JSONObject> p) throws IOException {
        Optional<JSONObject> o;
        while ((o = readNextObject()).isPresent()) {
            if (p.test(o.get())) return o;
        }
        return Optional.empty();
    }

    private JSONObject sendCommand(String cmd) throws IOException {
        mPyWriter.write(cmd);
        mPyWriter.newLine();
        mPyWriter.flush();
        return readNextObject().orElseThrow(() -> new IOException("Unexpected end of input"));
    }

    private static <T> boolean testValueAtKey(JSONObject o, String k, Class<T> clazz, Predicate<T> p) {
        if (!o.containsKey(k)) return false;
        var v = o.get(k);
        return clazz.isInstance(v) && p.test(clazz.cast(v));
    }

    public void close() throws IOException {
        mPyWriter.write("exit");
        mPyReader.close();
        mPyWriter.close();
    }
}
