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
 * Created by Du on 11/01/16.
 */
public class TrafficLightActuator extends SubsumptionAction {

    private TrafficLight trafficLight;

    private MemoryObject changingPhaseMO;
    private MemoryObject changedPhaseMO;

    private TLState previousTLState;

    private int timeStamp = 100;


    public TrafficLightActuator(TrafficLight trafficLight, int timestamp, SubsumptionArchitecture subsumptionArchitecture) {
        super(subsumptionArchitecture);
        setName(trafficLight.getID());
        setTimestamp(timestamp);

        this.setTrafficLight(trafficLight);


        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    }

    @Override
    public void accessMemoryObjects() {

        if(getChangingPhaseMO() ==null)
            setChangingPhaseMO(this.getInput(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString(), 0));

        if(getChangedPhaseMO() ==null)
            setChangedPhaseMO(this.getOutput(MemoryObjectName.TRAFFICLIGHT_MEMORY_CHANGED.toString(), 0));

    }

    @Override
    public void calculateActivation() {
        try {
            this.setActivation(1d);
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }
    }


    public boolean isDifferentThanPrevious(TLState state){

        if(getPreviousTLState() != null)
        {
            for(int i=0; i < getPreviousTLState().lightStates.length; i++){
                if(state.lightStates[i] != getPreviousTLState().lightStates[i]) {
                    return true;
                }
            }
        }
        else{
            return true;
        }

        return false;

    }

    @Override
    public boolean suppressCondition() {
        return false;
    }

    @Override
    public boolean inhibitCondition() {
        return false;
    }

    @Override
    public synchronized void act() {

        if (getChangingPhaseMO() != null) {
            if (getChangingPhaseMO().getI() != "0") {
                TLState tlState = ((TLState) getChangingPhaseMO().getI());
                if (tlState.lightStates.length != 0) {
                    if(isDifferentThanPrevious(tlState)) {
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

    public synchronized TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public synchronized void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }

    public synchronized MemoryObject getChangingPhaseMO() {
        return changingPhaseMO;
    }

    public synchronized void setChangingPhaseMO(MemoryObject changingPhaseMO) {
        this.changingPhaseMO = changingPhaseMO;
    }

    public synchronized MemoryObject getChangedPhaseMO() {
        return changedPhaseMO;
    }

    public synchronized void setChangedPhaseMO(MemoryObject changedPhaseMO) {
        this.changedPhaseMO = changedPhaseMO;
    }

    public int getTimestamp() {
        return timeStamp;
    }

    public void setTimestamp(int timestamp) {
        this.timeStamp = timestamp;
    }

    public synchronized TLState getPreviousTLState() {
        return previousTLState;
    }

    public synchronized void setPreviousTLState(TLState previousTLState) {

        LightState[] lightStates = new LightState[previousTLState.lightStates.length];

        for(int i=0; i<previousTLState.lightStates.length; i++){
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
