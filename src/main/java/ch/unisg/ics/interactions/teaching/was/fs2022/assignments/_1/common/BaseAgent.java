package ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.common;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>A BaseAgent is an abstract JADE agent that specifies two behavior classes for
 * interacting with the Directory Facilitator (DF):
 * <ul>
 * <li>{@link PublishServiceBehavior}: publish services to the DF
 * <li>{@link SearchServiceBehavior}: search for a service in the DF
 * </ul></p>
 * <p>NOTE: This class does not need to be changed for the purpose of this assignment.</p>
 */
public abstract class BaseAgent extends Agent {

  // The set of the agent's provided services that are published to the DF
  protected Set<String> providedServices = new HashSet<>();

  // The map of agents that provide the services for which the agent searched in the DF
  // The key is the type of the discovered service
  protected Map<String, Set<AID>> serviceProviders = new HashMap<>();

  /**
   * <p>A PublishServiceBehavior is a JADE behavior for publishing a service to the Directory Facilitator (DF).</p>
   * <p>The class extends {@link OneShotBehaviour}, i.e. the behavior is executed only once.</p>
   */
  public class PublishServiceBehavior extends OneShotBehaviour {

    @Override
    public void action() {
      // Create the agent description that is published in the DF
      DFAgentDescription dfd = new DFAgentDescription();

      // Set the agent offering the service
      dfd.setName(myAgent.getAID());

      for (String serviceType : providedServices) {
        // Create the service description that is published in the DF
        ServiceDescription sd = new ServiceDescription();

        // Set the service type and name
        sd.setType(serviceType);
        sd.setName(serviceType);

        // Add the service description to the agent description
        dfd.addServices(sd);
      }

      // Register the agent description to the yellow pages of the DF
      try {
        DFService.register(myAgent, dfd);
      } catch (FIPAException fe) {
        fe.printStackTrace();
      }
    }
  }

  /**
   * <p>A SearchServiceBehavior is a JADE behavior for searching a service in the Directory Facilitator (DF).</p>
   * <p>The class extends the generic {@link Behaviour} class, i.e. the behavior {@link #action()} is executed
   * until  {@link #done()} returns true. Here, {@link #done()} returns true only if at least one agent providing
   * the desired service has been discovered in the DF.</p>
   */
  protected class SearchServiceBehavior extends Behaviour {

    private final String serviceType;

    /**
     * Constructs a {@link SearchServiceBehavior} for searching a service in the DF.
     * <p>
     *
     * @param serviceType the type of service to search for
     */
    public SearchServiceBehavior(String serviceType) {
      this.serviceType = serviceType;
    }

    @Override
    public void action() {
      // Prepare the template for searching service of serviceType
      DFAgentDescription template = new DFAgentDescription();
      ServiceDescription sd = new ServiceDescription();
      sd.setType(serviceType);
      template.addServices(sd);
      try {
        // Search the DF for agent descriptions that match the template
        DFAgentDescription[] result = DFService.search(myAgent, template);

        // Store the agents that offer services of serviceType
        Set<AID> agents = new HashSet<>();
        for (DFAgentDescription serviceProviderDesc : result) {
          agents.add(serviceProviderDesc.getName());
        }
        serviceProviders.put(serviceType, agents);
      } catch (FIPAException fe) {
        fe.printStackTrace();
      }
    }

    @Override
    public boolean done() {
      // The behavior terminates when agents service providers have been discovered for the serviceType
      // Otherwise, the search action is repeated
      return !serviceProviders.get(serviceType).isEmpty();
    }
  }
}
