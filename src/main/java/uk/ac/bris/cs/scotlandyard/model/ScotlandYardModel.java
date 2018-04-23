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
	private ArrayList<ScotlandYardPlayer> mPlayers;
	private int mCurrentRound = NOT_STARTED;
	private int mMrXLastLocation = 0;
	private int mMovesPlayed = 0; // TODO: increment moves played every time someone makes a move
	private Colour mRotating = BLACK;
	private Optional<Colour> mLastPlayer = Optional.empty();
	private ArrayList<Colour> mWinners = new ArrayList<>();
	private boolean mGameOverNotified;
	private ArrayList<Spectator> mSpectators = new ArrayList<>();
	private Boolean mGameStarted = false;

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

	private void DEBUG_LOG (String input){
		String output = String.format("%s(%s) [%s/%s] - %s", this.mCurrentRound, this.mRotating.toString(), getRotatorIndex(this.mRotating), this.getPlayers().size(), input);
		System.out.println(output);
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


	private void saveMrXLocation (int location) {

		Optional<ScotlandYardPlayer> oMrX = ScotlandYardPlayer.getMrX(this.mPlayers);
		if(oMrX.isPresent()){
			if(this.mMrXLastLocation != location) {
				this.mMrXLastLocation = location;
				oMrX.get().location(location);
				DEBUG_LOG(String.format("saveMrXLocation %s -> %s", this.mMrXLastLocation, location));
			}
		} else {
			throw new IllegalStateException("cannot save mrX's location - cannot get MrX's ScotlandYardPlayer instance");
		}
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
					// System.out.println(String.format("gPL: %s @ %s", colour.toString(), player.location()));
					requestedLocation = Optional.of(player.location());
				} else {
					// System.out.println(String.format("gPL: %s @ %s (MASKED)", colour.toString(), player.location()));
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

	public int getRoundsRemaining() {
		return getRounds().size() - getCurrentRound();
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(mGraph);
	}

	private Integer getMrXLocation() {
		if (getCurrentRound() == 0) {
			return 0;
		} else if (isRevealRound() && this.mGameStarted) {
			Optional<ScotlandYardPlayer> oMrX = ScotlandYardPlayer.getMrX(this.mPlayers);
			if (oMrX.isPresent()) {
				saveMrXLocation(oMrX.get().location());
			} else {
				throw new IllegalStateException("getMrXLocation could not get the ScotlandYardPlayer for MrX");
			}
		}
		return this.mMrXLastLocation;
	}

	/**
	 * Returns the colour of the next player to play this round
	 */
	private Colour getNextPlayer(Colour current) {
//		System.out.println(String.format("getNextPlayer: %s -> ??", current.toString()));
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

		// System.out.println(String.format("getNextPlayer: %s -> %s", current.toString(), result));

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
//        DEBUG_LOG(String.format("getMoves(%s)", colour.toString()));
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
					if (player.hasTickets(DOUBLE) && getRoundsRemaining() >= 2) {// TODO: check if sufficient rounds remaining for a double move
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

		if(output.isEmpty() && player.isDetective()) {
			output.add(new PassMove(colour));
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
//		DEBUG_LOG("getOccupiedLocations() = " + output);
		return Collections.unmodifiableList(output);
	}

	/** END GETTERS SECTION */

	/** ROTATION AND MOVEMENT LOGIC SECTION */
	@Override
	public void startRotate() {
		DEBUG_LOG(String.format("startRotate() - current round is %s, reveal rounds are %s", getCurrentRound(), getRevealRounds()));

		// check if game over
		if (!this.isGameOver()) {
			playerTurn();
		} else {
			throw new IllegalStateException("startRotate called but game is already over");
		}
	}

	private Integer getRotatorIndex (Colour input) {
		Integer result = 0;
		List<Colour> players = this.getPlayers();
		for (int i = 0; i < this.getPlayers().size() ; i++) {
			if(players.get(i) == input){
				result = i + 1;
			}
		}
		return result;
	}

	public void playerTurn() {
		Colour currentPlayerColour = this.getCurrentPlayer();
		this.mRotating = currentPlayerColour;
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
				// reveal round logic
				if(currentPlayerColour == BLACK && isRevealRound()){
					int last = this.mMrXLastLocation;
					saveMrXLocation(location.get());
					DEBUG_LOG(String.format("Reveal round: mMrXLastLocation = %s -> %s", last, this.mMrXLastLocation));
				}

				// prompt player for move
				DEBUG_LOG(String.format("startRotate: %s @ %s ::makeMove will have %s choices", currentPlayerColour, location.get(), moves.size()));
				isGameOver(); // just to update the model's attributes
				current.player().makeMove(this, location.get(), moves, (choice) -> this.processMove(currentPlayerColour, choice));

			} else {
				throw new RuntimeException("location is missing");
			}
		}
	}

	private void nextRound(int diff){
		this.mGameStarted = true;
		this.mCurrentRound += diff;
		spectatorNotifyRoundStarted();
	}

	private void nextRound() {
		this.nextRound(1);
	}

	private List<Integer> getRevealRounds () {
		List<Boolean> rounds = this.getRounds();
		ArrayList<Integer> output = new ArrayList();

		for(int i = 0; i < rounds.size(); i++){
			if(rounds.get(i)){
				output.add(i);
			}
		}

		return Collections.unmodifiableList(output);
	}

	public void processMove(Colour colour, Move move) {
		requireNonNull(colour);
		requireNonNull(move);

		if (move instanceof DoubleMove) {
			this.mMovesPlayed += 2;
		} else if (move instanceof TicketMove || move instanceof PassMove) {
			this.mMovesPlayed++;
		}

		// Check that the move was one of the valid moves we provided
		Optional<Integer> oLoc = getPlayerLocation(colour, true);

		if(!(oLoc.isPresent() && getMoves(colour).contains(move))){
			throw new IllegalArgumentException("that wasn't one of the moves we provided!");
		} else {
			// TODO: update last player
			Optional<Colour> updatedLastPlayer = Optional.of(colour);
			this.mLastPlayer = updatedLastPlayer;
			DEBUG_LOG("this.mLastPlayer -> " + colour.toString());
			DEBUG_LOG(String.format("processMove(%s, %s)", colour, move));
			int roundCopy = this.mCurrentRound;

			// TODO: update the location and ticket counts
			Optional<ScotlandYardPlayer> oPlayer = ScotlandYardPlayer.getByColour(this.mPlayers, colour);
			if(oPlayer.isPresent()) {
				ScotlandYardPlayer player = oPlayer.get();
				DEBUG_LOG("processMove@start: " + player.toString());
				if (move instanceof DoubleMove) {
					DoubleMove dbl = (DoubleMove) move;
					TicketMove firstMove, secondMove;
					if(isRevealRound()) {
						firstMove = new TicketMove(colour, dbl.firstMove().ticket(), dbl.firstMove().destination());
						saveMrXLocation(dbl.firstMove().destination());
						DEBUG_LOG("updating MrX's public location to " + dbl.firstMove().destination());
					} else {
						firstMove = new TicketMove(colour, dbl.firstMove().ticket(), this.mMrXLastLocation);
					}

					if(isRevealRound(1)){ // next turn is reveal
						secondMove = new TicketMove(colour, dbl.secondMove().ticket(), dbl.secondMove().destination());
						saveMrXLocation(dbl.secondMove().destination());
					} else {
						secondMove = new TicketMove(colour, dbl.secondMove().ticket(), this.mMrXLastLocation);
					}

					DoubleMove toNotify = new DoubleMove(colour, requireNonNull(firstMove), requireNonNull(secondMove));
					player.removeTicket(DOUBLE);
					spectatorNotifyMove(toNotify);
					player.removeTicket(dbl.firstMove().ticket());
					nextRound();
					spectatorNotifyMove(firstMove);
					DEBUG_LOG(String.format("DoubleMove ticket processing: removed 1st ticket (%s). New count = %s", dbl.firstMove().ticket(), getPlayerTickets(colour, dbl.firstMove().ticket())));
					player.removeTicket(dbl.secondMove().ticket());
					player.location(dbl.firstMove().destination());
					nextRound();
					spectatorNotifyMove(secondMove);
					DEBUG_LOG(String.format("DoubleMove ticket processing: removed 2nd ticket (%s). New count = %s", dbl.secondMove().ticket(), getPlayerTickets(colour, dbl.secondMove().ticket())));
					player.location(dbl.finalDestination());
					DEBUG_LOG(String.format("DoubleMove detected.. removing 2 tickets (%s + %s) and setting location to %s", dbl.firstMove().ticket(), dbl.secondMove().ticket(), dbl.finalDestination()));
				} else if (move instanceof TicketMove) {
					TicketMove tkt = (TicketMove) move;
					TicketMove toNotify = tkt;
					DEBUG_LOG(String.format("TicketMove(%s)detected.. removing %s ticket.", tkt.ticket(), tkt.ticket()));
					player.removeTicket(tkt.ticket());
					if(player.isMrX()){
						if(isRevealRound(1)){
							DEBUG_LOG("will be a reveal round when nextRound is called so pre-emptively saving mrX's location");
							if(tkt.ticket() != SECRET){
								saveMrXLocation(tkt.destination());
							} else {
								DEBUG_LOG("SECRET MOVE: not pre-emptively saving MrX's location");
							}
						}
						nextRound();
						if (!isRevealRound()) {
							toNotify = new TicketMove(colour, tkt.ticket(), this.mMrXLastLocation);
						} else {
							if(tkt.ticket() != SECRET){
								saveMrXLocation(tkt.destination());
							}
							toNotify = new TicketMove(colour, tkt.ticket(), tkt.destination());
						}
					} else {
						DEBUG_LOG("giving the ticket to Mr X");
						Optional<ScotlandYardPlayer> oMrX = ScotlandYardPlayer.getByColour(this.mPlayers, BLACK);
						ScotlandYardPlayer mrX;
						if (oMrX.isPresent()){
							mrX = oMrX.get();
							mrX.addTicket(tkt.ticket());
						} else {
							throw new IllegalStateException("processMove failed to add ticket to mr X - unable to get Mr X's scotlandyardplayer instance");
						}
					}

					player.location(tkt.destination());
					spectatorNotifyMove(toNotify);
				} else if (move instanceof PassMove) {
					DEBUG_LOG(String.format("%s PASSES", colour.toString()));
					spectatorNotifyMove((PassMove) move);
				} else {
					throw new IllegalStateException(String.format("move (%s) is not an instance of DoubleMove, TicketMove or PassMove. Wtf?", move));
				}
				DEBUG_LOG("processMove@end: " + player.toString());
			} else {
				throw new IllegalArgumentException("processMove could not find the right ScotlandYardPlayer for colour (" + colour + ")");
			}

			System.out.println("ROUND "+ mCurrentRound+ ": " + colour.toString() + " " + move.toString());

			if(colour == BLACK){
				// update currentRound
				DEBUG_LOG(String.format("mCurrentRound = %s -> %s", roundCopy, mCurrentRound));
			}

			if(getCurrentPlayer() == BLACK){
				if(isGameOver()) {
					spectatorNotifyGameOver();
				} else {
					spectatorNotifyRotation();
				}
			} else {
				playerTurn();
			}
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
			DEBUG_LOG("MR X HAS WON");
		} else {
			DEBUG_LOG("DETECTIVES HAVE WON");
		}
		this.mWinners.clear();
		for (Colour player : getPlayers()) {
			if (isMrX) {
				if(player.isMrX()) {
					this.mWinners.add(player);
				}
			} else {
				if(player.isDetective()){
					this.mWinners.add(player);
				}
			}
		}

		DEBUG_LOG(String.format("winners: %s", this.mWinners));
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
//		DEBUG_LOG("Checking if MrX has won");
		boolean result = false;

		// TODO: all detectives are ticketless
		boolean ticketless = true;
		for(ScotlandYardPlayer player : this.mPlayers){
			if(player.isDetective() && !player.hasNoTickets()){
				ticketless = false;
			}
		}

		// TODO: all detectives have 0 valid moves available
		boolean moveless = true;
		for(ScotlandYardPlayer player : this.mPlayers) {
			if (player.isDetective() && getMoves(player.colour()).size() != 0) {
				moveless = false;
			}
		}

		// TODO: max rounds have been played
		boolean roundless = getCurrentRound() >= getRounds().size() && getCurrentPlayer() == BLACK;

		result = ticketless || moveless || roundless;
		if (result) {
			DEBUG_LOG(String.format("Mr X Win: Tickets? (%s) Moves? (%s) Rounds? (%s)", ticketless, moveless, roundless));
		}
		return result;
	}

	/**	returns true if detectives have won
	 * detective's win conditions:
	 * 	- mrX is stuck
	 *  - mrX is captured
	 */
	private boolean checkWinDetective() {
//		DEBUG_LOG("Checking if detectives have won");
		boolean result = false;

		// TODO: mrX is stuck
		boolean stuck = getCurrentPlayer().isMrX() && getMoves(BLACK).isEmpty();

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
		if (result) {
			DEBUG_LOG(String.format("Detective Win: Stuck? (%s) Captured? (%s)", stuck, captured));
		}
		return result;
	}

	public boolean isGameOver() {
		boolean result;
//			DEBUG_LOG("isGameOver?");
			boolean mrXWin = checkWinMrX();
			boolean playerWin = checkWinDetective();
			if(mrXWin){
				setWinningPlayers(true);
			} else if (playerWin) {
				setWinningPlayers(false);
			}
			result = mrXWin || playerWin;

		if(result) {
			DEBUG_LOG("GAME OVER");
		}
//		} else {
//			DEBUG_LOG("GAME STILL IN PROGRESS");
//		}
		return result;
	}

	/** END WIN CHECKING SECTION */
// overloaded version to check if it will be a reveal round in x rounds from now
    public boolean isRevealRound(Integer x) {
		boolean result = false;
		List<Boolean> rounds = getRounds();
		int currentRound = getCurrentRound();
		int numRounds = rounds.size();

		if (numRounds < 1) {
			throw new IllegalStateException("numRounds should be non-zero");
		}
		if (currentRound + x > numRounds) {
			throw new IllegalStateException(String.format("isRevealRound(%s) is invalid when max rounds is %s and current round is %s", x, numRounds, currentRound));
		} else if (currentRound > 0) {
			result = getRounds().get((currentRound + x - 1));
		} else if (currentRound == 0) {
			if (x >= numRounds) {
				result = false;
			} else {
				result = getRounds().get(x);
			}
		}
		// DEBUG_LOG(String.format("isRevealRound(%s): curr = %s, rounds[%s], ans = %s", x, getCurrentRound(), getRounds().size(), result ? "true" : "false"));
//		if (result) {
//			DEBUG_LOG(String.format("isRevealRound(%s) on round  %s = TRUE (%s)", x, currentRound, rounds));
//		}

		return result;
    }


    public boolean isRevealRound() {
        return isRevealRound(0);
		}
	/** REVEAL ROUND SECTION */

	/** END REVEAL ROUND SECTION */

	/** SPECTATOR SECTION */
	@Override
	public void registerSpectator(Spectator spectator) {
		DEBUG_LOG("Registering a spectator");
		requireNonNull(spectator);
		if (!getSpectators().contains(spectator)){
			this.mSpectators.add(spectator);
		} else {
			throw new IllegalArgumentException("Duplicate spectator");
		}
	}


	@Override
	public void unregisterSpectator(Spectator spectator) {
		DEBUG_LOG("Unregistering spectator");
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
		if(!isGameOver()){
			throw new IllegalStateException("spectatorNotifyGameOver called but game is not over yet");
		} else {
			DEBUG_LOG(String.format("NOTIFICATION: GAME OVER (Winners: %s) - %s rounds and %s moves played", getWinningPlayers(), this.mCurrentRound, this.mMovesPlayed));
			Collection<Spectator> specs = getSpectators();

			if(!getSpectators().isEmpty() && !this.mGameOverNotified){
				for (Spectator spec : specs) {
					spec.onGameOver(this, this.getWinningPlayers());
				}
				this.mGameOverNotified = true;
			}
		}
	}

	private void spectatorNotifyRoundStarted() {
		Collection<Spectator> specs = getSpectators();
		DEBUG_LOG(String.format("NOTIFICATION(%s): Round %s Started", specs.size(), getCurrentRound()));

		if(!getSpectators().isEmpty()){
			for (Spectator spec : specs) {
				spec.onRoundStarted(this, getCurrentRound());
			}
		}
	}

	private void spectatorNotifyMove(Move move){
		Collection<Spectator> specs = getSpectators();
		DEBUG_LOG(String.format("NOTIFICATION(%s): Move (%s)", specs.size(), move));
		isGameOver();
		if(!getSpectators().isEmpty()){
			for (Spectator spec : specs) {
				spec.onMoveMade(this, move);
			}
		}
	}

	private void spectatorNotifyRotation(){
		Collection<Spectator> specs = getSpectators();
		DEBUG_LOG(String.format("NOTIFICATION(%s): rotation complete", specs.size()));

		if(!getSpectators().isEmpty()){
			for (Spectator spec : specs) {
				spec.onRotationComplete(this);
			}
		}
	}
	/** END SPECTATOR SECTION */
}
