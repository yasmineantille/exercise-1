# Exercise 1: Writing Your First Autonomous Agent!

A partial [JADE](https://jade.tilab.com/documentation/tutorials-guides/jade-administration-tutorial/architecture-overview/) implementation of the Room Automation Use Case.

### Table of Contents
-   [JADE agents](#jade-agents)
-   [Agents' provided services](#agents-provided-services)
-   [Agent-to-agent communication](#agent-to-agent-communication)
    -   [FIPA Subscribe Interaction Protocol](#fipa-subscribe-interaction-protocol)
    -   [FIPA Request Interaction Protocol](#fipa-request-interaction-protocol)
    -   [FIPA Contract Net Interaction Protocol](#fipa-contract-net-interaction-protocol)
-   [Project structure](#project-structure)
-   [How to run the project](#how-to-run-the-project)

### JADE agents
The system features a main container where four different agents register in total:
- A Building Environment Agent (ENV) that models the building environment.
- A Room Manager Agent (MNG) that perceives the environment and manages the room conditions.
- A Blinds Controller Agent (BLINDS) that controls the blinds in the room (e.g. it raises the blinds).
- A Lamp Controller Agent (LAMP) that controls the light in the room (e.g. it turns on the light).

### Agents' provided services 
|        Service       | Provided by (Agent) | Searched by (Agent) | Associated FIPA Interaction Protocol |
|:--------------------:|:-------------------:|:-------------------:|:------------------------------------:|
|   read-illuminance   |         ENV         |         MNG         |              [Subscribe](http://www.fipa.org/specs/fipa00035/SC00035H.html)               |
|     read-weather     |         ENV         |         MNG         |              [Subscribe](http://www.fipa.org/specs/fipa00035/SC00035H.html)               |
|      set-weather     |         ENV         |     BLINDS, LAMP    |               [Request](http://www.fipa.org/specs/fipa00026/SC00026H.html)                |
| increase-illuminance |     BLINDS, LAMP    |         MNG         |             [Contract Net](http://www.fipa.org/specs/fipa00029/SC00029H.html)             |

### Agent-to-agent communication
#### [FIPA Subscribe Interaction Protocol](http://www.fipa.org/specs/fipa00035/SC00035H.html)  
- Initiators:
  - Agents: MNG
  - Associated Behaviors: PerceiveEnvironment, HandleIlluminancePercept, HandleWeatherPercept
- Participants:
  - Agents: ENV
  - Associated Behaviors: SubscriptionServer, NotificationServer

#### [FIPA Request Interaction Protocol](http://www.fipa.org/specs/fipa00026/SC00026H.html) 
- Initiators:
  - Agents: BLINDS, LAMP
  - Associated Behaviors: RequestSetIlluminance
- Participants:
  - Agents: ENV
  - Associated Behaviors: SetIlluminanceServer

#### [FIPA Contract Net Interaction Protocol](http://www.fipa.org/specs/fipa00029/SC00029H.html)  
- Initiators:
  - Agents: MNG
  - Associated Behaviors: PerformContractNetProtocol
- Participants:
  - Agents: BLINDS, LAMP
  - Associated Behaviors: OfferProposalsServer, SatisfyOffersServer

### Project structure
The project is structured as follows:
```
├── cnp
│   ├── initiators
│   │   └── RoomManagerAgent.java
│   └── participants
│       ├── BlindsControllerAgent.java
│       └── LampControllerAgent.java
├── environment
│   ├── BuildingEnvironmentAgent.java
│   └── BuildingEnvironmentGUI.java
└── common
│   ├── BaseAgent.java
│   ├── CNPInitiator.java
│   └── CNPParticipant.java
```

### How to run the project
Run with [Gradle 7.4](https://gradle.org/):

To start the main container only with the Room Environment Agent (required for Task 1):
```shell
./gradlew runEnv
```
To start the main container with all the agents in the environment (required for Task 2):
```shell
./gradlew runRoomAll
```




