package br.unicamp.tutorialsumo.agent;

import br.unicamp.cst.behavior.subsumption.SubsumptionArchitecture;
import br.unicamp.cst.core.entities.Mind;
import br.unicamp.cst.motivational.GoalArchitecture;

/**
 * Created by Du on 16/08/16.
 */
public class AgentMind extends Mind {

    private String name;
    private SubsumptionArchitecture subsumptionArchitecture;
    private GoalArchitecture goalArchitecture;

    public AgentMind(String name) {
        this.setName(name);

        setSubsumptionArchitecture(new SubsumptionArchitecture(this));

    }

    public SubsumptionArchitecture getSubsumptionArchitecture() {
        return subsumptionArchitecture;
    }

    public void setSubsumptionArchitecture(SubsumptionArchitecture subsumptionArchitecture) {
        this.subsumptionArchitecture = subsumptionArchitecture;
    }

    public GoalArchitecture getGoalArchitecture() {
        return goalArchitecture;
    }

    public void setGoalArchitecture(GoalArchitecture goalArchitecture) {
        this.goalArchitecture = goalArchitecture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
