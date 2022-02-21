package ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.cnp.participants;

import ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.cnp.initiators.RoomManagerAgent;
import ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.common.CNPParticipant;
import jade.util.Logger;

/**
 * A Lamp Controller Agent (LAMP) is a JADE agent that controls the blinds in a room.
 * <p>The LAMP publishes one service to the Directory Facilitator (DF):
 * <ul>
 * <li>raise-illuminance: for raising the illuminance of the room by turning on the light.
 * </ul></p>
 * <p>The class extends {@link CNPParticipant} for enabling an agent to behave as a participant
 * in the <a href="http://www.fipa.org/specs/fipa00029/SC00029H.html">FIPA Contract Net
 * Interaction Protocol</a>. The LAMP receives calls for proposals (CFPs) for increasing the
 * illuminance of the room and offers to increase the illuminance by turning on the light. </p>
 * NOTE: This class does not need to be changed for the purpose of this assignment.
 */
public class LampControllerAgent extends CNPParticipant {

  private final static Logger LOGGER = Logger.getJADELogger(RoomManagerAgent.class.getName());

  protected void setup() {

    LOGGER.info("Hello world! Lamp controller agent " + getAID().getName() + " is set up.");

    // Add all provided services
    this.providedServices.add("increase-illuminance");

    // Publish all provided services to DF
    addBehaviour(new PublishServiceBehavior());

    // Search services in the DF for setting the illuminance
    // This service is used to simulate the lamp controller agent's ability to affect the illuminance of the
    // environment (e.g. by turning on the light)
    addBehaviour(new SearchServiceBehavior("set-illuminance"));

    // Offer to increase illuminance by raising the blinds
    addBehaviour(new OfferProposalsServer("increase-illuminance", "turn-on-light"));

    // Satisfy offers by turning on the light. For satisfying the offer turn-on-light, a behavior RequestSetIlluminance
    // is triggered to simulate the effects of turning on the light.
    addBehaviour(new SatisfyOffersServer("turn-on-light", new RequestSetIlluminance("high")));
  }
}
