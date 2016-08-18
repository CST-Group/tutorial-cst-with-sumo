package br.unicamp.tutorialsumo.codelets.motivational.drives;

import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.motivational.Drive;
import br.unicamp.cst.motivational.DriveLevel;
import br.unicamp.tutorialsumo.constants.MemoryObjectName;

/**
 * Created by Du on 11/03/16.
 */
public class TrafficDrive extends Drive {

    private MemoryObject vehiclesMO;
    private MemoryObject occupancyMO;
    private MemoryObject meanVelocityMO;
    private MemoryObject maxVelocityMO;
    
    public TrafficDrive(String name, DriveLevel level, double priority, double relevance) throws CodeletActivationBoundsException {
        super(name, level, priority, relevance);
    }

    @Override
    public synchronized double calculateSimpleActivation() {

        return Math.max(Double.parseDouble(getOccupancyMO().getI().toString()),
                                                (0.55 * Double.parseDouble(getOccupancyMO().getI().toString()) +
                                                 0.45 * calcMeanVelocityActivation(Double.parseDouble(getMeanVelocityMO().getI().toString()),
                                                                                  Double.parseDouble(getMaxVelocityMO().getI().toString()))));
    }




    public synchronized double calcMeanVelocityActivation(double dMeanVelocity, double dMaxVelocity)
    {
        return (1 - dMeanVelocity/dMaxVelocity);
    }

    @Override
    public synchronized double calculateSecundaryDriveActivation() {
        return 0;
    }

    @Override
    public void accessMemoryObjects() {

        if(getVehiclesMO() ==null)
            setVehiclesMO(this.getInput(MemoryObjectName.LANE_VEHICLES_ID_LIST.toString(), 0));

        if(getOccupancyMO() == null)
            setOccupancyMO(this.getInput(MemoryObjectName.LANE_OCCUPANCY.toString(), 0));

        if(getMeanVelocityMO() == null)
            setMeanVelocityMO(this.getInput(MemoryObjectName.LANE_MEAN_VELOCITY.toString(), 0));

        if(getMaxVelocityMO() == null)
            setMaxVelocityMO(this.getInput(MemoryObjectName.LANE_MAX_VELOCITY.toString(), 0));

    }

    @Override
    public void proc() {

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
}
