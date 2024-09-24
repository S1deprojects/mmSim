import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
//Created by S1ft 2024

public class GameCalculator {

    final ArrayList<Game> allGamesPlayed = new ArrayList<>();
    final Set<Player> playersUsed = new HashSet<>();
    boolean firstLoop = true;
    final double POINTSCAP = 1.75;
    final double POINTSFLOOR = 0.25;

    final int ratingDiff;
    final ArrayList<Player> players;
    TreeSet<Player> newPlayers = new TreeSet<>();
    final int gamesPlayed;

    //creates a hashset of players within an assigned rating difference, to be used for creating teams
    public HashSet<Player> getPlayersInRatingDiff(Player p1, ArrayList<Player> players, int ratingDiff, int p1Index) {
        HashSet<Player> ratingDiffArray = new HashSet<>();

        if (players.size() > ratingDiff) {
            if (p1Index - (ratingDiff / 2) > 0 && p1Index + (ratingDiff / 2) < players.size()) {
                for (int i = 1; i < (ratingDiff / 2) + 1; i++) {
                    ratingDiffArray.add(players.get(p1Index + Math.abs(i)));
                    ratingDiffArray.add(players.get(p1Index - Math.abs(i)));
                }
            }
            // if players.size()-p1index is < ratingdiff/2 get all players to right of p1Index, and then ratingdiff/2 - (players.size()-p1index) to left of p1index
            else if (p1Index + (ratingDiff / 2) < players.size()) {
                int indexesBeforePivot = p1Index - 1;
                int indexesAfterPivot = ratingDiff - (p1Index - 1);

                for (int i = p1Index; i >= 0; i--) {
                    ratingDiffArray.add(players.get(i));
                }

                for (int i = p1Index; i < indexesAfterPivot; i++) {
                    ratingDiffArray.add(players.get(i));
                }
            }
            // if p1index is < ratingdiff/2 get all players to left of p1Index, and then ratingdiff/2 - p1index to right of p1index
            else {
                int indexesAfterPivot = players.size() - p1Index;
                int indexesBeforePivot = (ratingDiff - indexesAfterPivot);
                for (int i = 1; i < indexesAfterPivot; i++) {
                    ratingDiffArray.add(players.get(p1Index + i));
                }

                for (int i = p1Index; i > (p1Index - indexesBeforePivot); i--) {
                    ratingDiffArray.add(players.get(i-1));
                }
            }
        }
        else
        {
            ratingDiffArray.addAll(players);
        }
        /*boolean lessThan9 = false;
        boolean looped = false;
        while (!lessThan9) {
            for (Player p : players) {
                if (!p.equals(p1)) {
                    if (p.getRating() <= pivotRating + ratingDiff && p.getRating() >= pivotRating - ratingDiff) {
                        ratingDiffArray.add(p);
                        if (ratingDiffArray.size() > 9 && looped) {
                            return ratingDiffArray;
                        }
                        if (ratingDiffArray.size() > 1000) {
                            return ratingDiffArray;
                        }

                    }
                }
            }
            if (ratingDiffArray.size() < 9) {
                looped = true;
                ratingDiff += 50;
            } else {
                lessThan9 = true;
            }
        }*/
        return ratingDiffArray;
    }

    public GameCalculator(int ratingDiff, ArrayList<Player> players, int gamesPlayed) throws SQLException {
        this.ratingDiff = ratingDiff;
        this.players = players;
        this.gamesPlayed = gamesPlayed;
    }

    // runs through set number of games, adjusting individual players elo based on their 'personal skill'
    // 'personal skill' isn't calculated in this program, it runs under the assumption that there is an existing rating system for skill (hltv rating, leetify, etc)
    // games are won or lost based on the average elo of a team, however, elo is gained or lost in differing amounts depending on the skill of the player
    // eg: a player with disproportionately high skill relative to their teammates would lose less elo on a team loss, and gain more on a team win
    // obviously, teams with players of closer skill levels would all gain / lose similar amounts
    // the opposite is true as well: players who have lower skill gain fewer elo on a win, and lose more on a loss, as they also have an elo disproportionate to their skill, just in the opposite direction
    // there is a cap and a floor to the elo gained or lost
    public void calculateGamesPersonalSkill() throws IOException {
        boolean firstTime = true;
        int k = 50;
        Random rng = new Random();

        long startTime = System.nanoTime();
        int gameNum = 0;
        while (gameNum < gamesPlayed) {
            if (gameNum % (gamesPlayed / 10) == 0 && gameNum != 0) {
                System.out.println(gameNum);
            }
            if (gameNum % (gamesPlayed / 500) == 0 && gameNum != 0) {
                outputGamesPersonalToCSV(allGamesPlayed, firstTime);
                firstTime = false;
            }
            if (players.isEmpty()) {
                players.addAll(playersUsed);
                playersUsed.clear();
                firstLoop = false;
                players.sort(Comparator.comparingInt(Player::getRating));
            }

            int p1Index = rng.nextInt(players.size());
            Player p1 = players.get(p1Index);
            players.remove(p1);

            HashSet<Player> ratingDiffSet = getPlayersInRatingDiff(p1, players, ratingDiff, p1Index);
            ArrayList<Player> ratingDiffArray = new ArrayList<>(ratingDiffSet);

            Player[] teams = new Player[10];
            teams[0] = p1;

            Collections.shuffle(ratingDiffArray);
            int i = 1;
            while (i < 10) {
                teams[i] = ratingDiffArray.get(i - 1);
                i++;
            }

            ratingDiffArray.clear();

            Arrays.sort(teams);

            Team t1 = new Team(teams[0], teams[2], teams[4], teams[6], teams[8]);
            Team t2 = new Team(teams[1], teams[3], teams[5], teams[7], teams[9]);

            Game game;
            if (t1.getAvgSkill() > t2.getAvgSkill()) {
                game = new Game(t1, t2);
            } else if (t2.getAvgSkill() > t1.getAvgSkill()) {
                game = new Game(t2, t1);
            } else {
                if (rng.nextInt(0, 1) == 0) {
                    game = new Game(t1, t2);
                } else {
                    game = new Game(t2, t1);
                }
            }

            allGamesPlayed.add(game);
            double medianWinningTeamPlayerSkill = game.getWinningTeam().getMedianSkillPlayer();
            double medianLosingTeamPlayerSkill = game.getLosingTeam().getMedianSkillPlayer();
            for (Player p : game.getWinningTeam().getTeam()) {
                p.addGameWon();
                // elo calculation for the expected win chance, to be used in comparison with actual win chance (chance of team winning vs whether they won or lost)
                double expectedWinChanceWinner = Math.pow(10, ((double) p.getRating() / 400)) /
                        (Math.pow(10, ((double) p.getRating() / 400)) + (Math.pow(10, (game.getLosingTeam().getAvgRating() / 400))));
                //double E_winner = Math.abs(1 / (1.0 + Math.pow(10.0, (Math.abs(p.getRating() - game.getLosingTeam().getAvgRating()) / 400))));
                double pointsGained;
                // how much higher the individuals' skill was compared to the median players skill in the team
                double skillDividedByMedianWin = p.getSkill() / medianWinningTeamPlayerSkill;
                if (skillDividedByMedianWin < POINTSCAP) {
                    if (skillDividedByMedianWin > POINTSFLOOR) {
                        pointsGained = Math.abs(k * (1 - expectedWinChanceWinner)) * skillDividedByMedianWin;
                    } else {
                        pointsGained = Math.abs(k * (1 - expectedWinChanceWinner)) * POINTSFLOOR;
                    }
                } else {
                    pointsGained = Math.abs(k * (1 - expectedWinChanceWinner)) * POINTSCAP;
                }
                p.setRating((int) (p.getRating() + Math.round(pointsGained)));

                playersUsed.add(p);
                players.remove(p);
            }
            for (Player p : game.getLosingTeam().getTeam()) {
                p.addGameLost();
                double expectedWinChanceLoser = Math.pow(10, ((double) p.getRating() / 400)) /
                        (Math.pow(10, ((double) p.getRating() / 400)) + (Math.pow(10, (game.getWinningTeam().getAvgRating() / 400))));
                double pointsLost;
                double skillDividedByMedianLost = medianLosingTeamPlayerSkill / p.getSkill();

                if (skillDividedByMedianLost > POINTSFLOOR) {
                    if (skillDividedByMedianLost < POINTSCAP) {
                        pointsLost = Math.abs(k * (0 - expectedWinChanceLoser)) * skillDividedByMedianLost;
                    } else {
                        pointsLost = Math.abs(k * (0 - expectedWinChanceLoser)) * POINTSCAP;
                    }
                } else {
                    pointsLost = Math.abs(k * (0 - expectedWinChanceLoser)) * POINTSFLOOR;
                }
                if ((p.getRating() - pointsLost) >= 25) {
                    p.setRating((int) (p.getRating() - Math.round(pointsLost)));
                }
                else
                {
                    p.setRating(25);
                }
                playersUsed.add(p);
                players.remove(p);
            }

            gameNum++;
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println("seconds taken: " + totalTime / 1000000000);
        System.out.println("finished Simulating " + allGamesPlayed.size() * 500 + " games");

        players.addAll(playersUsed);
        outputPlayersPersonalToCSV(players);
        if (!allGamesPlayed.isEmpty()) {
            outputGamesPersonalToCSV(allGamesPlayed, firstTime);
        }
        long endTime2 = System.nanoTime();
        long totalTime2 = endTime2 - startTime;
        System.out.println("seconds taken: " + totalTime2 / 1000000000);
        System.out.println("finished outputting players");
    }

    // this function simulates the current system, where players gain or lose elo equally irrespective of individual skill
    public void calculateGamesElo() throws IOException {
        boolean firstTime = true;
        int k = 50;
        Random rng = new Random();

        long startTime = System.nanoTime();
        int gameNum = 0;
        while (gameNum < gamesPlayed) {
            if (gameNum % (gamesPlayed / 10) == 0 && gameNum != 0) {
                System.out.println(gameNum);
            }
            if (gameNum % (gamesPlayed / 500) == 0 && gameNum != 0) {
                outputGamesToCSV(allGamesPlayed, firstTime);
                firstTime = false;
            }
            if (players.isEmpty()) {
                players.addAll(playersUsed);
                playersUsed.clear();
                firstLoop = false;
            }

            int p1Index = rng.nextInt(players.size());
            Player p1 = players.get(p1Index);
            players.remove(p1);

            //int pivotRating = p1.getRating();

            HashSet<Player> ratingDiffSet = getPlayersInRatingDiff(p1, players, ratingDiff, p1Index);
            ArrayList<Player> ratingDiffArray = new ArrayList<>(ratingDiffSet);

            Player[] teams = new Player[10];
            teams[0] = p1;

            Collections.shuffle(ratingDiffArray);
            int i = 1;
            while (i < 10) {
                teams[i] = ratingDiffArray.get(i - 1);
                i++;
            }

            ratingDiffArray.clear();

            Arrays.sort(teams);

            Team t1 = new Team(teams[0], teams[2], teams[4], teams[6], teams[8]);
            Team t2 = new Team(teams[1], teams[3], teams[5], teams[7], teams[9]);

            Game game;
            if (t1.getAvgSkill() > t2.getAvgSkill()) {
                game = new Game(t1, t2);
            } else if (t2.getAvgSkill() > t1.getAvgSkill()) {
                game = new Game(t2, t1);
            } else {
                if (rng.nextInt(0, 1) == 0) {
                    game = new Game(t1, t2);
                } else {
                    game = new Game(t2, t1);
                }
            }

            allGamesPlayed.add(game);
            for (Player p : game.getWinningTeam().getTeam()) {
                p.addGameWon();
                //Ea = Qa / (Qa + Qb)
                // Qa = 10^(Ra/400), Qb = 10^(Rb/400), Ra = rating player a, Rb = rating player B
                double expectedWinChanceWinner = Math.pow(10, ((double) p.getRating() / 400)) /
                        (Math.pow(10, ((double) p.getRating() / 400)) + (Math.pow(10, (game.getLosingTeam().getAvgRating() / 400))));
                //points gained = k factor x (actual score - expected score)
                int pointsGained = (int) Math.round(Math.abs(k * (1 - expectedWinChanceWinner)));
                //double E_winner = 1 / (1.0 + Math.pow(10.0, ((game.getLosingTeam().getAvgRating() - p.getRating()) / 400)));
                p.setRating(p.getRating() + pointsGained);
                playersUsed.add(p);
                players.remove(p);
            }
            for (Player p : game.getLosingTeam().getTeam()) {
                p.addGameLost();
                double expectedWinChanceLoser = Math.pow(10, ((double) p.getRating() / 400)) /
                        (Math.pow(10, ((double) p.getRating() / 400)) + (Math.pow(10, (game.getWinningTeam().getAvgRating() / 400))));
                double pointsLost = Math.round(Math.abs(k * (0 - expectedWinChanceLoser)));
                //double E_loser = 1 / (1.0 + Math.pow(10.0, (game.getWinningTeam().getAvgRating() - p.getRating()) / 400));
                if (p.getRating() - pointsLost >= 25) {
                    p.setRating((int) (p.getRating() - pointsLost));
                } else {
                    p.setRating(25);
                }
                playersUsed.add(p);
                players.remove(p);
            }

            gameNum++;
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println("seconds taken: " + totalTime / 1000000000);
        System.out.println("finished Simulating " + allGamesPlayed.size() * 50 + " games");

        players.addAll(playersUsed);
        outputPlayersToCSV(players);
        if (!allGamesPlayed.isEmpty()) {
            outputGamesToCSV(allGamesPlayed, firstTime);
        }
    }

    public void outputPlayersToCSV(ArrayList<Player> players) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("playerlist.csv"));

        if(!Files.exists(Path.of("playerlist.csv")))
        {
            bw.write("id,name,skill,rating,volatility,confidence,games_won,games_played,history\n");
        }
        for (Player p : players) {
            StringBuilder player = new StringBuilder();
            String truncatedUUID = p.getUuid().toString().replaceAll("-", "");
            player.append(truncatedUUID);
            player.append(",");
            player.append(p.getName());
            player.append(",");
            player.append(p.getSkill());
            player.append(",");
            player.append(p.getRating());
            player.append(",");
            player.append(p.getVolatility());
            player.append(",");
            player.append(p.getConfidence());
            player.append(",");
            player.append(p.getGamesWon());
            player.append(",");
            player.append(p.getGamesPlayed());
            player.append(",");
            StringBuilder eloHistory = new StringBuilder();
            eloHistory.append("\"[");
            for (int i : p.getEloHistory())
            {
                eloHistory.append(i);
                eloHistory.append(",");
            }
            eloHistory = new StringBuilder(eloHistory.substring(0, eloHistory.length() - 1));
            eloHistory.append("]\"");

            player.append(eloHistory);
            player.append("\n");
            bw.append(player.toString());
        }

        bw.close();
    }

    // outputs results to a csv. yes this is messy, and could use a library instead. I chose not to.
    public void outputPlayersPersonalToCSV(ArrayList<Player> players) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("playerlistpersonal.csv"));

        bw.write("id,name,skill,rating,volatility,confidence,games_won,games_played,history\n");
        for (Player p : players) {
            StringBuilder player = new StringBuilder();
            String truncatedUUID = p.getUuid().toString().replaceAll("-", "");
            player.append(truncatedUUID);
            player.append(",");
            player.append(p.getName());
            player.append(",");
            player.append(p.getSkill());
            player.append(",");
            player.append(p.getRating());
            player.append(",");
            player.append(p.getVolatility());
            player.append(",");
            player.append(p.getConfidence());
            player.append(",");
            player.append(p.getGamesWon());
            player.append(",");
            player.append(p.getGamesPlayed());
            player.append(",");
            StringBuilder eloHistory = new StringBuilder();
            eloHistory.append("\"[");
            for (int i : p.getEloHistory())
            {
                eloHistory.append(i);
                eloHistory.append(",");
            }
            eloHistory = new StringBuilder(eloHistory.substring(0, eloHistory.length() - 1));
            eloHistory.append("]\"");

            player.append(eloHistory);
            player.append("\n");
            bw.append(player.toString());
        }

        bw.close();
    }

    public void outputGamesToCSV (ArrayList<Game> games, Boolean firstTime) throws IOException {
        if(firstTime)
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter("games.csv"));
            bw.write("game_id, winning_team, losing_team\n");
            bw.close();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter("games.csv", true));
        for (Game g : games) {
            StringBuilder sb = new StringBuilder();
            String truncatedUUID = g.getGameID().toString().replaceAll("-", "");
            sb.append(truncatedUUID);
            sb.append(",\"");
            sb.append(g.getWinningTeam().getNames());
            sb.append("\",\"");
            sb.append(g.getLosingTeam().getNames());
            sb.append("\"\n");
            bw.append(sb.toString());
        }

        bw.close();
        allGamesPlayed.clear();
    }

    public void outputGamesPersonalToCSV (ArrayList<Game> games, Boolean firstTime) throws IOException {
        if(firstTime) {
            BufferedWriter bw = new BufferedWriter(new FileWriter("gamespersonal.csv", false));
            bw.write("game_id, winning_team, losing_team\n");
            bw.close();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter("gamespersonal.csv", true));

        for (Game g : games) {
            StringBuilder sb = new StringBuilder();
            String truncatedUUID = g.getGameID().toString().replaceAll("-", "");
            sb.append(truncatedUUID);
            sb.append(",\"");
            sb.append(g.getWinningTeam().getNames());
            sb.append("\",\"");
            sb.append(g.getLosingTeam().getNames());
            sb.append("\"\n");
            bw.append(sb.toString());
        }
        bw.close();
        allGamesPlayed.clear();
    }
}