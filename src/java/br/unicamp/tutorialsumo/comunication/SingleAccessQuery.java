package br.unicamp.tutorialsumo.comunication;

import it.polito.appeal.traci.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Du on 19/01/16.
 */
public class SingleAccessQuery {

    private static SumoTraciConnection sumoTraciConnection;
    private static MultiQuery multiQuery;

    public static synchronized void setConnection(SumoTraciConnection connection) {
        setSumoTraciConnection(connection);
        getSumoTraciConnection().enableTcpNoDelay();

    }


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

    public static synchronized void nextSimStep() {
        synchronized (getSumoTraciConnection()) {
            try {
                getSumoTraciConnection().nextSimStep();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized Collection<Vehicle> getNumOfVehicles() {

        try {
            return getSumoTraciConnection().getVehicleRepository().getAll().values();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;


    }

    public static synchronized SumoTraciConnection getSumoTraciConnection() {
        return sumoTraciConnection;
    }

    public static synchronized void setSumoTraciConnection(SumoTraciConnection sumoTraciConnection) {
        SingleAccessQuery.sumoTraciConnection = sumoTraciConnection;
    }

    public static synchronized void closeConnection() {
        try {
            getSumoTraciConnection().close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static synchronized MultiQuery getMultiQuery() {
        return multiQuery;
    }

    public static synchronized void setMultiQuery(MultiQuery multiQuery) {
        SingleAccessQuery.multiQuery = multiQuery;
    }
}
