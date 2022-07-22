It is easiest to use Eclipse. Install a current version and import the project using the import wizard. Click File > Import > General. Click on Existing Projects into Workspace and select a directory that you cloned with git.

# explainable-planning

To run MobileRobotDemo:

- Compile MobileRobotXPlanner.java under /XPlanning/src/examples/mobilerobot/demo/.

- The mission files are located under /XPlanning/data/mobilerobot/missions/. Each missionX.json references a corresponding mapX.json under /XPlanning/data/mobilerobot/maps/.

- Once the MobileRobot's execution completes, it will generate several output files under /XPlanning/tmpdata/. A CSV file is generated that includes one row per generated policy (including the utility function weights, the costs, and the policy actions).

# Environment variables/required config to run XPlanning in Eclipse:
Env. variable:
When running MobileRobotXPlanner, you might get an error "java.lang.UnsatisfiedLinkError: no prism in java.library.path"
Right-click and select Run —> Run Configurations (or Debug -> Debug Configurations). Click on the tab `Environment'.
Set "DYLD_LIBRARY_PATH" to ":lib". If you are on Linux, use "LD_LIBRARY_PATH".
If you are on Windows, use "PATH" and set it to ";lib".

![environment_variable](https://user-images.githubusercontent.com/22395693/146450210-bf9a11f4-6026-46f6-a4b7-01ad53343257.png)
