package uk.ac.bris.cs.scotlandyard.model;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

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
	private Boolean mGameOverNotified = false;
	private ArrayList<Spectator> mSpectators = new ArrayList<>();

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
					System.out.println(String.format("gPL: %s @ %s", colour.toString(), player.location()));
					requestedLocation = Optional.of(player.location());
				} else {
					System.out.println(String.format("gPL: %s @ %s (MASKED)", colour.toString(), player.location()));
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
		System.out.println(String.format("getNextPlayer: %s -> ??", current.toString()));
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

		System.out.println(String.format("getNextPlayer: %s -> %s", current.toString(), result));

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
	private Set<Move> getMoves(Colour colour){
        System.out.println(String.format("getMoves(%s)", colour.toString()));
		Set<Move> output = new HashSet<>();
		// get moves for a given Colour
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
					for(Edge<Integer, Transport> doubleMoveOption : doubleMoveDestinations){
												Integer destination2 = getDestination(doubleMoveOption);
						Ticket transport2 = getTicket(doubleMoveOption);
						// TODO: finish double moves
						// TODO: add tickets for available valid double moves

                        boolean clashWithOtherPlayer = occupied.contains(destination) || occupied.contains(destination2);
						boolean sufficientTickets = (transport != transport2 && player.hasTickets(transport2))
								|| (transport == transport2 && player.hasTickets(transport2, 2) || player.hasTickets(SECRET, 2)); //TODO: check

                        if (!clashWithOtherPlayer && sufficientTickets) {
							TicketMove firstMove, secondMove;
							firstMove = new TicketMove(player.colour(), transport, destination);
							secondMove = new TicketMove(player.colour(), transport2, destination2);
							output.add(new DoubleMove(player.colour(), firstMove, secondMove));

							// enables support for double moves starting with a secret ticket
                            if (player.hasTickets(SECRET) && !clashWithOtherPlayer
									&& (sufficientTickets || (player.hasTickets(transport) || player.hasTickets(transport2)))) {
								TicketMove secretFirstMove = new TicketMove(player.colour(), SECRET, destination);
								TicketMove secretSecondMove = new TicketMove(player.colour(), SECRET, destination2);


								if (player.hasTickets(transport2)) {
									output.add(new DoubleMove(player.colour(), secretFirstMove, secondMove));
								}

								if (player.hasTickets(transport)) {
									output.add(new DoubleMove(player.colour(), firstMove, secretSecondMove));
								}

								if (player.hasTickets(SECRET, 2)){
									output.add(new DoubleMove(player.colour(), secretFirstMove, secretSecondMove));
								} // foo
							}
						}
						}
					}
				}
			}
		} else {
			throw new IllegalArgumentException("getMoves called with invalid colour (" + colour + ")");
		}

		return Collections.unmodifiableSet(output);
	}

	/**
	 * returns an immutable list of occupied locations
	 */
	private List<Integer> getOccupiedLocations() {
		System.out.println("getOccupiedLocations()");
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
		System.out.println("startRotate()");
		// check if game over
		if (this.isGameOver()) {
			throw new IllegalStateException("startRotate called but the game is already over!");
		}

		Colour currentPlayerColour = this.getCurrentPlayer();
		Optional<ScotlandYardPlayer> currentPlayer = ScotlandYardPlayer.getByColour(this.mPlayers, currentPlayerColour);

		if (!currentPlayer.isPresent()) {
			throw new IllegalArgumentException("Could not get current player instance");
		} else {
			ScotlandYardPlayer current = currentPlayer.get();
			// generate list of valid moves
			Set<Move> moves = getMoves(currentPlayerColour);

			Optional<Integer> location = getPlayerLocation(currentPlayerColour, true);

			// notify player to move via Player.makeMove
			// TODO: replace fake list with generated valid moves

			if (location.isPresent()) {
				System.out.println(String.format("startRotate: %s::makeMove will have %s choices", currentPlayerColour, moves.size()));
				current.player().makeMove(this, location.get(), moves, (choice) -> this.processMove(currentPlayerColour, choice));
				// update model: last player (so the next time startRotate was called)
			} else {
				throw new RuntimeException("empty Optional <Integer> (location)");
			}
		}
	}

	public void processMove(Colour colour, Move move) {
		System.out.println(String.format("processMove(%s, %s)", colour, move));
		// TODO: update last player
		Optional<Colour> updatedLastPlayer = Optional.of(colour);
		this.mLastPlayer = updatedLastPlayer;
		System.out.println("this.mLastPlayer -> " + colour.toString());

		// TODO: update the location and ticket counts
		Optional<ScotlandYardPlayer> oPlayer = ScotlandYardPlayer.getByColour(this.mPlayers, colour);
		if(oPlayer.isPresent()) {
			ScotlandYardPlayer player = oPlayer.get();
			if (move instanceof DoubleMove) {
				DoubleMove dbl = (DoubleMove) move;
				player.removeTicket(dbl.firstMove().ticket());
				player.removeTicket(dbl.secondMove().ticket());
				player.location(dbl.finalDestination());
				System.out.println(String.format("DoubleMove detected.. removing 2 tickets (%s + %s) and setting location to %s", dbl.firstMove().ticket(), dbl.secondMove().ticket(), dbl.finalDestination()));
			} else if (move instanceof TicketMove) {
				TicketMove tkt = (TicketMove) move;
				System.out.println(String.format("Standard TicketMove detected.. removing %s ticket.", tkt.ticket()));
				player.removeTicket(tkt.ticket());
				player.location(tkt.destination());
			}
		} else {
			throw new IllegalArgumentException("processMove could not find the right ScotlandYardPlayer for colour (" + colour + ")");
		}

		// update currentRound
		System.out.println("ROUND "+ mCurrentRound+ " : " + colour.toString() + " " + move.toString());

		int roundCopy = this.mCurrentRound;

		if(move instanceof DoubleMove){
			this.mCurrentRound += 2;

		} else {
			this.mCurrentRound++;
		}

		System.out.println(String.format("mCurrentRound = %s -> %s", roundCopy, mCurrentRound));

		if(this.mSpectators.size() > 0){
			System.out.println("Notifying spectators");
			spectatorNotifyMove(move);
			if(getNextPlayer(colour) == BLACK){
				spectatorNotifyRotation();
			}
			if (isGameOver()) {
				spectatorNotifyGameOver();
			}
		}

		if(!isGameOver()){
			System.out.println("Rotating for the next player");
			startRotate();
		} else {
			System.out.println("startRotate: Game is over!");
		}
	}

	/** END ROTATION+MOVEMENT LOGIC SECTION */

	/** WIN CHECKING SECTION */
	/**
	 * setWinningPlayers
	 * populates `this.mWinners` list with winning players
	 * (bool) isMrX - if mrX is the winner true, else false
	 */
	private void setWinningPlayers(boolean isMrX) {
		if (isMrX) {
			System.out.println("MR X HAS WON");
		} else {
			System.out.println("DETECTIVES HAVE WON");
		}
		this.mWinners.clear();
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
		System.out.println("Checking if MrX has won");
		boolean result = false;

		// TODO: all detectives are ticketless
		boolean ticketless = true;
		for(ScotlandYardPlayer player : this.mPlayers){
			if(player.isDetective() && !player.hasNoTickets()){
				ticketless = false;
			}
		}
		if (ticketless) {
			System.out.println("MR X WIN: All players are ticketless");
		}

		// TODO: all detectives have 0 valid moves available
		boolean moveless = true;
		for(ScotlandYardPlayer player : this.mPlayers){
			if(player.isDetective() && getMoves(player.colour()).size() != 0){
				moveless = false;
			}
		}

		if (moveless) {
			System.out.println("MR X WIN: All players are moveless");
		}

		// TODO: max rounds have been played
		boolean roundless = getCurrentRound() >= getRounds().size();

		if (roundless) {
			System.out.println("MR X WIN: No rounds left");
		}

		result = ticketless || moveless || roundless;
		return result;
	}

	/**	returns true if detectives have won
	 * detective's win conditions:
	 * 	- mrX is stuck
	 *  - mrX is captured
	 */
	private boolean checkWinDetective() {
		System.out.println("Checking if detectives have won");
		boolean result = false;

		// TODO: mrX is stuck
		boolean stuck = getMoves(BLACK).isEmpty();

		// TODO: mrX is captured
		boolean captured = false;
		Integer mrXlocation = -1;
		Optional<Integer> oLoc = getPlayerLocation(BLACK, true);
		if (oLoc.isPresent()){
			mrXlocation = oLoc.get();
		}
		if (getOccupiedLocations().contains(mrXlocation)) {
            captured = true;
		}

		result = stuck || captured;
		return result;
	}

	public boolean isGameOver() {
		boolean result;
		if(mCurrentRound == NOT_STARTED){
			result = false;
		} else {
			boolean mrXWin = checkWinMrX();
			boolean playerWin = checkWinDetective();
			if(mrXWin){
				setWinningPlayers(true);
			} else if (playerWin) {
				setWinningPlayers(false);
			}
			result = mrXWin || playerWin;
		}
		if(result){
			System.out.println("GAME OVER");
		} else {
			System.out.println("GAME STILL IN PROGRESS");
		}
		return result;
	}

	/** END WIN CHECKING SECTION */
// overloaded version to check if it will be a reveal round in x rounds from now
    public boolean isRevealRound(Integer x) {
        int currentRound = getCurrentRound();
        return getRounds().size() > (currentRound + x - 1)
                && getRounds().get(currentRound + x - 1);
    }


    public boolean isRevealRound() {
        return isRevealRound(0);
		}
	/** REVEAL ROUND SECTION */

	/** END REVEAL ROUND SECTION */

	/** SPECTATOR SECTION */
	@Override
	public void registerSpectator(Spectator spectator) {
		System.out.println("Registering a spectator");
		requireNonNull(spectator);
		if (!getSpectators().contains(spectator)){
			this.mSpectators.add(spectator);
		} else {
			throw new IllegalArgumentException("Duplicate spectator");
		}
	}


	@Override
	public void unregisterSpectator(Spectator spectator) {
		System.out.println("Unregistering spectator");
		// TODO
		requireNonNull(spectator);

		if (!getSpectators().contains(spectator)) {
				throw new IllegalArgumentException("Spectator not found");
		}

		this.mSpectators.remove(spectator);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		return Collections.unmodifiableList(this.mSpectators);
	}

	private void spectatorNotifyGameOver(){
		System.out.println("NOTIFICATION: GAME OVER");
		Collection<Spectator> specs = getSpectators();

		if(!getSpectators().isEmpty() && !this.mGameOverNotified){
			for (Spectator spec : specs) {
				spec.onGameOver(this, this.getWinningPlayers());
			}
			this.mGameOverNotified = true;
		}
	}

	private void spectatorNotifyMove(Move move){
		Collection<Spectator> specs = getSpectators();
		System.out.println(String.format("NOTIFICATION(%s): Move (%s)", specs.size(), move));

		if(!getSpectators().isEmpty() && !this.mGameOverNotified){
			for (Spectator spec : specs) {
				spec.onMoveMade(this, move);
			}
		}
	}

	private void spectatorNotifyRotation(){
		Collection<Spectator> specs = getSpectators();
		System.out.println(String.format("NOTIFICATION(%s): GAME OVER", specs.size()));

		if(!getSpectators().isEmpty() && !this.mGameOverNotified){
			for (Spectator spec : specs) {
				spec.onRotationComplete(this);
			}
		}
	}
	/** END SPECTATOR SECTION */
}
