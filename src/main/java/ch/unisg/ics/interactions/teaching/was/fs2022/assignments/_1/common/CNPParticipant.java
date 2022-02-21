package ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.common;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.Iterator;

/**
 * <p>A CNPParticipant is an abstract JADE agent that specifies two behavior classes for
 * implementing the <a href="http://www.fipa.org/specs/fipa00029/SC00029H.html">FIPA Contract Net
 * Interaction Protocol</a> for the role of the Participant:</p>
 * <ul>
 * <li>{@link OfferProposalsServer}: respond to call for proposals (CFPs)
 * <li>{@link SatisfyOffersServer}: satisfy offers if the agent's proposal is accepted
 * </ul></p>
 * The class also includes a {@link RequestSetIlluminance} behavior for simulating the effects of
 * satisfying an offer in the context of the Room Management use case.
 * <p>NOTE: This class does not need to be changed for the purpose of this assignment.</p>
 */
public abstract class CNPParticipant extends BaseAgent {

  private final static Logger LOGGER = Logger.getJADELogger(CNPParticipant.class.getName());

  /**
   * <p>An OfferProposalsServer is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00029/SC00029H.html">FIPA Contract Net Interaction Protocol</a>
   * for the role of the Participant.</p>
   * <p>The behavior enables the agent to continuously receive calls for proposals (CFPs) to execute a task,
   * i.g. to increase the illuminance by raising the blinds or by turning on the light.</p>
   * <p>The class extends {@link CyclicBehaviour}, i.e. the behavior is executed continuously.</p>
   */
  protected class OfferProposalsServer extends CyclicBehaviour {

    // The task for which a proposal is offered
    private final String serviceType;

    // The offer that is proposed
    private final String offer;

    public OfferProposalsServer(String serviceType, String offer) {
      this.serviceType = serviceType;
      this.offer = offer;
    }

    public void action() {
      // The agent continuously accepts CFP (call for proposals) messages
      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
      ACLMessage msg = myAgent.receive(mt);

      if (msg != null) {
        // CFP message received
        String serviceType = msg.getContent();
        ACLMessage reply = msg.createReply();

        // The agent PROPOSES only if the call is for its serviceType
        if (this.serviceType.equals(serviceType)) {
          reply.setPerformative(ACLMessage.PROPOSE);
          reply.setContent(offer);
          LOGGER.info("PROPOSE " + serviceType + " with " + offer);
        } else {
          reply.setPerformative(ACLMessage.REFUSE);
          reply.setContent("not-available");
          LOGGER.info("REFUSE " + serviceType);
        }

        // Send the message
        myAgent.send(reply);
      } else {
        // Block the behavior until a new message that matches the template is received
        block();
      }
    }
  }  // End of inner class OfferProposalsServer


  /**
   * <p>A SatisfyOffersServer is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00029/SC00029H.html">FIPA Contract Net Interaction Protocol</a>
   * for the role of the Participant.</p>
   * <p>The behavior enables the agent to continuously receive acceptance messages to satisfy offers
   * (e.g. to control the blinds or the light), and then behave to satisfy the offer.</p>
   * <p>The class extends {@link CyclicBehaviour}, i.e. the behavior is executed continuously.</p>
   */
  protected class SatisfyOffersServer extends CyclicBehaviour {

    // The offer that will be satisfied
    private final String offer;

    // The effect of satisfying the offer, i.e. the behavior that is needed to satisfy the offer
    private final Behaviour effect;

    public SatisfyOffersServer(String offer, Behaviour effect) {
      this.offer = offer;
      this.effect = effect;
    }

    public void action() {
      // The agent continuously accepts ACCEPT PROPOSAL messages
      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
      ACLMessage msg = myAgent.receive(mt);
      if (msg != null) {
        // Accept Proposal message received
        String offer = msg.getContent();
        ACLMessage reply = msg.createReply();

        // The agent INFORMS that the offer is realized only if it successfully satisfies the offer
        if (this.offer.equals(offer)) {

          // Behaves to satisfy the offer
          // E.g. this behavior is used to simulate how the environment is affected by controlling the blinds.
          // E.g. if the blinds are raised (offer), the agent offering the service set-illuminance is contacted to
          // set illuminance to high.
          myAgent.addBehaviour(effect);

          reply.setPerformative(ACLMessage.INFORM);
          reply.setContent("inform-done");
          LOGGER.info("INFORM " + offer + " done");
        } else {
          reply.setPerformative(ACLMessage.FAILURE);
          reply.setContent("not-available");
          LOGGER.info("FAIL " + offer + " not available");
        }

        // Send the message
        myAgent.send(reply);
      } else {
        block();
      }
    }
  } // End of inner class SatisfyOffersServer

  /**
   * <p>A RequestSetIlluminance is a JADE behavior that implements part of the
   * <a href="hhttp://www.fipa.org/specs/fipa00026/XC00026F.html">FIPA Request Interaction Protocol</a>
   * for the role of the Initiator.</p>
   * <p>The behavior enables the agent to simulate how controlling the blinds or the light changes
   * the illuminance of the room, e.g. to high or low.</p>
   * <p>The class extends {@link OneShotBehaviour}, i.e. the behavior is executed only once.</p>
   */
  protected class RequestSetIlluminance extends OneShotBehaviour {

    // The target value of illuminance
    private final String illuminanceValue;

    // The service type is used to identify the agents that will be contacted
    private final String serviceType = "set-illuminance";

    // The agent that provides the service of serviceType. In this Request protocol,
    // at most one participant is required.
    private AID participant;

    // The template to receive messages
    // The template is used to filter incoming messages e.g. based on the conversation id,
    // or the performative
    private MessageTemplate msgTemplate;

    // The step that indicates the phase of the protocol
    // Step 0: send REQUEST messages
    // Step 1: receive INFORM or REFUSE messages
    // Step 2: terminate
    private int step = 0;

    public RequestSetIlluminance(String illuminanceValue) {
      this.illuminanceValue = illuminanceValue;
    }

    public void action() {
      switch (step) {
        case 0:
          LOGGER.info("Initiating Request protocol");
          // Terminate behavior if there is not at most one agent providing the service read-illuminance
          if (!serviceProviders.containsKey(serviceType) || serviceProviders.get(serviceType).size() > 1) {
            LOGGER.info("No appropriate service provider found");
            // Update protocol phase to terminate the protocol
            step = 2;
          } else {
            // The agent REQUESTS the environment agent to set the illuminance
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

            // Add the receiver, i.e. the participant
            Iterator<AID> environmentAgents = serviceProviders.get(serviceType).iterator();
            AID environment = environmentAgents.next();
            msg.addReceiver(environment);

            // Set the content, i.e. the serviceType
            msg.setContent(illuminanceValue);

            // Set additional message meta-data, that are used to identify the incoming messages
            // of the conversation
            msg.setConversationId("request-" + serviceType);
            msg.setReplyWith("request-" + System.currentTimeMillis()); // Unique value

            // Send the message
            myAgent.send(msg);
            LOGGER.info("REQUEST " + serviceType + " " + illuminanceValue);

            // Prepare the template to get responses to the request
            msgTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("request-" + serviceType),
                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

            // Update protocol phase and break
            step = 1;
          }
          break;
        case 1:
          // The agent continuously accepts messages from the agents that were contacted on step 0
          ACLMessage msg = myAgent.receive(msgTemplate);

          if (msg != null) {
            // Message received
            // Update protocol phase
            step = 2;
          } else {
            // Block the behavior until a new message that matches the template is received
            block();
          }
          break;
        case 2:
          // The behavior terminates if the protocol reached phase 2
          LOGGER.info("Request protocol terminated");
      }
    }
  } // End of inner class RequestSetIlluminance
}
