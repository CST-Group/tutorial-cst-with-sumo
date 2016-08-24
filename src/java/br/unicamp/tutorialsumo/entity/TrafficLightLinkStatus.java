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

package br.unicamp.tutorialsumo.entity;

import it.polito.appeal.traci.ControlledLink;
import it.polito.appeal.traci.LightState;

/**
 * This class is responsible for relationship between traffic light phases and controlled links.
 * The index attribute holds what is respective phase position of controlled link in trafficlight phase.
 * The phase attribute holds what is respective phase(red, yellow, green) about a controlled link.
 * The controlledLink attribute holds the controlled link.
 */
public class TrafficLightLinkStatus {

    private int index;
    private LightState phase;
    private ControlledLink controlledLink;

    /**
     * TrafficLightLinkStatus Constructor.
     * @param index
     * @param phase
     * @param controlledLink
     */
    public TrafficLightLinkStatus(int index, LightState phase, ControlledLink controlledLink){
        setIndex(index);
        setPhase(phase);
        setControlledLink(controlledLink);
    }


    /**
     * Gets Traffic Light State for respective controlled link.
     * @return
     */
    public synchronized LightState getPhase() {
        return phase;
    }

    /**
     * Sets Traffic Light State for respective controlled link.
     * @param phase
     */
    public synchronized void setPhase(LightState phase) {
        this.phase = phase;
    }


    /**
     * Gets current controlled link.
     * @return
     */
    public synchronized ControlledLink getControlledLink() {
        return controlledLink;
    }


    /**
     * Sets controlled link.
     * @param controlledLink
     */
    public synchronized void setControlledLink(ControlledLink controlledLink) {
        this.controlledLink = controlledLink;
    }


    /**
     * Gets index of traffic light phase.
     * @return
     */
    public int getIndex() {
        return index;
    }


    /**
     * Sets index of traffic light phase.
     * @param index
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
