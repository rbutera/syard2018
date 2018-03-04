package uk.ac.bris.cs.scotlandyard.model;


import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView;
import uk.ac.bris.cs.scotlandyard.harness.TestHarness;

import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.bus;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.taxi;
import static uk.ac.bris.cs.scotlandyard.auxiliary.TestGames.underground;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.TicketAbbr.BS;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.TicketAbbr.SC;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.TicketAbbr.TX;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.TicketAbbr.UG;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.TicketAbbr.X2;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.black;
import static uk.ac.bris.cs.scotlandyard.harness.ImmutableScotlandYardView.blue;
import static uk.ac.bris.cs.scotlandyard.harness.PlayerInteractions.player;
import static uk.ac.bris.cs.scotlandyard.harness.Requirement.eq;
import static uk.ac.bris.cs.scotlandyard.harness.Requirement.hasSize;
import static uk.ac.bris.cs.scotlandyard.harness.SpectatorInteractions.spectator;
import static uk.ac.bris.cs.scotlandyard.harness.SpectatorInteractions.startRotate;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;

/**
 * Complete game play outs that asserts at player and spectator events that the expected
 * conditions must hold
 * Each test includes a PlantUML diagram at the end in comments marked by
 * {@code @startuml ... @enduml}, you may visualise this using tools provided in the project
 * description
 */
public class ModelTwoPlayerPlayOutTestSimple extends ParameterisedModelTestBase {

	private TestHarness harness;
	@Before public void initialise() { harness = new TestHarness(); }
	@After public void tearDown() { harness.forceReleaseShutdownLock(); }

	@Test
	public void testGameWhereMrXNeverPickedADoubleMove() {
		PlayerConfiguration black = harness.newPlayer(
				black(78).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5));
		PlayerConfiguration blue = harness.newPlayer(
				blue(117).with(TX, 11, BS, 8, UG, 4, X2, 0, SC, 0));
		List<Boolean> rounds = Arrays.asList(
				false, false, true
				, false, false, false, false, true
				, false, false, false, false, true
				, false, false, false, false, true
				, false, false, false, false, false, true
		                                    );
		ScotlandYardGame game = createGame(rounds, defaultGraph(),
				black, blue);
		Spectator spectator = harness.createSpectator();
		game.registerSpectator(spectator);
		ImmutableScotlandYardView seed = ImmutableScotlandYardView.snapshot(game);
		harness.play(game).startRotationAndAssertTheseInteractionsOccurInOrder(
				player(BLACK).makeMove()
						.givenGameState(eq(seed.players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(117).with(TX, 11, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(78))
						.givenMoves(hasSize(193))
						.willPick(taxi(BLACK, 79)),
				spectator().onRoundStarted()
						.givenGameState(eq(seed.current(BLUE).round(1).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 3, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(117).with(TX, 11, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenRound(eq(1)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.current(BLUE).round(1).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 3, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(117).with(TX, 11, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLACK, 0))),
				player(BLUE).makeMove()
						.givenGameState(eq(seed.current(BLUE).round(1).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 3, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(117).with(TX, 11, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(117))
						.givenMoves(hasSize(4))
						.willPick(taxi(BLUE, 116)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.round(1).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(116).with(TX, 10, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLUE, 116))),
				spectator().onRotationComplete()
						.givenGameState(eq(seed.round(1).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(116).with(TX, 10, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.respondWith(startRotate(game)),
				player(BLACK).makeMove()
						.givenGameState(eq(seed.round(1).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(116).with(TX, 10, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(79))
						.givenMoves(hasSize(277))
						.willPick(bus(BLACK, 63)),
				spectator().onRoundStarted()
						.givenGameState(eq(seed.current(BLUE).round(2).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 2, UG, 3, X2, 2, SC, 5),
								blue(116).with(TX, 10, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenRound(eq(2)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.current(BLUE).round(2).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 2, UG, 3, X2, 2, SC, 5),
								blue(116).with(TX, 10, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(bus(BLACK, 0))),
				player(BLUE).makeMove()
						.givenGameState(eq(seed.current(BLUE).round(2).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 2, UG, 3, X2, 2, SC, 5),
								blue(116).with(TX, 10, BS, 8, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(116))
						.givenMoves(hasSize(8))
						.willPick(bus(BLUE, 86)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.round(2).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(86).with(TX, 10, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(bus(BLUE, 86))),
				spectator().onRotationComplete()
						.givenGameState(eq(seed.round(2).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(86).with(TX, 10, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.respondWith(startRotate(game)),
				player(BLACK).makeMove()
						.givenGameState(eq(seed.round(2).players(
								//<editor-fold defaultstate="collapsed">
								black(0).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(86).with(TX, 10, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(63))
						.givenMoves(hasSize(197))
						.willPick(taxi(BLACK, 79)),
				spectator().onRoundStarted()
						.givenGameState(eq(seed.current(BLUE).round(3).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 3, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(86).with(TX, 10, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenRound(eq(3)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.current(BLUE).round(3).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 3, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(86).with(TX, 10, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLACK, 79))),
				player(BLUE).makeMove()
						.givenGameState(eq(seed.current(BLUE).round(3).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 3, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(86).with(TX, 10, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(86))
						.givenMoves(hasSize(7))
						.willPick(taxi(BLUE, 69)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.round(3).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(69).with(TX, 9, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLUE, 69))),
				spectator().onRotationComplete()
						.givenGameState(eq(seed.round(3).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(69).with(TX, 9, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.respondWith(startRotate(game)),
				player(BLACK).makeMove()
						.givenGameState(eq(seed.round(3).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 3, X2, 2, SC, 5),
								blue(69).with(TX, 9, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(79))
						.givenMoves(hasSize(277))
						.willPick(underground(BLACK, 67)),
				spectator().onRoundStarted()
						.givenGameState(eq(seed.current(BLUE).round(4).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(69).with(TX, 9, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenRound(eq(4)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.current(BLUE).round(4).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(69).with(TX, 9, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(underground(BLACK, 79))),
				player(BLUE).makeMove()
						.givenGameState(eq(seed.current(BLUE).round(4).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(69).with(TX, 9, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(69))
						.givenMoves(hasSize(4))
						.willPick(taxi(BLUE, 68)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.round(4).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 5, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(68).with(TX, 8, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLUE, 68))),
				spectator().onRotationComplete()
						.givenGameState(eq(seed.round(4).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 5, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(68).with(TX, 8, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.respondWith(startRotate(game)),
				player(BLACK).makeMove()
						.givenGameState(eq(seed.round(4).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 5, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(68).with(TX, 8, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(67))
						.givenMoves(hasSize(346))
						.willPick(taxi(BLACK, 51)),
				spectator().onRoundStarted()
						.givenGameState(eq(seed.current(BLUE).round(5).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(68).with(TX, 8, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenRound(eq(5)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.current(BLUE).round(5).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(68).with(TX, 8, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLACK, 79))),
				player(BLUE).makeMove()
						.givenGameState(eq(seed.current(BLUE).round(5).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 4, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(68).with(TX, 8, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenLocation(eq(68))
						.givenMoves(hasSize(4))
						.willPick(taxi(BLUE, 51)),
				spectator().onMoveMade()
						.givenGameState(eq(seed.round(5).over(true).winning(BLUE).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 5, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(51).with(TX, 7, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenMove(eq(taxi(BLUE, 51))),
				spectator().onGameOver()
						.givenGameState(eq(seed.round(5).over(true).winning(BLUE).players(
								//<editor-fold defaultstate="collapsed">
								black(79).with(TX, 5, BS, 3, UG, 2, X2, 2, SC, 5),
								blue(51).with(TX, 7, BS, 7, UG, 4, X2, 0, SC, 0))
								//</editor-fold>
						                  ))
						.givenWinners(eq(ImmutableSet.of(BLUE))))
				.thenAssertNoFurtherInteractions();
//<editor-fold defaultstate="collapsed">
/*
@startuml

skinparam monochrome true
title Sample game sequence(2 player, 5 rounds)
legend right
Type legend
In package: uk.ac.bris.cs.scotlandyard.model
	S - interface Spectator
	M - class ScotlandYardModel
	V - class ScotlandYardView
	P - interface Player
endlegend

skinparam ParticipantPadding 0
actor UI
participant Spectator <<(S, #BBBBBB)>>
participant ScotlandYardModel <<(M, #BBBBBB) java.util.function.Consumer<Move> >>
box "Players" #LightBlue

	participant MrX <<(P, #BBBBBB)>>
	participant Blue <<(P, #BBBBBB)>>
end box

autonumber

UI -> ScotlandYardModel : startRotate()
group rotation
rnote over ScotlandYardModel: Round=0, CurrentPlayer=MrX
ScotlandYardModel -> MrX: makeMove(V, int, Move[], callback<Move>)
activate MrX
	MrX-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate MrX
rnote over ScotlandYardModel: Round=1, CurrentPlayer=Blue
ScotlandYardModel->Spectator: onRoundStarted(V, 1)
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel -> Blue: makeMove(V, int, Move[], callback<Move>)
activate Blue
	Blue-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate Blue
rnote over ScotlandYardModel: Round=1, CurrentPlayer=MrX
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel->Spectator: onRotationComplete(V)
end
UI -> ScotlandYardModel : startRotate()
group rotation
rnote over ScotlandYardModel: Round=1, CurrentPlayer=MrX
ScotlandYardModel -> MrX: makeMove(V, int, Move[], callback<Move>)
activate MrX
	MrX-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate MrX
rnote over ScotlandYardModel: Round=2, CurrentPlayer=Blue
ScotlandYardModel->Spectator: onRoundStarted(V, 2)
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel -> Blue: makeMove(V, int, Move[], callback<Move>)
activate Blue
	Blue-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate Blue
rnote over ScotlandYardModel: Round=2, CurrentPlayer=MrX
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel->Spectator: onRotationComplete(V)
end
UI -> ScotlandYardModel : startRotate()
group rotation
rnote over ScotlandYardModel: Round=2, CurrentPlayer=MrX
ScotlandYardModel -> MrX: makeMove(V, int, Move[], callback<Move>)
activate MrX
	MrX-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate MrX
rnote over ScotlandYardModel: Round=3, CurrentPlayer=Blue
ScotlandYardModel->Spectator: onRoundStarted(V, 3)
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel -> Blue: makeMove(V, int, Move[], callback<Move>)
activate Blue
	Blue-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate Blue
rnote over ScotlandYardModel: Round=3, CurrentPlayer=MrX
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel->Spectator: onRotationComplete(V)
end
UI -> ScotlandYardModel : startRotate()
group rotation
rnote over ScotlandYardModel: Round=3, CurrentPlayer=MrX
ScotlandYardModel -> MrX: makeMove(V, int, Move[], callback<Move>)
activate MrX
	MrX-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate MrX
rnote over ScotlandYardModel: Round=4, CurrentPlayer=Blue
ScotlandYardModel->Spectator: onRoundStarted(V, 4)
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel -> Blue: makeMove(V, int, Move[], callback<Move>)
activate Blue
	Blue-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate Blue
rnote over ScotlandYardModel: Round=4, CurrentPlayer=MrX
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel->Spectator: onRotationComplete(V)
end
UI -> ScotlandYardModel : startRotate()
group rotation
rnote over ScotlandYardModel: Round=4, CurrentPlayer=MrX
ScotlandYardModel -> MrX: makeMove(V, int, Move[], callback<Move>)
activate MrX
	MrX-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate MrX
rnote over ScotlandYardModel: Round=5, CurrentPlayer=Blue
ScotlandYardModel->Spectator: onRoundStarted(V, 5)
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel -> Blue: makeMove(V, int, Move[], callback<Move>)
activate Blue
	Blue-->ScotlandYardModel : Consumer.accept(Ticket)
deactivate Blue
rnote over ScotlandYardModel: Round=5, CurrentPlayer=MrX
ScotlandYardModel->Spectator: onMoveMade(V, Ticket)
ScotlandYardModel->Spectator: onGameOver(V, [MrX,Blue])
rnote over ScotlandYardModel: Round=5, CurrentPlayer=MrX, GameOver=true
end
@enduml
*/
//</editor-fold>

	}
}