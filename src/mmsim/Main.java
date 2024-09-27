package mmsim;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;

import picocli.CommandLine;

enum RatingSystem {
    Elo,
    PersonalSkill,
}

@CommandLine.Command(name = "mmsim", mixinStandardHelpOptions = true, version = "0.1.7", description = "Matchmaking Simulator")
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-r"}, description = "Which rating system to use. Valid values: ${COMPLETION-CANDIDATES}.")
    private RatingSystem ratingSystem = RatingSystem.PersonalSkill;

    @CommandLine.Option(names = {"-p", "--players"}, description = "How many players to generate.")
    private int playerCount = 15_000;

    @CommandLine.Option(names = {"-g", "--games"}, description = "How many games to generate.")
    private int gameCount = 150_000;

    @Override
    public Integer call() throws Exception {
        if (ratingSystem == null) {
            System.out.println("Error: you need to specify which rating system (-r) to calculate games with.");
            return 1;
        }

        PlayerGenerator playerGenerator = new PlayerGenerator();
        Set<Player> players = playerGenerator.GeneratePlayers(playerCount);

        ArrayList<Player> playerList = new ArrayList<>(players);

        int ratingDiff = 100;
        GameCalculator gc = new GameCalculator(ratingDiff, playerList, gameCount);

        switch (ratingSystem) {
            case Elo -> gc.calculateGamesElo();
            case PersonalSkill -> gc.calculateGamesPersonalSkill();
        }

        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}
