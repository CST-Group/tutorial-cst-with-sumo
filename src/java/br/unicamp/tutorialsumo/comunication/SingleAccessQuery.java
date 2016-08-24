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

package br.unicamp.tutorialsumo.comunication;

import it.polito.appeal.traci.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is very important to do connection and communication with SUMO because at the moment the TraCI4J and SUMO developers don't implemented multi thread connection.
 * Therefore, we should use this class to share SUMO connection among the agents.
 * To do queries in SUMO we can use executeReadQueries method with list of ReadObjectVarQuery as parameter,
 * and to change states in traffic lights and lanes we use executeChangeQueries method.
 */

public class SingleAccessQuery {

    private static SumoTraciConnection sumoTraciConnection;
    private static MultiQuery multiQuery;

    /**
     * Sets SUMO connection.
     * @param connection
     */
    public static synchronized void setConnection(SumoTraciConnection connection) {
        setSumoTraciConnection(connection);
        getSumoTraciConnection().enableTcpNoDelay();

    }

    /**
     * Performing queries in SUMO server, and return Map of integer(id) and object(value).
     *
     * @param readObjectVarQueries
     * @return Map with id and value.
     */
    public static synchronized Map<Integer, Object> executeReadQueries(List<ReadObjectVarQuery<?>> readObjectVarQueries) {

        setMultiQuery(getSumoTraciConnection().makeMultiQuery());

        for (ReadObjectVarQuery<?> query : readObjectVarQueries) {
            getMultiQuery().add(query);
        }

        try {
            getMultiQuery().run();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<Integer, Object> objectMap = new HashMap<Integer, Object>();

        for (ReadObjectVarQuery<?> query : readObjectVarQueries) {
            try {
                objectMap.put(query.hashCode(), query.get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return objectMap;


    }

    /**
     * Performing change queries in SUMO map.
     *
     * @param changeObjectVarQueries
     */
    public static synchronized void executeChangeQueries(List<ChangeObjectVarQuery<?>> changeObjectVarQueries) {

        setMultiQuery(getSumoTraciConnection().makeMultiQuery());

        for (ChangeObjectVarQuery<?> query : changeObjectVarQueries) {
            getMultiQuery().add(query);
        }

        try {
            getMultiQuery().run();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Sends command to get next simulation step.
     */
    public static synchronized void nextSimStep() {
        synchronized (getSumoTraciConnection()) {
            try {
                getSumoTraciConnection().nextSimStep();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a collection of all vehicles in map.
     *
     * @return Collection<Vehicles>
     */
    public static synchronized Collection<Vehicle> getNumOfVehicles() {

        try {
            return getSumoTraciConnection().getVehicleRepository().getAll().values();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * Gets SUMO connection.
     *
     * @return SumoTraciConnection
     */
    public static synchronized SumoTraciConnection getSumoTraciConnection() {
        return sumoTraciConnection;
    }


    /**
     * Sets SUMO connection.
     *
     * @param sumoTraciConnection
     */
    public static synchronized void setSumoTraciConnection(SumoTraciConnection sumoTraciConnection) {
        SingleAccessQuery.sumoTraciConnection = sumoTraciConnection;
    }

    /**
     * Closes the SUMO connection.
     */
    public static synchronized void closeConnection() {
        try {
            getSumoTraciConnection().close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets MultiQuery.
     *
     * @return MultiQuery
     */
    public static synchronized MultiQuery getMultiQuery() {
        return multiQuery;
    }

    /**
     * Sets MultiQuery.
     *
     * @param multiQuery
     */
    public static synchronized void setMultiQuery(MultiQuery multiQuery) {
        SingleAccessQuery.multiQuery = multiQuery;
    }
}
