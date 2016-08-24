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

package br.unicamp.tutorialsumo.codelets.actuators;

import br.unicamp.cst.behavior.subsumption.SubsumptionAction;
import br.unicamp.cst.behavior.subsumption.SubsumptionArchitecture;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.tutorialsumo.comunication.SingleAccessQuery;
import br.unicamp.tutorialsumo.constants.MemoryObjectName;
import it.polito.appeal.traci.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * TrafficLightActuator class is actuator that sends command to traffic light. It is driven by goal that won the goal competition
 * or is invoked by urgent intervention in GoalArchitecture.
 */
public class TrafficLightActuator extends SubsumptionAction {

    /**
     * Attributes:
     * TrafficLight Object,
     * Changing Phase Memory Object (Phases that will change),
     * Changed Phase Memory Object (Phases that were change),
     * Previous TLState Object,
     * Timestamp.
     */
    private TrafficLight trafficLight;
    private MemoryObject changingPhaseMO;
    private MemoryObject changedPhaseMO;
    private TLState previousTLState;
    private int timeStamp = 100;

    /**
     * TrafficLightActuator Constructor.
     *
     * @param trafficLight
     * @param timestamp
     * @param subsumptionArchitecture
     */
    public TrafficLightActuator(TrafficLight trafficLight, int timestamp, SubsumptionArchitecture subsumptionArchitecture) {
        super(subsumptionArchitecture);
        setName(trafficLight.getID());
        setTimestamp(timestamp);

        this.setTrafficLight(trafficLight);


        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    }

    /**
     * This method is responsible for access the input and output memories of traffic light actuator.
     */
    @Override
    public void accessMemoryObjects() {

        if (getChangingPhaseMO() == null)
            setChangingPhaseMO(this.getInput(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString(), 0));

        if (getChangedPhaseMO() == null)
            setChangedPhaseMO(this.getOutput(MemoryObjectName.TRAFFICLIGHT_MEMORY_CHANGED.toString(), 0));

    }

    /**
     * Calculates activation value of traffic light actuator
     */
    @Override
    public void calculateActivation() {
        try {
            this.setActivation(1d);
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is responsible for verify if the phases that will change traffic light state is different than the previous.
     *
     * @param state
     * @return
     */
    public boolean isDifferentThanPrevious(TLState state) {

        if (getPreviousTLState() != null) {
            for (int i = 0; i < getPreviousTLState().lightStates.length; i++) {
                if (state.lightStates[i] != getPreviousTLState().lightStates[i]) {
                    return true;
                }
            }
        } else {
            return true;
        }

        return false;

    }

    /**
     * This method is used to suppress another actuator in SubsumptionArchitecture, but here we don't use.
     * @return
     */
    @Override
    public boolean suppressCondition() {
        return false;
    }

    /**
     * This method is used to inhibit another actuator in SubsumptionArchitecture, but here we don't use.
     * @return
     */
    @Override
    public boolean inhibitCondition() {
        return false;
    }

    /**
     * Action that is performed by traffic light actuator.
     */
    @Override
    public synchronized void act() {

        if (getChangingPhaseMO() != null) {
            if (getChangingPhaseMO().getI() != "0") {
                TLState tlState = ((TLState) getChangingPhaseMO().getI());
                if (tlState.lightStates.length != 0) {
                    if (isDifferentThanPrevious(tlState)) {
                        ChangeLightsStateQuery changeLightsStateQuery = getTrafficLight().queryChangeLightsState();

                        changeLightsStateQuery.setValue(tlState);
                        List<ChangeObjectVarQuery<?>> changeObjectVarQueryList = Collections.synchronizedList(new ArrayList<ChangeObjectVarQuery<?>>());
                        changeObjectVarQueryList.add(changeLightsStateQuery);

                        SingleAccessQuery.executeChangeQueries(changeObjectVarQueryList);

                        getChangedPhaseMO().setI(getChangingPhaseMO().getI());

                        setPreviousTLState((TLState) getChangedPhaseMO().getI());

                        getChangingPhaseMO().setI("0");
                    }

                }
            }
        }

        try {
            Thread.sleep(getTimestamp());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the Traffic Light Object.
     * @return
     */
    public synchronized TrafficLight getTrafficLight() {
        return trafficLight;
    }

    /**
     * Sets the Traffic Light Object.
     * @param trafficLight
     */
    public synchronized void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
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
     * Gets the Changed Phase Memory Object.
     * @return
     */
    public synchronized MemoryObject getChangedPhaseMO() {
        return changedPhaseMO;
    }

    /**
     * Sets the Changed Phase Memory Object.
     * @param changedPhaseMO
     */
    public synchronized void setChangedPhaseMO(MemoryObject changedPhaseMO) {
        this.changedPhaseMO = changedPhaseMO;
    }

    /**
     * Gets Timestamp.
     *
     * @return
     */
    public int getTimestamp() {
        return timeStamp;
    }


    /**
     * Sets Timestep.
     * @param timestamp
     */
    public void setTimestamp(int timestamp) {
        this.timeStamp = timestamp;
    }


    /**
     * Gets the previous TLState.
     * @return
     */
    public synchronized TLState getPreviousTLState() {
        return previousTLState;
    }

    /***
     * Sets the previous TLState.
     * @param previousTLState
     */
    public synchronized void setPreviousTLState(TLState previousTLState) {

        LightState[] lightStates = new LightState[previousTLState.lightStates.length];

        for (int i = 0; i < previousTLState.lightStates.length; i++) {
            if (previousTLState.lightStates[i].isGreen()) {
                lightStates[i] = LightState.GREEN;
            }
            if (previousTLState.lightStates[i].isRed()) {
                lightStates[i] = LightState.RED;
            }
            if (previousTLState.lightStates[i].isYellow()) {
                lightStates[i] = LightState.YELLOW;
            }
        }

        this.previousTLState = new TLState(lightStates);
    }
}
