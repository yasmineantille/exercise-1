package ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.environment;

import ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.common.BaseAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A Building Environment Agent (ENV) is a JADE agent that simulates a building environment.
 * <p>The ENV publishes three services to the Directory Facilitator (DF):
 * <ul>
 * <li>read-weather: for reading the outdoors weather, i.e. sunny or cloudy
 * <li>read-illuminance: for reading the illuminance of the room, i.e. high or low
 * <li>set-illuminance: for setting the illuminance of the room, i.e. high or low
 * </ul><p>
 * NOTE: This class does not need to be changed for the purpose of this assignment.
 */
public class BuildingEnvironmentAgent extends BaseAgent {

  private final static Logger LOGGER = Logger.getJADELogger(BuildingEnvironmentAgent.class.getName());
  private final Map<String, List<AID>> subscribers = new HashMap<>();
  private final ReadWriteLock subscribersLock = new ReentrantReadWriteLock();
  private BuildingEnvironmentGUI environmentGUI;
  private String illuminance = "high";
  private String weather = "sunny";

  protected void setup() {

    LOGGER.info("Hello world! Building environment agent " + getLocalName() + " is set up.");

    this.environmentGUI = new BuildingEnvironmentGUI(this);
    this.environmentGUI.showGui();

    // Add all provided services
    this.providedServices.add("read-illuminance");
    this.providedServices.add("read-weather");
    this.providedServices.add("set-illuminance");

    // Publish all provided services to DF
    addBehaviour(new PublishServiceBehavior());

    // Receive subscription requests for reading illuminance and weather
    addBehaviour(new SubscriptionServer());

    // Receive requests for setting illuminance to low or high
    addBehaviour(new SetIlluminanceServer());

    // TODO Add a behavior, such that the agent periodically prints the illuminance and the weather on the environment (Task 1)
    // HINT: Use the method addBehaviour(). As input, provide an instance of the Behaviour class
    // that you implemented below
    addBehaviour(new PrintIlluminanceServer(this, 1000));
  }

  // TODO Implement a Behaviour for periodically printing the illuminance and the weather on the environment (Task 1)
  // HINT 1: Implement an inner class that extends TickerBehavior
  // (see https://jade.tilab.com/doc/api/jade/core/behaviours/TickerBehaviour.html).
  // HINT 2: Use the instance attributes illuminance and weather to print the conditions of the environment
  // HINT 3: The inner class NotificationServer (see line 158) also extends TickerBehavior

  public String getIlluminance() {
    return illuminance;
  }

  public void setIlluminance(String illuminance) {
    this.illuminance = illuminance;
  }

  public String getWeather() {
    return weather;
  }

  public void setWeather(String weather) {
    this.weather = weather;
  }

  /**
   * Task 1
   */
  private class PrintIlluminanceServer extends TickerBehaviour {

    public PrintIlluminanceServer(Agent a, long period) {
      super(a, period);
    }

    @Override
    protected void onTick() {
      String illuminance = getIlluminance();
      String weather = getWeather();

      LOGGER.info("ILLUMINANCE: " + illuminance);
      LOGGER.info("WEATHER: " + weather);
    }
  }

  /**
   * <p>A SubscriptionServer is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00035/SC00035H.html">FIPA Subscribe Interaction Protocol</a>
   * for the role of the Participant.</p>
   * <p>The behavior enables the agent to continuously receive subscription requests to illuminance or
   * weather notifications.</p>
   * <p>In case of successful subscription, the {@link NotificationServer} behavior is triggered.</p>
   * <p>The class extends {@link CyclicBehaviour}, i.e. the behavior is executed continuously.</p>
   */
  private class SubscriptionServer extends CyclicBehaviour {

    private void addSubscriber(AID subscriber, String notificationType) {

      boolean startNotificationServer = false;

      subscribersLock.writeLock().lock();
      if (!subscribers.containsKey(notificationType)) {
        subscribers.put(notificationType, new ArrayList<AID>());
        startNotificationServer = true;
      }
      if (!subscribers.get(notificationType).contains(subscriber)) {
        subscribers.get(notificationType).add(subscriber);
      }
      subscribersLock.writeLock().unlock();

      if (startNotificationServer) {
        addBehaviour(new NotificationServer(notificationType, myAgent, 2000));
      }
    }

    public void action() {
      // The agent continuously accepts SUBSCRIBE messages
      MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
      ACLMessage msg = myAgent.receive(msgTemplate);

      if (msg != null) {
        // Subscribe message received
        AID sender = msg.getSender();
        String serviceType = msg.getContent();
        ACLMessage reply = msg.createReply();

        // The agent AGREES only if subscription is for reading illuminance or reading the weather
        if ("read-illuminance".equals(serviceType) || "read-weather".equals(serviceType)) {
          reply.setPerformative(ACLMessage.AGREE);
          reply.setContent(serviceType);
          LOGGER.info("AGREE on " + serviceType);

          // Add subscriber to the subscriber
          addSubscriber(sender, serviceType);
        } else {
          // The agent REFUSES subscription for other service types
          reply.setPerformative(ACLMessage.REFUSE);
          reply.setContent(serviceType);
          LOGGER.info("REFUSE " + serviceType);
        }
        // Send the reply
        myAgent.send(reply);
      } else {
        // Block the behavior until a new message that matches the template is received
        block();
      }
    }
  }  // End of inner class OfferSubscriptionServer

  /**
   * <p>A NotificationServer is a JADE behavior that implements part of the
   * <a href="http://www.fipa.org/specs/fipa00035/SC00035H.html">FIPA Subscribe Interaction Protocol</a>
   * for the role of the Participant.</p>
   * <p>The behavior enables the agent to periodically send notifications about illuminance or weather.</p>
   * <p>This behavior is triggered upon successful subscription on the {@link SubscriptionServer}.</p>
   * <p>The class extends {@link TickerBehaviour}, i.e. the behavior is executed periodically.</p>
   */
  private class NotificationServer extends TickerBehaviour {

    private final String notificationType;

    public NotificationServer(String notificationType, Agent a, long period) {
      super(a, period);
      this.notificationType = notificationType;
    }

    @Override
    protected void onTick() {
      ACLMessage msg;
      // The agent INFORMS the subscribers about their topic of preference, i.e. illuminance or weather
      if ("read-illuminance".equals(notificationType) || "read-weather".equals(notificationType)) {
        msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("read-illuminance".equals(notificationType) ? illuminance : weather);
        LOGGER.info("INFORM " + notificationType + ": " + msg.getContent());
      } else {
        // The agent FAILS to notify subscribers for unknown topics
        msg = new ACLMessage(ACLMessage.FAILURE);
        msg.setContent(notificationType);
        LOGGER.info("FAIL " + notificationType);
      }

      // Read subscribers
      subscribersLock.readLock().lock();
      for (AID subscriber : subscribers.get(notificationType)) {
        msg.addReceiver(subscriber);
      }
      subscribersLock.readLock().unlock();

      // Set additional message meta-data, that are used to identify the incoming messages
      // of the conversation
      msg.setConversationId("subscribe-" + notificationType);

      // Send the message
      myAgent.send(msg);
    }
  } // End of inner class NotificationServer

  /**
   * <p>An SetIlluminanceServer is a JADE behavior that implements the
   * <a href="http://www.fipa.org/specs/fipa00026/SC00026H.html">FIPA Request Interaction Protocol</a>
   * for the role of the Participant.</p>
   * <p>The bahavior enables the agent to continuously receive requests to set the illuminance to "low" or
   * "high".</p>
   * <p>The behavior is used to simulate changes on the illuminance of the room, e.g. when a lamp is turned
   * on or blinds are raised.</p>
   * <p>The class extends {@link CyclicBehaviour}, i.e. the behavior is executed continuously.</p>
   */
  private class SetIlluminanceServer extends CyclicBehaviour {

    public void action() {
      // The agent continuously accepts REQUEST messages
      MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
      ACLMessage msg = myAgent.receive(mt);

      if (msg != null) {
        // Request message received
        String illuminanceValue = msg.getContent();
        ACLMessage reply = msg.createReply();

        // The agent INFORMS that the request is satisfied only if it is for setting illuminance to high or low
        if ("low".equals(illuminanceValue) || "high".equals(illuminanceValue)) {
          reply.setPerformative(ACLMessage.INFORM);
          reply.setContent("inform-done");
          illuminance = illuminanceValue;
          LOGGER.info("INFORM done set-illuminance " + illuminanceValue);
        } else {
          // The agent FAILS to satisfy the request if it is not for setting illuminance to high or low
          reply.setPerformative(ACLMessage.FAILURE);
          reply.setContent("set-illuminance");
          LOGGER.info("REFUSE set-illuminance " + illuminanceValue);
        }

        // Send the reply
        myAgent.send(reply);
      } else {
        // Block the behavior until a new message that matches the template is received
        block();
      }
    }
  }  // End of inner class SetIlluminanceServer
}
