package br.unicamp.tutorialsumo.main;

import br.unicamp.cst.behavior.subsumption.SubsumptionAction;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.motivational.Drive;
import br.unicamp.cst.motivational.DriveLevel;
import br.unicamp.cst.motivational.Goal;
import br.unicamp.cst.motivational.GoalArchitecture;
import br.unicamp.tutorialsumo.agent.AgentMind;
import br.unicamp.tutorialsumo.codelets.actuators.TrafficLightActuator;
import br.unicamp.tutorialsumo.codelets.motivational.drives.TrafficDrive;
import br.unicamp.tutorialsumo.codelets.motivational.goals.LaneHelpGoal;
import br.unicamp.tutorialsumo.codelets.sensors.LaneSensor;
import br.unicamp.tutorialsumo.comunication.SingleAccessQuery;
import br.unicamp.tutorialsumo.constants.MemoryObjectName;
import br.unicamp.tutorialsumo.entity.TrafficLightLinkStatus;
import it.polito.appeal.traci.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by Du on 16/08/16.
 */
public class Main {

    private static int iSumoPort = 8091;
    private static int iServerPort = 4011;
    private static SumoTraciConnection conn;
    private static Process sumoProcess;
    private static String sPath;
    private static int iTotalStep = 0;
    private static int iTimeStep = 0;
    private static List<AgentMind> agentMinds;


    public static void main(String[] args) {

        if (args.length != 0) {
            setsPath(args[0]);
            setiTotalStep(Integer.parseInt(args[1]));
            setiTimeStep(Integer.parseInt(args[2]));
        }

        String[] sSumoArgs = new String[]{
                "sumo-gui",
                "-c", getsPath(),
                "--remote-port", Integer.toString(getiSumoPort()),
                "-S"
        };

        int iSteps = getiTotalStep() <= 0 ? 1000 : getiTotalStep();
        int iTimeStep = getiTimeStep() <= 0 ? 200 : getiTimeStep();

        try {
            setSumoProcess(Runtime.getRuntime().exec(sSumoArgs));
            conn = new SumoTraciConnection(InetAddress.getByName("127.0.0.1"), getiSumoPort());


            SingleAccessQuery.setConnection(conn);

            initTrafficAgent();

            for (int i = 0; i < iSteps; i++) {

                SingleAccessQuery.nextSimStep();

                Collection<Vehicle> vehicles = SingleAccessQuery.getNumOfVehicles();

                if (i == 0) {
                    for (int j = 0; j < getAgentMinds().size(); j++) {
                        getAgentMinds().get(j).start();
                    }
                }

                Thread.sleep(iTimeStep);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SingleAccessQuery.closeConnection();

        for (int j = 0; j < getAgentMinds().size(); j++) {
            getAgentMinds().get(j).shutDown();
        }

    }


    public static void initTrafficAgent() throws IOException {
        try {

            Map<String, TrafficLight> mapTrafficLights = SingleAccessQuery.getSumoTraciConnection().getTrafficLightRepository().getAll();

            setAgentMinds(Collections.synchronizedList(new ArrayList<AgentMind>()));

            for (Map.Entry<String, TrafficLight> trafficLightPairs : mapTrafficLights.entrySet()) {

                TrafficLight trafficLight = trafficLightPairs.getValue();

                AgentMind agent = new AgentMind(trafficLight.getID());

                List<Drive> lstOfDrives = Collections.synchronizedList(new ArrayList<Drive>());
                List<Goal> lstOfGoals = Collections.synchronizedList(new ArrayList<Goal>());

                List<Lane> incomingLanes = Collections.synchronizedList(new ArrayList<Lane>());
                List<Lane> outgoingLanes = Collections.synchronizedList(new ArrayList<Lane>());

                List<ControlledLink> lstOfControlledLink = Collections.synchronizedList(new ArrayList<ControlledLink>());

                ControlledLink[][] links = trafficLight.queryReadControlledLinks().get().getLinks();


                List<TrafficLightLinkStatus> lstOfLinkPhase = Collections.synchronizedList(new ArrayList<TrafficLightLinkStatus>());
                for (int i = 0; i < links.length; i++) {
                    for (int k = 0; k < links[i].length; k++) {
                        lstOfLinkPhase.add(new TrafficLightLinkStatus(i, LightState.RED, links[i][k]));
                    }
                }


                for (int i = 0; i < links.length; i++) {
                    for (int j = 0; j < links[i].length; j++) {
                        Lane incomingLane = links[i][j].getIncomingLane();
                        Lane outgoingLane = links[i][j].getOutgoingLane();

                        if (!outgoingLanes.contains(outgoingLane))
                            outgoingLanes.add(outgoingLane);

                        if (!incomingLanes.contains(incomingLane)) {
                            incomingLanes.add(incomingLane);

                            /*
                                Init Lane Sensor.
                             */
                            Codelet laneSensorInput = new LaneSensor(incomingLane, getiTimeStep());

                            MemoryObject occupancyMO = agent.createMemoryObject(MemoryObjectName.LANE_OCCUPANCY.toString(), 0);
                            MemoryObject vehiclesMO = agent.createMemoryObject(MemoryObjectName.LANE_VEHICLES_ID_LIST.toString(), new HashMap<Lane, List<String>>());
                            MemoryObject meanVelocityMO = agent.createMemoryObject(MemoryObjectName.LANE_MEAN_VELOCITY.toString(), 0);
                            MemoryObject maxVelocityMO = agent.createMemoryObject(MemoryObjectName.LANE_MAX_VELOCITY.toString(), 0);

                            laneSensorInput.addOutput(occupancyMO);
                            laneSensorInput.addOutput(vehiclesMO);
                            laneSensorInput.addOutput(meanVelocityMO);
                            laneSensorInput.addOutput(maxVelocityMO);

                            agent.insertCodelet(laneSensorInput);


                            /*
                                Init Traffic Drive for Lanes.
                            */
                            Drive trafficDrive = new TrafficDrive(incomingLane.getID(), DriveLevel.LOW_LEVEL, 0.7d, 0.9d);
                            trafficDrive.addInputs(laneSensorInput.getOutputs());
                            agent.insertCodelet(trafficDrive);

                            lstOfDrives.add(trafficDrive);

                            /*
                                Init Lane Goals.
                             */
                            List<Drive> lstOfDriveLaneGoal = Collections.synchronizedList(new ArrayList<Drive>());
                            lstOfDriveLaneGoal.add(trafficDrive);

                            List<Integer> lstOfIndexGreenWave = findLaneToGreenWave(links, incomingLane);

                            Goal goalLane = new LaneHelpGoal(incomingLane.getID(), getiTimeStep(), 80, 30, 0.75d, 0.0d, 0.7d, lstOfIndexGreenWave);

                            goalLane.addOutput(agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString(), "0"));
                            MemoryObject trafficLinkPhaseMO = agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString(), lstOfLinkPhase);
                            goalLane.addBroadcast(trafficLinkPhaseMO);

                            MemoryObject drivesLaneGoal = agent.createMemoryObject(Goal.DRIVES_VOTE_MEMORY, 0d);
                            drivesLaneGoal.setI(lstOfDriveLaneGoal);

                            goalLane.addInput(drivesLaneGoal);

                            SubsumptionAction trafficLightActuator = new TrafficLightActuator(trafficLight, getiTimeStep(), agent.getSubsumptionArchitecture());
                            MemoryObject changeTrafficLightPhaseMO = agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_MEMORY_CHANGED.toString());
                            trafficLightActuator.addOutput(changeTrafficLightPhaseMO);
                            goalLane.addSubsumptionAction(trafficLightActuator, goalLane);

                            lstOfGoals.add(goalLane);

                            agent.getSubsumptionArchitecture().addLayer(goalLane.getSubsumptionBehaviourLayer());

                            agent.insertCodelet(goalLane);

                        }

                        lstOfControlledLink.add(links[i][j]);

                    }
                }

                Random random = new Random();
                random.ints(0, lstOfGoals.size() - 1);
                lstOfGoals.get(random.nextInt((((lstOfGoals.size() - 1) - 0) + 1))).setActivation(0.7);


                GoalArchitecture goalArchitecture = new GoalArchitecture();
                MemoryObject drives = agent.createMemoryObject(GoalArchitecture.DRIVES_MEMORY, 0d);
                drives.setI(lstOfDrives);

                MemoryObject goals = agent.createMemoryObject(GoalArchitecture.GOALS_MEMORY, 0d);
                goals.setI(lstOfGoals);

                goalArchitecture.addInput(drives);
                goalArchitecture.addInput(goals);

                agent.setGoalArchitecture(goalArchitecture);
                agent.insertCodelet(agent.getGoalArchitecture());

                getAgentMinds().add(agent);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }


    }


    public static List<Integer> findLaneToGreenWave(ControlledLink[][] controlledLinks, Lane incomingLane) throws IOException {

        List<Integer> indexList = Collections.synchronizedList(new ArrayList<Integer>());
        List<Edge> edgeList = Collections.synchronizedList(new ArrayList<Edge>());
        List<Edge> edgesToOpen = Collections.synchronizedList(new ArrayList<Edge>());
        List<Edge> edgeOfLinks = new ArrayList<>();
        int indexOfIncomingEdge = 0;
        Edge currentEdge = null;


        for (int i = 0; i < controlledLinks.length; i++) {
            for (int j = 0; j < controlledLinks[i].length; j++) {
                Edge edge = controlledLinks[i][j].getIncomingLane().getParentEdge();
                edgeOfLinks.add(controlledLinks[i][j].getIncomingLane().getParentEdge());

                if (!edgeList.contains(edge)) {
                    edgeList.add(edge);
                }


                if (controlledLinks[i][j].getIncomingLane().getID().equals(incomingLane.getID()) && currentEdge == null) {
                    currentEdge = controlledLinks[i][j].getIncomingLane().getParentEdge();
                    indexOfIncomingEdge = edgeList.size() - 1;
                }
            }
        }

        for (int i = 0; i < edgeList.size(); i++) {
            if (i % 2 == 0) {
                if (indexOfIncomingEdge % 2 == 0) {
                    edgesToOpen.add(edgeList.get(i));
                }
            } else {
                if (indexOfIncomingEdge % 2 == 1) {
                    edgesToOpen.add(edgeList.get(i));
                }
            }
        }

        int indexCount = 0;
        for (Edge edge : edgeOfLinks) {
            if (edgesToOpen.contains(edge)) {
                indexList.add(indexCount);
            }
            indexCount++;
        }

        return indexList;
    }


    public static int getiSumoPort() {
        return iSumoPort;
    }

    public static String getsPath() {
        return sPath;
    }

    public static void setsPath(String sPath) {
        Main.sPath = sPath;
    }

    public static int getiTotalStep() {
        return iTotalStep;
    }

    public static void setiTotalStep(int iTotalStep) {
        Main.iTotalStep = iTotalStep;
    }

    public static int getiTimeStep() {
        return iTimeStep;
    }

    public static void setiTimeStep(int iTimeStep) {
        Main.iTimeStep = iTimeStep;
    }

    public static List<AgentMind> getAgentMinds() {
        return agentMinds;
    }

    public static void setAgentMinds(List<AgentMind> agentMinds) {
        Main.agentMinds = agentMinds;
    }

    public static Process getSumoProcess() {
        return sumoProcess;
    }

    public static void setSumoProcess(Process sumoProcess) {
        Main.sumoProcess = sumoProcess;
    }

    public void setSumoPort(int sumoPort) {
        this.iSumoPort = sumoPort;
    }

    public int getServerPort() {
        return iServerPort;
    }

    public void setServerPort(int serverPort) {
        this.iServerPort = serverPort;
    }
}
