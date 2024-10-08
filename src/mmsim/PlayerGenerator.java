package mmsim;

import java.util.Set;
import java.util.HashSet;

public class PlayerGenerator {
    private final Set<Integer> names = new HashSet<>();

    // generates a base list of players up to a set amount, with 'skill' increasing by 0.01
    public Set<Player> GeneratePlayers(int count) {
        Set<Player> playerList = new HashSet<>();

        double startingSkill = 1;

        int defaultELO = 1000;
        double defaultVolatility = 0.05;
        double defaultConfidence = 35.0;

        for (int i = 1; i <= count; i++) {
            playerList.add(new Player(i, startingSkill/100, defaultELO, defaultVolatility, defaultConfidence));
            startingSkill += 1;
        }

        return playerList;
    }
}
