package br.unicamp.tutorialsumo.codelets.sensors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.tutorialsumo.comunication.SingleAccessQuery;
import br.unicamp.tutorialsumo.constants.MemoryObjectName;
import it.polito.appeal.traci.Lane;
import it.polito.appeal.traci.ReadObjectVarQuery;

import java.util.*;

/**
 * Created by Du on 06/01/16.
 */
public class LaneSensor extends Codelet {

    private Lane laneObject;

    private MemoryObject vehiclesMO;
    private MemoryObject occupancyMO;
    private MemoryObject meanVelocityMO;
    private MemoryObject maxVelocityMO;

    private int timestamp = 100;


    public LaneSensor(Lane laneObject, int timestamp){

        this.setLaneObject(laneObject);

        this.setTimestamp(timestamp);
    }

    public synchronized void setLaneObject(Lane currentLane){
        this.laneObject = currentLane;
    }


    public synchronized Lane getLaneObject(){
        return this.laneObject;
    }

    @Override
    public void accessMemoryObjects() {

        if(getVehiclesMO() ==null)
            setVehiclesMO(this.getOutput(MemoryObjectName.LANE_VEHICLES_ID_LIST.toString(), 0));

        if(getOccupancyMO() == null)
            setOccupancyMO(this.getOutput(MemoryObjectName.LANE_OCCUPANCY.toString(), 0));

        if(getMeanVelocityMO() == null)
            setMeanVelocityMO(this.getOutput(MemoryObjectName.LANE_MEAN_VELOCITY.toString(), 0));

        if(getMaxVelocityMO() == null)
            setMaxVelocityMO(this.getOutput(MemoryObjectName.LANE_MAX_VELOCITY.toString(), 0));
    }

    @Override
    public void calculateActivation() {
        try {
            this.setActivation(0.0d);
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void proc() {

        ReadObjectVarQuery<List<String>> qrVehicle = getLaneObject().queryReadLastStepVehicleIDList();
        ReadObjectVarQuery<Double> qrOccupancy = getLaneObject().queryReadLastStepOccupancy();
        ReadObjectVarQuery<Double> qrMeanVelocity = getLaneObject().queryReadMeanSpeed();
        ReadObjectVarQuery<Double> qrMaxVelocity = getLaneObject().queryReadMaxSpeed();

        List<ReadObjectVarQuery<?>> readObjectVarQueries = Collections.synchronizedList(new ArrayList<ReadObjectVarQuery<?>>());

        readObjectVarQueries.add(qrVehicle);
        readObjectVarQueries.add(qrOccupancy);
        readObjectVarQueries.add(qrMeanVelocity);
        readObjectVarQueries.add(qrMaxVelocity);

        Map<Integer, Object> objectMap = new HashMap<Integer, Object>();
        if(readObjectVarQueries.size() > 0)
          objectMap = SingleAccessQuery.executeReadQueries(readObjectVarQueries);

        Object vehicles = objectMap.containsKey(qrVehicle.hashCode())? objectMap.get(qrVehicle.hashCode()) : null;

        if(vehicles != null) {
            getVehiclesMO().setI(vehicles);
        }

        Object occupancy = objectMap.containsKey(qrOccupancy.hashCode())? objectMap.get(qrOccupancy.hashCode()) : null;

        if(occupancy != null)
            getOccupancyMO().setI(Double.parseDouble(occupancy.toString()));


        Object meanVelocity = objectMap.containsKey(qrMeanVelocity.hashCode())? objectMap.get(qrMeanVelocity.hashCode()) : null;

        if(meanVelocity != null)
            getMeanVelocityMO().setI(Double.parseDouble(meanVelocity.toString()));


        Object maxVelocity = objectMap.containsKey(qrMaxVelocity.hashCode())? objectMap.get(qrMaxVelocity.hashCode()) : null;

        if(maxVelocity != null)
            getMaxVelocityMO().setI(Double.parseDouble(maxVelocity.toString()));


        try {
            Thread.sleep(getTimestamp()*5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


    public MemoryObject getVehiclesMO() {
        return vehiclesMO;
    }

    public void setVehiclesMO(MemoryObject vehiclesMO) {
        this.vehiclesMO = vehiclesMO;
    }

    public MemoryObject getOccupancyMO() {
        return occupancyMO;
    }

    public void setOccupancyMO(MemoryObject occupancyMO) {
        this.occupancyMO = occupancyMO;
    }

    public MemoryObject getMeanVelocityMO() {
        return meanVelocityMO;
    }

    public void setMeanVelocityMO(MemoryObject meanVelocityMO) {
        this.meanVelocityMO = meanVelocityMO;
    }

    public MemoryObject getMaxVelocityMO() {
        return maxVelocityMO;
    }

    public void setMaxVelocityMO(MemoryObject maxVelocityMO) {
        this.maxVelocityMO = maxVelocityMO;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
