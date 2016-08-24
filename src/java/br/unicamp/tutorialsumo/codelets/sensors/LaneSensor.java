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
 * LaneSensor class is sensor that retrieves data and information from a lane. These information are such as occupancy, maximum and mean velocity,
 * and vehicles that there are on lane.
 */
public class LaneSensor extends Codelet {

    /**
     * Attributes:
     * Lane Object;
     * Different of Memory Objects sucb as Occupation, Amount of Vehicles, Mean and Max of Vehicles Speed.
     * Execution Timestamp.
     */
    private Lane laneObject;
    private MemoryObject vehiclesMO;
    private MemoryObject occupancyMO;
    private MemoryObject meanVelocityMO;
    private MemoryObject maxVelocityMO;
    private int timestamp = 100;

    /**
     * LaneSensor Constructor.
     * @param laneObject
     * @param timestamp
     */
    public LaneSensor(Lane laneObject, int timestamp){

        this.setLaneObject(laneObject);

        this.setTimestamp(timestamp);
    }


    /**
     * This method is responsible for access the input and output memories of lane sensor.
     */
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

    /**
     * Calculates activation value of lane sensor
     */
    @Override
    public void calculateActivation() {
        try {
            this.setActivation(0.0d);
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }

    }

    /**
     * Action that is performed by lane sensor.
     */
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

    /**
     * Sets Lane Object
     * @param currentLane
     */
    public synchronized void setLaneObject(Lane currentLane){
        this.laneObject = currentLane;
    }

    /**
     * Gets Lane Object.
     * @return
     */
    public synchronized Lane getLaneObject(){
        return this.laneObject;
    }

    /**
     * Gets Vehicles Memory Object.
     * @return
     */
    public MemoryObject getVehiclesMO() {
        return vehiclesMO;
    }

    /**
     * Sets Vehicles Memory Object.
     * @param vehiclesMO
     */
    public void setVehiclesMO(MemoryObject vehiclesMO) {
        this.vehiclesMO = vehiclesMO;
    }


    /**
     * Gets Occupancy Memory Object.
     * @return
     */
    public MemoryObject getOccupancyMO() {
        return occupancyMO;
    }


    /**
     * Sets Occupancy Memory Object.
     * @param occupancyMO
     */
    public void setOccupancyMO(MemoryObject occupancyMO) {
        this.occupancyMO = occupancyMO;
    }

    /**
     * Gets Mean Velocity Memory Object.
     * @return
     */
    public MemoryObject getMeanVelocityMO() {
        return meanVelocityMO;
    }

    /**
     * Sets Mean Velocity Memory Object.
     * @param meanVelocityMO
     */
    public void setMeanVelocityMO(MemoryObject meanVelocityMO) {
        this.meanVelocityMO = meanVelocityMO;
    }


    /**
     * Gets Maximum Velocity Memory Object.
     * @return
     */
    public MemoryObject getMaxVelocityMO() {
        return maxVelocityMO;
    }

    /**
     * Sets Maximum Velocity Memory Object.
     * @param maxVelocityMO
     */
    public void setMaxVelocityMO(MemoryObject maxVelocityMO) {
        this.maxVelocityMO = maxVelocityMO;
    }

    /**
     * Gets Timestamp.
     * @return
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Sets Timestamp.
     * @param timestamp
     */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
