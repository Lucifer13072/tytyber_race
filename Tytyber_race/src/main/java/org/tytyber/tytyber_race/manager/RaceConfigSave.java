package org.tytyber.tytyber_race.manager;

import java.util.List;

public class RaceConfigSave {
    private final RaceDataStore dataStore;

    public RaceConfigSave(RaceDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public boolean setPlayerRace(String uuid, String race) {
        return dataStore.setPlayerRace(uuid, race);
    }

    public String getPlayerRace(String uuid) {
        return dataStore.getPlayerRace(uuid);
    }

    public List<String> getPlayersByRace(String race) {
        return dataStore.getPlayersByRace(race);
    }

    public void save() {
        dataStore.saveToFile();
    }
}
