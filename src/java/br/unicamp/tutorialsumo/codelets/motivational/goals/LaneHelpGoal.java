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

package br.unicamp.tutorialsumo.codelets.motivational.goals;

import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.motivational.Drive;
import br.unicamp.cst.motivational.Goal;
import br.unicamp.tutorialsumo.constants.MemoryObjectName;
import br.unicamp.tutorialsumo.entity.TrafficLightLinkStatus;
import it.polito.appeal.traci.LightState;
import it.polito.appeal.traci.TLState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The LaneHelpGoal class is a goal that helps determined lane.
 */

public class LaneHelpGoal extends Goal {

    /**
     * Attributes:
     * Changing Phase Memory Object,
     * Traffic Light Phase Memory Object,
     * List of Index Lane that can be opened in same time,
     * Timestamp.
     */
    private MemoryObject changingPhaseMO;
    private MemoryObject trafficLinkPhaseMO;
    private List<Integer> lstOfIndexToGreenWave;
    private List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus;
    private int timestamp = 0;

    /**
     * LaneHelpGoal Constructor.
     * @param name
     * @param timestamp
     * @param steps
     * @param minSteps
     * @param interventionThreshold
     * @param belowUrgentInterventionThreshold
     * @param priorityHighLevel
     */
    public LaneHelpGoal(String name, int timestamp, int steps, int minSteps, double interventionThreshold, double belowUrgentInterventionThreshold, double priorityHighLevel) {
        super(name, steps, minSteps, interventionThreshold, belowUrgentInterventionThreshold, priorityHighLevel);

        this.setTimestamp(timestamp);

        this.setLstOfIndexToGreenWave(Collections.synchronizedList(new ArrayList<Integer>()));
    }


    /**
     * LaneHelpGoal Constructor.
     * @param name
     * @param timestamp
     * @param steps
     * @param minSteps
     * @param interventionThreshold
     * @param belowUrgentInterventionThreshold
     * @param priorityHighLevel
     * @param lstOfIndexToGreenWave
     */
    public LaneHelpGoal(String name, int timestamp, int steps, int minSteps, double interventionThreshold, double belowUrgentInterventionThreshold, double priorityHighLevel, List<Integer> lstOfIndexToGreenWave) {
        super(name, steps, minSteps, interventionThreshold, belowUrgentInterventionThreshold, priorityHighLevel);

        this.setTimestamp(timestamp);

        this.setLstOfIndexToGreenWave(lstOfIndexToGreenWave);
    }

    /**
     * !Important!
     * This method is responsible for urgent vote calculation. It gets just high level drive to does the vote. If the vote reaches the threshold then
     * the GoalArchitecture execute the urgent intervention.
     * @param lstOfHighPriorityDrive
     * @return
     */
    @Override
    public synchronized double calculateUrgentVote(List<Drive> lstOfHighPriorityDrive) {
        return lstOfHighPriorityDrive.size() == 0 ? 0 : (lstOfHighPriorityDrive.stream().mapToDouble(drive -> drive.getActivation()).sum() / lstOfHighPriorityDrive.size());
    }


    /**
     * !Important!
     * This method is responsible for vote calculation. It gets high and low level drive to compose the vote.
     * @param listOfDrivesVote
     * @return
     */
    @Override
    public synchronized double calculateVote(List<Drive> listOfDrivesVote) {
        List<Drive> lstOfHighLevelDrive = listOfDrivesVote.stream().filter(drive -> drive.getPriority() >= getPriorityHighLevel()).collect(Collectors.toList());
        List<Drive> lstOfLowLevelDrive = listOfDrivesVote.stream().filter(drive -> drive.getPriority() < getPriorityHighLevel()).collect(Collectors.toList());


        double dHighVote = lstOfHighLevelDrive.size() == 0 ? 0 : lstOfHighLevelDrive.stream().mapToDouble(drive -> drive.getActivation()).sum() /
                lstOfHighLevelDrive.size();

        double dLowVote = lstOfLowLevelDrive.size() == 0 ? 0 : lstOfLowLevelDrive.stream().mapToDouble(drive -> drive.getActivation()).sum() /
                lstOfLowLevelDrive.size();

        return (dHighVote * 1 + dLowVote * 0);

    }


    /**
     * Action that is performed by Lane Help Goal.
     */
    @Override
    public synchronized void executeActions() {

        LightState[] lightStates = new LightState[getLstOfTrafficLightLinkStatus().size()];

        try {
            if (getExecutedSteps() == 0 || isbPause()) {

                getChangingPhaseMO().setI(showYellowWave(getLstOfTrafficLightLinkStatus(), lightStates));
                Thread.sleep(getTimestamp() * 2);

                getChangingPhaseMO().setI(showRedWave(getLstOfTrafficLightLinkStatus(), lightStates));
                Thread.sleep(getTimestamp() * 4);

            } else
                getChangingPhaseMO().setI(showGreenWave(getLstOfTrafficLightLinkStatus(), lightStates));


            Thread.sleep(getTimestamp());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is responsible for shows the red wave for respective lane.
     * @param lstOfTrafficLightLinkStatus
     * @param lightStates
     * @return
     */
    public TLState showRedWave(List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus, LightState[] lightStates) {
        for (int i = 0; i < lstOfTrafficLightLinkStatus.size(); i++) {
            lightStates[lstOfTrafficLightLinkStatus.get(i).getIndex()] = LightState.RED;
            lstOfTrafficLightLinkStatus.get(i).setPhase(LightState.RED);
        }

        TLState tlStateRed = new TLState(lightStates);

        return tlStateRed;
    }

    /**
     * This method is responsible for shows the yellow wave for respective lane.
     * @param lstOfTrafficLightLinkStatus
     * @param lightStates
     * @return
     */
    public TLState showYellowWave(List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus, LightState[] lightStates) {
        for (int i = 0; i < lstOfTrafficLightLinkStatus.size(); i++) {

            if (lstOfTrafficLightLinkStatus.get(i).getPhase() == LightState.GREEN || lstOfTrafficLightLinkStatus.get(i).getPhase() == LightState.GREEN_NODECEL) {
                lightStates[lstOfTrafficLightLinkStatus.get(i).getIndex()] = LightState.YELLOW;
                lstOfTrafficLightLinkStatus.get(i).setPhase(LightState.YELLOW);
            } else {
                lightStates[i] = LightState.RED;
                lstOfTrafficLightLinkStatus.get(i).setPhase(LightState.RED);
            }

        }

        TLState tlStateYellow = new TLState(lightStates);

        return tlStateYellow;
    }

    /**
     * This method is responsible for shows the green wave for respective lane.
     * @param lstOfTrafficLightLinkStatus
     * @param lightStates
     * @return
     */
    public TLState showGreenWave(List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus, LightState[] lightStates) {
        for (int i = 0; i < lstOfTrafficLightLinkStatus.size(); i++) {
            lightStates[lstOfTrafficLightLinkStatus.get(i).getIndex()] = LightState.RED;
            lstOfTrafficLightLinkStatus.get(i).setPhase(LightState.RED);
        }

        for (int j = 0; j < getLstOfIndexToGreenWave().size(); j++) {
            lightStates[getLstOfIndexToGreenWave().get(j)] = LightState.GREEN;
            lstOfTrafficLightLinkStatus.get(getLstOfIndexToGreenWave().get(j)).setPhase(LightState.GREEN);
        }

        TLState tlState = new TLState(lightStates);

        return tlState;
    }

    /**
     * This method is responsible for access the input and output memories of traffic light actuator.
     */
    @Override
    public void accessMemoryObjects() {

        if (getTrafficLinkPhaseMO() == null) {
            setTrafficLinkPhaseMO(this.getBroadcast(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString()));
            setLstOfTrafficLightLinkStatus((List<TrafficLightLinkStatus>)getTrafficLinkPhaseMO().getI());
        }

        if (getChangingPhaseMO() == null)
            setChangingPhaseMO(this.getOutput(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString()));


    }

    /**
     * Gets the Changing Phase Memory Object.
     * @return
     */
    public synchronized MemoryObject getChangingPhaseMO() {
        return changingPhaseMO;
    }

    /**
     * Sets the Changing Phase Memory Object.
     * @param changingPhaseMO
     */
    public synchronized void setChangingPhaseMO(MemoryObject changingPhaseMO) {
        this.changingPhaseMO = changingPhaseMO;
    }

    /**
     * Gets the Traffic Light Phase Memory Object.
     * @return
     */
    public synchronized MemoryObject getTrafficLinkPhaseMO() {
        return trafficLinkPhaseMO;
    }

    /**
     * Sets the Traffic Light Phase Memory Object.
     * @param trafficLinkPhaseMO
     */
    public synchronized void setTrafficLinkPhaseMO(MemoryObject trafficLinkPhaseMO) {
        this.trafficLinkPhaseMO = trafficLinkPhaseMO;
    }

    /**
     * Gets List of Index Lane that can be opened in same time.
     * @return
     */
    public List<Integer> getLstOfIndexToGreenWave() {
        return lstOfIndexToGreenWave;
    }

    /**
     * Sets List of Index Lane that can be opened in same time.
     * @param lstOfIndexToGreenWave
     */
    public void setLstOfIndexToGreenWave(List<Integer> lstOfIndexToGreenWave) {
        this.lstOfIndexToGreenWave = lstOfIndexToGreenWave;
    }

    /**
     * Gets Timestamp.
     *
     * @return
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Sets Timestep.
     * @param timestamp
     */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the list of Traffic Light Status.
     * @return
     */
    public List<TrafficLightLinkStatus> getLstOfTrafficLightLinkStatus() {
        return lstOfTrafficLightLinkStatus;
    }

    /**
     * Sets the list of Traffic Light Status.
     * @param lstOfTrafficLightLinkStatus
     */
    public void setLstOfTrafficLightLinkStatus(List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus) {
        this.lstOfTrafficLightLinkStatus = lstOfTrafficLightLinkStatus;
    }
}
