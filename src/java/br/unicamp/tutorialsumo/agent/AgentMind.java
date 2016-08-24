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

package br.unicamp.tutorialsumo.agent;

import br.unicamp.cst.behavior.subsumption.SubsumptionArchitecture;
import br.unicamp.cst.core.entities.Mind;
import br.unicamp.cst.motivational.GoalArchitecture;

/**
 * This class is the agent mind. Here, we have some structures such as SubsumptionArchitecture and GoalArchitecture.
 * SubsumptionArchitecture is responsible for execute the agent behaviors.
 * GoalArchitecture is responsible for manage the goal competition and urgent intervention.
 * To understand these structures see the cst source code.
 */
public class AgentMind extends Mind {

    /**
     * Attributes:
     * Agent name, Subsumption Architecture and Goal Architecture.
     */
    private String name;
    private SubsumptionArchitecture subsumptionArchitecture;
    private GoalArchitecture goalArchitecture;

    /**
     * AgentMind Constructor.
     * @param name
     */
    public AgentMind(String name) {
        this.setName(name);

        setSubsumptionArchitecture(new SubsumptionArchitecture(this));

    }

    /**
     * Get Subsumption Architecture.
     * @return
     */
    public SubsumptionArchitecture getSubsumptionArchitecture() {
        return subsumptionArchitecture;
    }

    /**
     * Set Subsumption Architecture.
     * @param subsumptionArchitecture
     */
    public void setSubsumptionArchitecture(SubsumptionArchitecture subsumptionArchitecture) {
        this.subsumptionArchitecture = subsumptionArchitecture;
    }

    /**
     * Get Goal Architecture.
     * @return
     */
    public GoalArchitecture getGoalArchitecture() {
        return goalArchitecture;
    }


    /**
     * Set Goal Architecture.
     * @param goalArchitecture
     */
    public void setGoalArchitecture(GoalArchitecture goalArchitecture) {
        this.goalArchitecture = goalArchitecture;
    }

    /**
     * Get agent name.
     * @return
     */
    public String getName() {
        return name;
    }


    /**
     * Set agent name.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }
}
