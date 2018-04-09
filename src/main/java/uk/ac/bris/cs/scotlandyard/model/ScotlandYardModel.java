package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;
import static uk.ac.bris.cs.scotlandyard.model.Player.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.security.auth.callback.ConfirmationCallback;

import org.omg.IOP.TAG_RMI_CUSTOM_MAX_STREAM_FORMAT;

import java.util.*; // TODO: figure out what we actually need to import here (to solve errors for ImmutableGraph etc)
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.UndirectedGraph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {
	private List<Boolean> mRounds;
	private Graph<Integer, Transport> mGraph;
	private ArrayList<ScotlandYardPlayer> mPlayers = new ArrayList<>();
	private int mCurrentRound = NOT_STARTED;
	private int mMrXLastLocation = 0;
	private int mMovesPlayed = 0; // TODO: increment moves played every time someone makes a move
	private Optional<Colour> mLastPlayer = Optional.empty();
	private ArrayList<Colour> mWinners = new ArrayList<>();

	//Constructor
	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph, PlayerConfiguration mrX,
			PlayerConfiguration firstDetective, PlayerConfiguration... restOfTheDetectives) {
		this.mRounds = requireNonNull(rounds);
		this.mGraph = requireNonNull(graph);

		if (mRounds.isEmpty()) {
			throw new IllegalArgumentException("Empty mRounds");
		}

		if (this.mGraph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}

		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}

		// Loop over all detectives in temporary list to validate
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>(); // temporary list for validation

		configurations.add(mrX);
		configurations.add(firstDetective);

		// add configurations to temporary list
		for (PlayerConfiguration configuration : restOfTheDetectives) {
			configurations.add(requireNonNull(configuration));
		}

		// start processing all configurations
		// data stores for processed data
		Set<Integer> locations = new HashSet<>();
		this.mPlayers = new ArrayList<>(); //List of ScotlandYardPlayers (mutable)
		Set<Colour> colours = new HashSet<>();

		for (PlayerConfiguration configuration : configurations) {
			// Check if players have duplicated locations
			if (locations.contains(configuration.location)) {
				throw new IllegalArgumentException("Duplicate location");
			}
			locations.add(configuration.location);
			if (colours.contains(configuration.colour)) {
				throw new IllegalArgumentException("Duplicate colour");
			}
			colours.add(configuration.colour);

			// ticket check
			if (configuration.tickets.get(BUS) == null) {
				throw new IllegalArgumentException("Detective is missing BUS tickets");
			}

			if (configuration.tickets.get(TAXI) == null) {
				throw new IllegalArgumentException("Detective is missing TAXI tickets");
			}

			if (configuration.tickets.get(UNDERGROUND) == null) {
				throw new IllegalArgumentException("Detective is missing UNDERGROUND tickets");
			}

			if (configuration.colour.isDetective()) {
				if (requireNonNull(configuration.tickets.get(SECRET)) != 0) {
					throw new IllegalArgumentException("Detective should not have secret tickets");
				}
				if (requireNonNull(configuration.tickets.get(DOUBLE)) != 0) {
					throw new IllegalArgumentException("Detective should not have secret tickets");
				}
			}

			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour,
					configuration.location, configuration.tickets);
			this.mPlayers.add(player);
		}
	}

	public void processMove(Move move) {
		// TODO: finish this
		System.out.println("Move made: " + move.toString());
		this.mMovesPlayed++;
	}

	private Integer getMrXLocation() {
		return this.mMrXLastLocation;
	}

	/**
	 * Returns the colour of the next player to play this round
	 */
	private Colour nextPlayer(Colour current) {
		List<Colour> players = getPlayers();
		Colour result = BLACK; // initialise as black just incase
		int currentIndex = -1;

		for (Colour player : players) {
			if (player == current) {
				currentIndex = players.indexOf(player);
				if (currentIndex < players.size() - 1) {
					result = players.get(currentIndex + 1);
				}
			}
		}

		if (currentIndex < 0) {
			throw new RuntimeException("nextPlayer unable to generate index for Colour " + current);
		}

		return result;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// check if game over
		if (this.isGameOver()) {
			throw new IllegalStateException("startRotate called but the game is already over!");
		}

		Colour currentPlayerColour = this.getCurrentPlayer();
		Optional<ScotlandYardPlayer> currentPlayer = ScotlandYardPlayer.getByColour(this.mPlayers, currentPlayerColour);

		if (!currentPlayer.isPresent()) {
			throw new RuntimeException("Could not get current player instance");
		} else {
			ScotlandYardPlayer current = currentPlayer.get();
			// generate list of valid moves
			HashSet<Move> moves = new HashSet<>();

			Optional<Integer> location = getPlayerLocation(currentPlayerColour);

			// notify player to move via Player.makeMove
			// TODO: use empty list OR a list containing a single PassMove
			// TODO: replace fake list with generated valid moves

			if (location.isPresent()) {
				current.player().makeMove(this, location.get(), moves, (choice) -> this.processMove(choice));
			} else {
				throw new RuntimeException("empty Optional <Integer> (location)");
			}

			// TODO: figure out the rest of this

			// update model: last player (so the next time startRotate was called)
			Optional<Colour> updatedLastPlayer = Optional.of(currentPlayerColour);
			this.mLastPlayer = updatedLastPlayer;
		}
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> result = new ArrayList<>();
		for (ScotlandYardPlayer player : this.mPlayers) {
			result.add(player.colour());
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		HashSet<Colour> winners = new HashSet<>();
		Integer mrXLocation = 0;

		for (ScotlandYardPlayer player : this.mPlayers) {
			if (player.colour() == BLACK) {
				mrXLocation = player.location();
			} else {
				if (player.location() == mrXLocation) {
					winners.add(player.colour());
				}
			}
		}

		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> requestedLocation = Optional.empty();
		boolean playerFound = false;

		// look for the player
		for (ScotlandYardPlayer player : this.mPlayers) {
			if (!playerFound && player.colour() == colour) {
				playerFound = true;
				if (colour != BLACK) {
					requestedLocation = Optional.of(player.location());
				} else {
					requestedLocation = Optional.of(this.getMrXLocation());
				}
			}
		}

		return requestedLocation;
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		Optional<Integer> result = Optional.empty();
		for (ScotlandYardPlayer player : this.mPlayers) {
			if (player.colour() == colour) {
				result = Optional.of(player.tickets().get(ticket));
			}
		}

		return result;
	}

	private void winners(boolean isMrX) {
		for (Colour player : getPlayers()) {
			if ((player.isDetective() && !isMrX) || (player.isMrX() && isMrX)) {
				this.mWinners.add(player);
			}
		}
	}

	private boolean playerIsTicketless (Colour colour) {
	    return !(getPlayerTickets(colour, BUS).isPresent() || getPlayerTickets(colour, TAXI).isPresent() || getPlayerTickets(colour, UNDERGROUND).isPresent())
    }

    private ArrayList<Integer> getOccupiedLocations () {
	    ArrayList<Integer> output = new ArrayList<>();

        for (Colour colour: getPlayers()) {
            if (colour != BLACK) {
                Optional<Integer> loc = getPlayerLocation(colour);
                if (loc.isPresent()) {
                    output.add(loc.get());
                }
            }
        }

	    return output;
    }

    private Set<Move> getMoves (ScotlandYardPlayer player) {
	    Set<Move> result = new HashSet<>();



	    return result;
    }

	private boolean winCheckMrX() {
		// check if all detectives are ticketless
        boolean allTicketless = true;
        for (Colour colour: getPlayers()) {
            if(colour != BLACK) {
                allTicketless = allTicketless && playerIsTicketless(colour);
            }
        }

		// check if all detectives have exhausted valid moves
        boolean noValidMoves = false;


		// check if all rounds have been used up
        boolean noRoundsLeft = false;


        return allTicketless || noValidMoves || noRoundsLeft;
	}

	private boolean winCheckDetective() {
		// check if mrX is stuck

		// check if mrX has been captured

		return false;
	}

	@Override
	public boolean isGameOver() {
		boolean mrXWin = winCheckMrX();
		boolean playerWin = winCheckDetective();

		return mrXWin || playerWin;
	}

	@Override
	public Colour getCurrentPlayer() {
		if (this.mLastPlayer.isPresent()) {
			return nextPlayer(this.mLastPlayer.get());
		} else {
			return (BLACK);
		}
	}

	@Override
	public int getCurrentRound() {
		return mCurrentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(mRounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(mGraph);
	}

}
