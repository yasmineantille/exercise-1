package ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.cnp.initiators;

import ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.common.CNPInitiator;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static jade.lang.acl.MessageTemplate.MatchPerformative;

/**
 * <p> A Room Manager Agent (MNG) is a JADE agent that manages a room and strives to maintain
 * appropriate room conditions for the room's occupants, e.g. to preserve a high illuminance
 * in the room (when the room is occupied). </p>
 *
 * <p> The MNG implements part of the
 * <a href="http://www.fipa.org/specs/fipa00035/SC00035H.html">FIPA Subscribe Interaction Protocol</a>
 * for the role of the Initiator: The MNG subscribes to an agent that offers the read-illuminance
 * and read-weather services, so as to receive notifications about the illuminance and the weather
 * (i.e. if the illuminance is "low" or "high", and if the weather is "sunny" or "cloudy").
 * (See {@link PerceiveEnvironment}, {@link HandleIlluminancePercept}, {@link HandleWeatherPercept}).</p>
 *
 * <p>The class extends {@link CNPInitiator} for enabling an agent to behave as an initiator
 * in the <a href="http://www.fipa.org/specs/fipa00029/SC00029H.html">FIPA Contract Net
 * Interaction Protocol</a>. If the MNG perceives that the illuminance is low, it sends calls for proposals
 * (CFPs) to agents that offer the raise-illuminance service.
 * (See {@link PerformContractNetProtocol}).</p>
 * <p> NOTE: This is the only class that needs to be changed for implementing Task 2 of this assignment.
 * You need to implement parts of the {@link PerformContractNetProtocol} to enable the MNG to successfully
 * perform the Contract Net Protocol.</p>
 */
public class RoomManagerAgent extends CNPInitiator {

  private final static Logger LOGGER = Logger.getJADELogger(RoomManagerAgent.class.getName());

  private String perceivedIlluminance = "";
  private String perceivedWeather = "";

  protected void setup() {

    LOGGER.info("Hello world! Room manager agent " + getLocalName() + " is set up.");

    addBehaviour(
            new WakerBehaviour(this, 5000) {
              public void onWake() {
                addBehaviour(new PerceiveEnvironment("read-illuminance", new HandleIlluminancePercept()));
                addBehaviour(new PerceiveEnvironment("read-weather", new HandleWeatherPercept()));
              }
            });

    // Search services in the DF for reading the illuminance
    // This service is used to simulate the manager agent's ability to perceive the illuminance of the environment
    // (e.g. through sensors)
    addBehaviour(new SearchServiceBehavior("read-illuminance"));

    // Search services in the DF for reading the illuminance
    // This service is used to simulate the manager agent's ability to perceive the weather conditions
    // (e.g. by using a third-party weather service)
    addBehaviour(new SearchServiceBehavior("read-weather"));

    // Search services in the DF for increasing the illuminance (e.g. in case the illuminance is low)
    addBehaviour(new SearchServiceBehavior("increase-illuminance"));
  }

  /**
   * <p>A PerformContractNetProtocol is a JADE behavior that implements the
   * <a href="http://www.fipa.org/specs/fipa00029/SC00029H.html">FIPA Contract Net
   * Interaction Protocol</a>
   * for the role of the Initiator.</p>
   * <p>The behavior enables the agent to:
   * <ul>
   * <li> Step = 0:
   * Send call for proposals (CFPs) to participants offering a relevant serviceType
   * (e.g. serviceType is increase-illuminance) (Task 2.1)
   * <li> Step = 1:
   * Receive proposals with the offers of the participants (e.g. raise-blinds or turn-on-light) (Task 2.2)
   * <li> Step = 2:
   * Accept the proposal of the participant with the best offer (e.g. if the weather is cloudy
   * the best offer is to turn-on-light. If the weather is sunny, the best offer is to raise-blinds) (Task 2.3)
   * <li> Step = 3:
   * Accept information about the progress of the offer (e.g. "inform-done" or "not-available")
   * by the the participant with the best offer (Task 2.4)
   * </ul></p>
   * <p>This behavior is triggered when the MNG perceives that conditions of the environment need to
   * change, e.g. when the illuminance is perceived to be low. See also line 431 on
   * {@link HandleIlluminancePercept}.</p>
   * <p>The class extends the generic {@link Behaviour}. </p>
   */
  protected class PerformContractNetProtocol extends Behaviour {
    // The service type is used to identify the agents that will be contacted
    private final String serviceType;

    // The agent that provides the service of serviceType. In this Contract Net protocol,
    // at least one participant is required.
    private Set<AID> participants = new HashSet<>();

    // The agent who provides the best offer
    private AID bestParticipant;

    // The best offer
    private String bestOffer;

    // The counter of replies from seller agents
    private int repliesCounter = 0;

    // The template to receive messages
    // The template is used to filter incoming messages e.g. based on the conversation id,
    // or the performative
    private MessageTemplate msgTemplate;

    // The step that indicates the phase of the protocol
    // Step 0: send CFP (call for proposals) messages with serviceType
    // Step 1: receive PROPOSE messages with offer
    // Step 2: send ACCEPT PROPOSAL messages to the bestParticipant with offer
    // Step 3: receive INFORM or FAILURE messages
    // Step 4: terminate
    private int step = 0;

    PerformContractNetProtocol(String serviceType) {
      this.serviceType = serviceType;
      if (serviceProviders.containsKey(serviceType)) {
        this.participants = serviceProviders.get(serviceType);
      }
    }

    public void action() {
      switch (step) {
        case 0: // TODO: Implement the case to send CFP (call for proposals) messages
          // Initiating Contract Net protocol
          LOGGER.info("Initiating Contract Net protocol");

          // 1) Terminate behavior if there is not at least one agent providing the service of serviceType
          // HINT: If the participants set is empty, update the protocol phase step=4 and break.
          if(participants.isEmpty()) {
            step = 4;
            break;
          }

          // The agent CALLS FOR PROPOSALS to service providers to increase illuminance
          // 2) Create an ACL Message with the appropriate performative
          ACLMessage msg = new ACLMessage(ACLMessage.CFP);

          // 3) Add all receivers of the participants set
          for (AID receiver: participants) {
            msg.addReceiver(receiver);
          }

          // 4) Set the content, i.e. the serviceType
          msg.setContent(serviceType);

          // 5) Set additional message meta-data, that are used to identify the incoming messages
          // of the conversation
          msg.setConversationId("cfp-" + serviceType);
          msg.setReplyWith("cfp-" + System.currentTimeMillis());

          LOGGER.info("TESTING TESTING TESTING TESTING");

          // 6) Send the message
          myAgent.send(msg);

          LOGGER.info("CFP " + serviceType);

          // 7) Prepare the template to get proposals within this conversation
          msgTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("cfp-" + serviceType),
                  MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

          // 8) Update protocol phase and break
          step = 1;
          break;
          // block(); // remove this line after implementing case 0
        case 1: // TODO Implement the case to handle incoming proposals of this conversation

          // The agent continuously accepts messages (with any performative)
          // from the agents that were contacted on step 0

          // 1) Accept messages from the agents that were contacted on step 0
          // HINT: Update the following line to use myAgent.receive(msgTemplate), where the msgTemplate
          // is the template that you prepared on step 7) of case:0.
          msg = myAgent.receive(msgTemplate);

          if (msg != null) {
            // Message received
            // If the sender PROPOSES an offer:
            // 2.1) extract the sender and the offer
            // 2.2) if there is no former proposal, set the sender and the offer as bestParticipant and bestOffer
            // 2.3) if there is former proposal, check if the offer is good and, if needed, update the bestParticipant
            //      and bestOffer.
            //      HINT: Use the method isGoodOffer() to determine whether an offer is best
            if(msg.getPerformative() == ACLMessage.PROPOSE) {
              AID sender = msg.getSender();
              String offer = msg.getContent();
              if(bestOffer == null) {
                bestOffer = offer;
                bestParticipant = sender;
              } else {
                if(isGoodOffer(offer) && !isGoodOffer(bestOffer)) {
                  bestOffer = offer;
                  bestParticipant = sender;
                }
              }
            }

            // 3) Update the number of messages received in this conversation (not only PROPOSE messages)
            // HINT: Increase the value of repliesCounter
            repliesCounter += 1;

            // 4) If messages were received by all participants, update the protocol phase
            // HINT: Compare the repliesCounter with the size of the participants set
            if (repliesCounter == participants.size()) step = 2;
          } else {
            // Block the behavior until a new message that matches the template is received
            block();
          }
          break;
        case 2: // TODO: Implement the case to send ACCEPT PROPOSAL messages
          // The agent ACCEPTS the PROPOSAL of the bestParticipant
          // 1) Create an ACL Message with the appropriate performative
          ACLMessage acceptProposalMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

          // 2) Add the receiver, i.e. the bestParticipant
          acceptProposalMsg.addReceiver(bestParticipant);

          // 3) Set the content, i.e. the bestOffer
          acceptProposalMsg.setContent(bestOffer);

          // 4) Set additional message meta-data, that are used to identify the incoming messages
          // of the conversation
          acceptProposalMsg.setConversationId("acceptProposal-" + serviceType + "-with-" + bestOffer);
          acceptProposalMsg.setReplyWith("acceptProposal-" + System.currentTimeMillis());

          // 5) Send the message
          myAgent.send(acceptProposalMsg);

          LOGGER.info("ACCEPT PROPOSAL " + serviceType + " with " + bestOffer);

          // 7) Prepare the template to receive information about the progress of the service within
          // this conversation
          msgTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("acceptProposal-" + serviceType + "-with-" + bestOffer),
                  MessageTemplate.MatchInReplyTo(acceptProposalMsg.getReplyWith()));

          // 8) Update protocol phase and break
          step = 3;
          break;
          // block(); // remove this line after implementing case 2
        case 3: // TODO: Implement the case to handle incoming information about the progress of the service within this conversation

          // 1) Accept messages from the agents that were contacted on step 2
          // HINT: Update the following line to use myAgent.receive(msgTemplate), where the msgTemplate
          // is the template that you prepared on step 7) of case:2.
          msg = myAgent.receive(msgTemplate);

          if (msg != null) {
            // Message received
            // If the sender INFORMS about the progress of the service:
            // 2.1) extract the sender and the serviceType
            // 2.2) print the serviceType, the sender, the bestOffer, and that the service was successfully completed.
            if (msg.getPerformative() == ACLMessage.INFORM) {
              AID sender = msg.getSender();
              String serviceType = msg.getContent();
              LOGGER.info("Progress of service " + serviceType + " informed by " + sender + " with offer " + bestOffer + ". Successfully completed.");
            } else {
              LOGGER.info("Service " + serviceType + " not successfully completed.");
            }
            // If the sender does not INFORM:
            // 3) print the serviceType, and that the service was not successfully completed.

            // LOGGER.info("Remove me to report about success of failure of executing" + serviceType);

            // 4) Update protocol phase
            step = 4;
          } else {
            // Block the behavior until a new message that matches the template is received
            block();
          }
          break;
      }
    }

    // The behavior terminates if the protocol reached phase 4 or if no participant proposed an offer
    @Override
    public boolean done() {

      if (step == 2 && bestParticipant == null) {
        LOGGER.info("Contract net protocol terminated because no agent proposed an offer");
        return true;
      }
      if (step == 4) {
        LOGGER.info("Contract net protocol terminated");
        return true;
      }
      return false;
    }

    private boolean isGoodOffer(String offer) {
      return (("cloudy".equals(perceivedWeather) && "turn-on-light".equals(offer))
              || ("sunny".equals(perceivedWeather) && "raise-blinds".equals(offer)));
    }
  }

  /**
   * <p>A PerceiveEnvironment is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00035/SC00035H.html">FIPA Subscribe Interaction Protocol</a>
   * for the role of the Initiator.</p>
   * <p>The behavior enables the agent to simulate how it becomes able to perceive the conditions of the
   * environment: The agent subscribes to an agent that offers a relevant service (e.g. serviceType is
   * read-illuminance or read-weather). Upon successful subscription, the agent behaves to handle the
   * notifications (the percepts) (e.g. by triggering the {@link HandleIlluminancePercept} or the
   * {@link HandleWeatherPercept} behavior).</p>
   * <p>The class extends the generic {@link Behaviour}. </p>
   */
  private class PerceiveEnvironment extends Behaviour {
    // The service type is used to identify the agents that will be contacted
    private final String serviceType;

    // The behavior that is triggered when the agent perceives the environment
    // (e.g. the illuminance or the weather)
    private final Behaviour perceptHandler;

    // The agent that provides the service of serviceType. In this Request protocol,
    // at most one participant is required.
    private AID participant;

    // The template to receive messages
    // The template is used to filter incoming messages e.g. based on the conversation id,
    // or the performative
    private MessageTemplate msgTemplate;

    // The step that indicates the phase of the protocol
    // Step 0: send SUBSCRIBE messages
    // Step 1: receive AGREE or REFUSE messages
    // Step 2: terminate
    private int step = 0;

    public PerceiveEnvironment(String serviceType, Behaviour perceptHandler) {
      this.serviceType = serviceType;
      this.perceptHandler = perceptHandler;
    }

    @Override
    public void action() {

      switch (step) {
        case 0:
          LOGGER.info("Initiating Subscribe protocol");
          // Terminate behavior if there is not at most one agent providing the service read-illuminance
          if (!serviceProviders.containsKey(serviceType) || serviceProviders.get(serviceType).size() > 1) {
            LOGGER.info("No appropriate service provider found");
            // Update protocol phase to terminate the protocol
            step = 2;
            break;
          } else {
            // The agent SUBSCRIBES to the environment agent to receive notifications about illuminance
            ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);

            // Add the receiver, i.e. the participant
            Iterator<AID> environmentAgents = serviceProviders.get(serviceType).iterator();
            AID environment = environmentAgents.next();
            msg.addReceiver(environment);

            // Set the content, i.e. the serviceType
            msg.setContent(serviceType);

            // Set additional message meta-data, that are used to identify the incoming messages
            // of the conversation
            msg.setConversationId("subscribe-" + serviceType);
            msg.setReplyWith("subscribe-" + System.currentTimeMillis()); // Unique value

            // Send the message
            myAgent.send(msg);
            LOGGER.info("SUBSCRIBE " + serviceType);

            // Prepare the template to get responses to the request
            msgTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("subscribe-" + serviceType),
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
            // If it is AGREED that the agent can perceive the environment (e.g. the illuminance or the weather),
            // the agent handles the notifications.
            if (msg.getPerformative() == ACLMessage.AGREE) {
              addBehaviour(perceptHandler);
            }
            // Update protocol phase
            step = 2;
          } else {
            // Block the behavior until a new message that matches the template is received
            block();
          }
          break;
      }
    }

    // The behavior terminates if the protocol reached phase 2
    @Override
    public boolean done() {
      if (step == 2) {
        LOGGER.info("Subscribe protocol terminated");
        return true;
      }
      return false;
    }
  }

  /**
   * <p>A HandleIlluminancePercept is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00035/SC00035H.html">FIPA Subscribe Interaction Protocol</a>
   * for the role of the Initiator.</p>
   * <p>The behavior enables the agent to simulate how it perceives the illuminance of the
   * room and handles the illuminance percept: The agent receives notifications by an agent offering
   * the read-illuminance service. Upon reception, if the illuminance is low, the agent triggers the
   * behavior {@link PerformContractNetProtocol}, so as to send calls for proposal (CFPs) for raising
   * the illuminance.</p>
   * <p>This behavior is triggered upon successful subscription on {@link PerceiveEnvironment}
   * for the notificationType read-illuminance.</p>
   * <p>The class extends {@link CyclicBehaviour}, i.e. the behavior is executed continuously.</p>
   */
  private class HandleIlluminancePercept extends CyclicBehaviour {

    @Override
    public void action() {
      // The agent continuously accepts INFORM messages in this conversation
      MessageTemplate msgTemplate = MessageTemplate.and(MatchPerformative(ACLMessage.INFORM),
              MessageTemplate.MatchConversationId("subscribe-read-illuminance"));
      ACLMessage msg = myAgent.receive(msgTemplate);
      if (msg != null) {
        // Request message received
        String illuminanceValue = msg.getContent();

        // If it is the first time that the illuminance is perceived as low,
        // the agent requests from other agents to increase the illuminance
        if ("low".equals(illuminanceValue) && !perceivedIlluminance.equals(illuminanceValue)) {
          addBehaviour(new PerformContractNetProtocol("increase-illuminance"));
        }

        // Update the value of perceived illuminanceValue
        perceivedIlluminance = illuminanceValue;
        LOGGER.info("Perceived illuminance: " + perceivedIlluminance);
      } else {
        // Block the behavior until a new message that matches the template is received
        block();
      }
    }
  }

  /**
   * <p>A HandleWeatherPercept is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00035/SC00035H.html">FIPA Subscribe Interaction Protocol</a>
   * for the role of the Initiator.</p>
   * <p>The behavior enables the agent to simulate how it perceives the weather:
   * The agent receives notifications by an agent offering the read-weather service.
   * <p>This behavior is triggered upon successful subscription on {@link PerceiveEnvironment}
   * for the notificationType read-weather.</p>
   * <p>The class extends {@link CyclicBehaviour}, i.e. the behavior is executed continuously.</p>
   */
  private class HandleWeatherPercept extends CyclicBehaviour {

    @Override
    public void action() {
      // The agent continuously accepts INFORM messages in this conversation
      MessageTemplate msgTemplate = MessageTemplate.and(MatchPerformative(ACLMessage.INFORM),
              MessageTemplate.MatchConversationId("subscribe-read-weather"));
      ACLMessage msg = myAgent.receive(msgTemplate);
      if (msg != null) {
        // Request message received
        String weatherValue = msg.getContent();

        // Update the value of perceived weather
        perceivedWeather = weatherValue;
        LOGGER.info("Perceived weather: " + perceivedWeather);
      } else {
        // Block the behavior until a new message that matches the template is received
        block();
      }
    }
  }

}
