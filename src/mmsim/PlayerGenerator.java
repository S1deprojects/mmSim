package mmsim;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.HashSet;

public class PlayerGenerator {
    private final Set<Integer> names = new HashSet<>();

    public void rngNameGen(int numNames) {
        names.clear();
        for (int i = 0; i < numNames; i++) {
            names.add(i);
        }
    }

    // generates a base list of players up to a set amount, with 'skill' increasing by 0.01
    public Set<Player> GeneratePlayers(int count) {
        Set<Player> playerList = new HashSet<>();

        double startingSkill = 0.01;

        int defaultELO = 1000;
        double defaultVolatility = 0.05;
        double defaultConfidence = 35.0;

        rngNameGen(count);
        for (Integer name : names) {

            Player p = new Player(name, startingSkill, defaultELO, defaultVolatility, defaultConfidence);
            playerList.add(p);
            startingSkill += 0.01;
        }

        return playerList;
    }
}
