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
	private Colour mCurrentPlayer = BLACK;
	private Optional<Colour> mLastPlayer = Optional.empty();
	private ArrayList<Colour> mWinners = new ArrayList<>();

	//Constructor
	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph, PlayerConfiguration mrX,
			PlayerConfiguration firstDetective, PlayerConfiguration... restOfTheDetectives) {

		// data stores for processed data
		this.mRounds = requireNonNull(rounds);
		this.mGraph = requireNonNull(graph);
		Set<Integer> locations = new HashSet<>();
		this.mPlayers = new ArrayList<>(); //List of ScotlandYardPlayers (mutable)
		Set<Colour> colours = new HashSet<>();
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>(); // temporary list for validation

		// basic sanity-check validation
		if (mRounds.isEmpty()) {
			throw new IllegalArgumentException("Empty mRounds");
		}

		if (this.mGraph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}

		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}
		configurations.add(mrX);
		configurations.add(firstDetective);

		// Loop over all detectives in temporary list to validate
		for (PlayerConfiguration configuration : restOfTheDetectives) {
			// add configurations to temporary list
			configurations.add(requireNonNull(configuration));
		}

		// start processing all configurations
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
					throw new IllegalArgumentException("Detective should not have SECRET tickets");
				}
				if (requireNonNull(configuration.tickets.get(DOUBLE)) != 0) {
					throw new IllegalArgumentException("Detective should not have SECRET tickets");
				}
			}

			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour,
					configuration.location, configuration.tickets);
			this.mPlayers.add(player);
		}
	}

	/** GENERIC GETTERS SECTION */
	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> result = new ArrayList<>();
		for (ScotlandYardPlayer player : this.mPlayers) {
			result.add(player.colour());
		}
		return Collections.unmodifiableList(result);
	}

	/** overloaded version of getPlayerLocation
	 * the second argument `forceMrX` can be set to true
	 * if true, returns mrX's true location even on non-reveal rounds,
	 * if false, returns mrX's last revealed location
	 **/
	public Optional<Integer> getPlayerLocation(Colour colour, boolean forceMrX) {
		Optional<Integer> requestedLocation = Optional.empty();
		boolean playerFound = false;

		// look for the player
		for (ScotlandYardPlayer player : this.mPlayers) {
			if (!playerFound && player.colour() == colour) {
				playerFound = true;
				if (colour != BLACK || forceMrX) {
					requestedLocation = Optional.of(player.location());
				} else {
					requestedLocation = Optional.of(this.getMrXLocation());
				}
			}
		}

		return requestedLocation;
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		return getPlayerLocation(colour, false);
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

	@Override
	public Colour getCurrentPlayer() {
		if (this.mLastPlayer.isPresent()) {
			return getNextPlayer(this.mLastPlayer.get());
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

	private Integer getMrXLocation() {
		return this.mMrXLastLocation;
	}

	/**
	 * Returns the colour of the next player to play this round
	 */
	private Colour getNextPlayer(Colour current) {
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
			throw new RuntimeException("getNextPlayer unable to generate index for Colour " + current);
		}

		return result;
	}

	Integer getDestination(Edge<Integer, Transport> input) {
		// TODO: implement getDestination
		return input.destination().value();
	}

	Ticket getTicket(Edge<Integer, Transport> input) {
		// TODO: implement getDestination
		return Ticket.fromTransport(input.data());
	}

	Collection<Edge<Integer, Transport>> getOptions(Integer input) {
		// TODO: implement getOptions
		Collection<Edge<Integer, Transport>> output = getGraph().getEdgesFrom(getGraph().getNode(input));
		return Collections.unmodifiableCollection(output);
	}

	// TODO: finish getMoves
	/**
	 * getMoves
	 * returns an unmodifiable set of valid moves for a specific player (uses `colour`)
	 * see also: getOccupiedLocations, getOptions, getDestination, getTicket,
	 */
	private Set<Move> getMoves(Colour colour) {
		Set<Move> output = new HashSet<>();
		// TODO: get moves for a given Colour
		System.out
				.println("** getMoves for " + colour + " FAIL - (not yet implemented) - returning an empty set of moves **");

		Optional<ScotlandYardPlayer> p = ScotlandYardPlayer.getByColour(this.mPlayers, colour);
		ScotlandYardPlayer player;

		if (p.isPresent()) {
			player = p.get();
			List<Integer> occupied = getOccupiedLocations();
			Collection<Edge<Integer, Transport>> options = getOptions(player.location());
			for (Edge<Integer, Transport> option : options) {
				Integer destination = getDestination(option);
				Ticket transport = getTicket(option);
				if (!occupied.contains(destination)) {
					if (player.hasTickets(transport)) {
						output.add(new TicketMove(player.colour(), transport, destination));
					}
					if (player.hasTickets(SECRET)) {
						output.add(new TicketMove(player.colour(), SECRET, destination));
					}
					if (player.hasTickets(DOUBLE)) {// TODO: check if sufficient rounds remaining for a double move
						Collection<Edge<Integer, Transport>> doubleMoveDestinations = getOptions(destination);
						Integer destination2 = getDestination(option);
						Ticket transport2 = getTicket(option);
						// TODO: finish double moves
						// TODO: add tickets for available valid double moves
						boolean secondDestinationAlreadyOccupied = occupied.contains(destination2);
						boolean sufficientTickets = (transport != transport2 && player.hasTickets(transport2))
								|| (transport == transport2 && player.hasTickets(transport2, 2)); //TODO: check

						if (!secondDestinationAlreadyOccupied && sufficientTickets) {
							TicketMove firstMove, secondMove;
							firstMove = new TicketMove(player.colour(), transport, destination);
							secondMove = new TicketMove(player.colour(), transport2, destination2);
							output.add(new DoubleMove(player.colour(), firstMove, secondMove));

							// enables support for double moves starting with a secret ticket
							if (player.hasTickets(SECRET) && !secondDestinationAlreadyOccupied
									&& (sufficientTickets || (player.hasTickets(transport) || player.hasTickets(transport2)))) {
								TicketMove secretFirstMove = new TicketMove(player.colour(), SECRET, destination);
								TicketMove secretSecondMove = new TicketMove(player.colour(), SECRET, destination2);

								if (player.hasTickets(transport2)) {
									output.add(new DoubleMove(player.colour(), secretFirstMove, secondMove));
								}

								if (player.hasTickets(transport)) {
									output.add(new DoubleMove(player.colour(), firstMove, secretSecondMove));
								}
							}
						}
					}
				}
			}
		} else {
			throw new RuntimeException("getMoves called with invalid colour (" + colour + ")");
		}

		return Collections.unmodifiableSet(output);
	}

	/**
	 * returns an immutable list of occupied locations
	 */
	private List<Integer> getOccupiedLocations() {
		ArrayList<Integer> output = new ArrayList<Integer>();
		for (ScotlandYardPlayer player : this.mPlayers) {
			if (player.isDetective()) {
				output.add(player.location());
			}
		}
		return Collections.unmodifiableList(output);
	}

	/** END GETTERS SECTION */

	/** ROTATION AND MOVEMENT LOGIC SECTION */
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
			Set<Move> moves = getMoves(currentPlayerColour);

			Optional<Integer> location = getPlayerLocation(currentPlayerColour, true);

			// notify player to move via Player.makeMove
			// TODO: replace fake list with generated valid moves

			if (location.isPresent()) {
				current.player().makeMove(this, location.get(), moves,
						(choice) -> this.processMove(currentPlayerColour, choice));
				// update model: last player (so the next time startRotate was called)
			} else {
				throw new RuntimeException("empty Optional <Integer> (location)");
			}
		}
	}

	public void processMove(Colour colour, Move move) {
		// TODO: finish this
		// TODO: update the location
		// TODO: increment movesPlayed / currentRound
		// TODO: update last player
		Optional<Colour> updatedLastPlayer = Optional.of(colour);
		this.mLastPlayer = updatedLastPlayer;
		System.out.println("Move made: " + move.toString());
	}

	/** END ROTATION+MOVEMENT LOGIC SECTION */

	/** WIN CHECKING SECTION */
	/**
	 * setWinningPlayers
	 * populates `this.mWinners` list with winning players
	 * (bool) isMrX - if mrX is the winner true, else false
	 */
	private void setWinningPlayers(boolean isMrX) {
		this.mPlayers.clear();
		for (Colour player : getPlayers()) {
			if ((player.isDetective() && !isMrX) || (player.isMrX() && isMrX)) {
				this.mWinners.add(player);
			}
		}
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		Set<Colour> output = new HashSet<Colour>(this.mWinners);
		return Collections.unmodifiableSet(output);
	}

	/**	returns true if mrX has won
	 * mrX's win conditions:
	 * 	- all detectives are ticketless
	 *  - all detectives have 0 valid moves available
	 *  - max rounds have been played
	 */
	private boolean checkWinMrX() {
		boolean result = false;

		// TODO: all detectives are ticketless

		// TODO: all detectives have 0 valid moves available

		// TODO: max rounds have been played

		return result;
	}

	/**	returns true if detectives have won
	 * detective's win conditions:
	 * 	- mrX is stuck
	 *  - mrX is captured
	 */
	private boolean checkWinDetective() {
		boolean result = false;

		// TODO: mrX is stuck

		// TODO: mrX is captured

		return result;
	}

	@Override
	public boolean isGameOver() {
		boolean mrXWin = checkWinMrX();
		boolean playerWin = checkWinDetective();
		return mrXWin || playerWin;
	}

	/** END WIN CHECKING SECTION */

	/** SPECTATOR SECTION */
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
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	/** END SPECTATOR SECTION */
}
