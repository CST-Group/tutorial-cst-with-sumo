/*******************************************************************************
 * Copyright (c) 2016  DCA-FEEC-UNICAMP
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * E. M. Froes, R. R. Gudwin - initial API and implementation
 ******************************************************************************/

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
 * The class Main is the principal class of tutorial CST-SUMO. This class is responsible for control the simulation cycle.
 *
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


    /**
     * Principal Method
     * @param args Map file path and port
     */
    public static void main(String[] args) {


        /**
         *  Getting parameters from args.
         */
        if (args.length != 0) {
            setsPath(args[0]);
            setiTotalStep(Integer.parseInt(args[1]));
            setiTimeStep(Integer.parseInt(args[2]));
        }

        /**
         * Starting SUMO application.
         */
        String[] sSumoArgs = new String[]{
                "sumo-gui",
                "-c", getsPath(),
                "--remote-port", Integer.toString(getiSumoPort()),
                "-S"
        };

        /**
         * Defining amount of steps and step time.
         */
        int iSteps = getiTotalStep() <= 0 ? 1000 : getiTotalStep();
        int iTimeStep = getiTimeStep() <= 0 ? 200 : getiTimeStep();

        /**
         * Starting SUMO connection and simulation cycle.
         */
        try {
            setSumoProcess(Runtime.getRuntime().exec(sSumoArgs));

            /**
             * Starting connection with Sumo Traffic Simulation.
             */
            conn = new SumoTraciConnection(InetAddress.getByName("127.0.0.1"), getiSumoPort());
            SingleAccessQuery.setConnection(conn);

            /**
             * Starting Agents.
             */
            startTrafficAgent();

            for (int i = 0; i < iSteps; i++) {

                SingleAccessQuery.nextSimStep();

                //Collection<Vehicle> vehicles = SingleAccessQuery.getNumOfVehicles();

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


        /**
         * Closing SUMO connection and shotdown agents.
         */
        SingleAccessQuery.closeConnection();

        for (int j = 0; j < getAgentMinds().size(); j++) {
            getAgentMinds().get(j).shutDown();
        }

    }

    /**
     * This method is responsible for build the agent structure with sensors, actuators, drives and goals.
     * For instancing this objects, the application needs use the TraCI4J to get objects which give statistic information about trafficlights, lanes and vehicles.
     * In this context, TraCI4J is a proxy that performing a bridge between application and SUMO Simulation Server.
     */
    public static void startTrafficAgent() {
        try {

            /**
             * Getting trafficlights and starting agent list.
             */
            Map<String, TrafficLight> mapTrafficLights = SingleAccessQuery.getSumoTraciConnection().getTrafficLightRepository().getAll();
            setAgentMinds(Collections.synchronizedList(new ArrayList<AgentMind>()));

            /**
             * Loop of all trafficlights.
             */
            for (Map.Entry<String, TrafficLight> trafficLightPairs : mapTrafficLights.entrySet()) {

                TrafficLight trafficLight = trafficLightPairs.getValue();

                AgentMind agent = new AgentMind(trafficLight.getID());

                /**
                 * Starting drive and goal list.
                 */
                List<Drive> lstOfDrives = Collections.synchronizedList(new ArrayList<Drive>());
                List<Goal> lstOfGoals = Collections.synchronizedList(new ArrayList<Goal>());

                /**
                 * Starting incoming and outgoing lane list.
                 */
                List<Lane> incomingLanes = Collections.synchronizedList(new ArrayList<Lane>());
                List<Lane> outgoingLanes = Collections.synchronizedList(new ArrayList<Lane>());


                /**
                 * Starting controlled link list. Controlled links are connections between lanes in a cross.
                 */
                List<ControlledLink> lstOfControlledLink = Collections.synchronizedList(new ArrayList<ControlledLink>());

                /**
                 * Getting controlled links from traffic light.
                 */
                ControlledLink[][] links = trafficLight.queryReadControlledLinks().get().getLinks();

                /**
                 * Starting Traffic Light Phase. This list is responsible for associate lanes with respective traffic light.
                 */
                List<TrafficLightLinkStatus> lstOfLinkPhase = Collections.synchronizedList(new ArrayList<TrafficLightLinkStatus>());
                for (int i = 0; i < links.length; i++) {
                    for (int k = 0; k < links[i].length; k++) {
                        lstOfLinkPhase.add(new TrafficLightLinkStatus(i, LightState.RED, links[i][k]));
                    }
                }

                for (int i = 0; i < links.length; i++) {
                    for (int j = 0; j < links[i].length; j++) {

                        /**
                         * Getting incoming and outgoing lanes from traffic light.
                         */
                        Lane incomingLane = links[i][j].getIncomingLane();
                        Lane outgoingLane = links[i][j].getOutgoingLane();

                        if (!outgoingLanes.contains(outgoingLane))
                            outgoingLanes.add(outgoingLane);

                        if (!incomingLanes.contains(incomingLane)) {
                            incomingLanes.add(incomingLane);

                            /**
                             * Starting Lane Sensor.
                             */
                            Codelet laneSensorInput = new LaneSensor(incomingLane, getiTimeStep());

                            /**
                             * Lane Sensor Memories.
                             */
                            MemoryObject occupancyMO = agent.createMemoryObject(MemoryObjectName.LANE_OCCUPANCY.toString(), 0);
                            MemoryObject vehiclesMO = agent.createMemoryObject(MemoryObjectName.LANE_VEHICLES_ID_LIST.toString(), new HashMap<Lane, List<String>>());
                            MemoryObject meanVelocityMO = agent.createMemoryObject(MemoryObjectName.LANE_MEAN_VELOCITY.toString(), 0);
                            MemoryObject maxVelocityMO = agent.createMemoryObject(MemoryObjectName.LANE_MAX_VELOCITY.toString(), 0);

                            laneSensorInput.addOutput(occupancyMO);
                            laneSensorInput.addOutput(vehiclesMO);
                            laneSensorInput.addOutput(meanVelocityMO);
                            laneSensorInput.addOutput(maxVelocityMO);

                            agent.insertCodelet(laneSensorInput);


                            /**
                             *  Starting Traffic Drive for Lanes.
                             */
                            Drive trafficDrive = new TrafficDrive(incomingLane.getID(), DriveLevel.LOW_LEVEL, 0.7d, 0.9d);

                            /**
                             * Output lanes memories are input memories in Traffic Drive.
                             */
                            trafficDrive.addInputs(laneSensorInput.getOutputs());
                            agent.insertCodelet(trafficDrive);

                            lstOfDrives.add(trafficDrive);


                            /**
                             * Starting Lane Goals.
                             */
                            List<Drive> lstOfDriveLaneGoal = Collections.synchronizedList(new ArrayList<Drive>());
                            lstOfDriveLaneGoal.add(trafficDrive);

                            /**
                             * Finding lane that can be opened.
                             */
                            List<Integer> lstOfIndexGreenWave = findLaneToGreenWave(links, incomingLane);

                            Goal goalLane = new LaneHelpGoal(incomingLane.getID(), getiTimeStep(), 80, 30, 0.75d, 0.0d, 0.7d, lstOfIndexGreenWave);
                            goalLane.addOutput(agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString(), "0"));

                            MemoryObject trafficLinkPhaseMO = agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString(), lstOfLinkPhase);
                            goalLane.addBroadcast(trafficLinkPhaseMO);


                            /**
                             * !Important!
                             * Here is fundamental code because each goal needs a drive list to do the vote calculate.
                             * Therefore given the drive list, we need to create an instance of MemoryObject class with memory name "DRIVES_VOTE_MEMORY"
                             * and set its value with drive list as showed below.
                             */
                            MemoryObject drivesLaneGoal = agent.createMemoryObject(Goal.DRIVES_VOTE_MEMORY, 0d);
                            drivesLaneGoal.setI(lstOfDriveLaneGoal);

                            goalLane.addInput(drivesLaneGoal);


                            /**
                             * !Important!
                             * To act in environment, every goal must have an action. If it win the goals competition in GoalArchitecture class then
                             * it will act in environment. To do it, we need to make an instance of SubsumptionAction class and after add it in
                             * goal through of method "addSubsumptionAction". Consequently, we need to add the behaviour layer in agent with addLayer method.
                             */
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

                /**
                 * !Important!
                 * Starting GoalArchitecture class. It is responsible for manage the goals competition and urgent intervention.
                 * A goal can acts in two different moments. If it won the goal competition or if it was triggered by urgent intervention.
                 * Goal competition is simple choice that class does that is driven by goal votes. The goal that has the best vote score won the competition.
                 * Urgent Intervention is when a goal gets critical vote threshold and it must be performed.
                 * You can see with details in CST doc.
                 */
                GoalArchitecture goalArchitecture = new GoalArchitecture();

                /**
                 * For GoalArchitecture class to make a goals competition, we need to create two MemoryObject instance with name "DRIVE_MEMORY"
                 * and "GOALS_MEMORY". In drives memory we set with all drives list and goals memory we set with all goals list as showed below.
                 */
                MemoryObject drives = agent.createMemoryObject(GoalArchitecture.DRIVES_MEMORY, 0d);
                drives.setI(lstOfDrives);

                MemoryObject goals = agent.createMemoryObject(GoalArchitecture.GOALS_MEMORY, 0d);
                goals.setI(lstOfGoals);

                goalArchitecture.addInput(drives);
                goalArchitecture.addInput(goals);

                /**
                 * Setting GoalArchitecture in agent, and add it in agent coderack.
                 */
                agent.setGoalArchitecture(goalArchitecture);
                agent.insertCodelet(agent.getGoalArchitecture());


                /**
                 * Adding agent in agent list.
                 */
                getAgentMinds().add(agent);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }


    }

    /**
     * The findLaneToGreenWave is responsible for find out what are lanes that can be opened from a chosen lane.
     * @param controlledLinks
     * @param incomingLane
     * @return
     * @throws IOException
     */
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

    /**
     * Gets SUMO Server Port.
     * @return
     */
    public static int getiSumoPort() {
        return iSumoPort;
    }

    /**
     * Gets Map File Path.
     * @return
     */
    public static String getsPath() {
        return sPath;
    }

    /**
     * Sets Map File Path.
     * @param sPath
     */
    public static void setsPath(String sPath) {
        Main.sPath = sPath;
    }

    /**
     * Gets Total of Steps.
     * @return
     */
    public static int getiTotalStep() {
        return iTotalStep;
    }

    /**
     * Sets Total of Steps.
     * @param iTotalStep
     */
    public static void setiTotalStep(int iTotalStep) {
        Main.iTotalStep = iTotalStep;
    }

    /**
     * Gets Step Time.
     * @return
     */
    public static int getiTimeStep() {
        return iTimeStep;
    }

    /**
     * Sets Step Time.
     * @param iTimeStep
     */
    public static void setiTimeStep(int iTimeStep) {
        Main.iTimeStep = iTimeStep;
    }

    /**
     * Gets Agent List.
     * @return
     */
    public static List<AgentMind> getAgentMinds() {
        return agentMinds;
    }

    /**
     * Sets Agent List.
     * @param agentMinds
     */
    public static void setAgentMinds(List<AgentMind> agentMinds) {
        Main.agentMinds = agentMinds;
    }


    /**
     * Gets SUMO Process.
     * @return
     */
    public static Process getSumoProcess() {
        return sumoProcess;
    }

    /**
     * Sets SUMO Process.
     * @param sumoProcess
     */
    public static void setSumoProcess(Process sumoProcess) {
        Main.sumoProcess = sumoProcess;
    }

}
