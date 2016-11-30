/*
 *  LCACloud
 *
 *  MIT License
 *
 *  Copyright (c) 2016
 *
 *  Geoffrey Mastenbroek, geoffrey.mastenbroek@student.hu.nl
 *  Feiko Wielsma, feiko.wielsma@student.hu.nl
 *  Robbin van den Berg, robbin.vandenberg@student.hu.nl
 *  Arnoud den Haring, arnoud.denharing@student.hu.nl
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.maschel.lca.cloud.agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Agent class to forward messages between a local device agent and a cloud agent. Needed to perform
 * mapping between a local Device ID and a cloud AID. All communication goes through this agent.
 */
public class CloudCommAgent extends Agent {

    private final String CLOUD_AGENT_DEVICE_PREFIX = "Cloud";

    protected void setup()
    {
        addBehaviour(new MessagePerformer(this));
    }

    private class MessagePerformer extends CyclicBehaviour {

        public MessagePerformer(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if(msg != null) {
                myAgent.addBehaviour(new MessageForwardingBehaviour(myAgent, msg));
            }
            else {
                block();
            }
        }

    }

    /**
     * MessageForwardingBehaviour, is called when a message is received by the CloudCommAgent
     * This will get or register a CloudDeviceAgent with it's attached Device instance
     * and then forward the received message from the CloudCommAgent to the found or created
     * CloudDeviceAgent.
     */
    private class MessageForwardingBehaviour extends OneShotBehaviour {

        private final String DEVICE_SERVICE_DESCRIPTION = "device";
        private final String DEVICE_ID_PARAMETER = "deviceId";

        private ACLMessage message;

        public MessageForwardingBehaviour(Agent a, ACLMessage msg) {
            super(a);
            this.message = msg;
        }

        @Override
        public void action() {

            String deviceId = message.getUserDefinedParameter(DEVICE_ID_PARAMETER);

            ServiceDescription serviceDescription  = new ServiceDescription();
            serviceDescription.setType(DEVICE_SERVICE_DESCRIPTION);
            serviceDescription.setName(deviceId);

            DFAgentDescription dfAgentDescription = new DFAgentDescription();
            dfAgentDescription.addServices(serviceDescription);

            AID agentIdentifier = getService(dfAgentDescription, deviceId);

            // Forward message
            if(agentIdentifier != null) {
                message.setSender(message.getSender()); // Use the original sender
                message.addReceiver(agentIdentifier);   // The CloudDeviceAgent
                message.removeReceiver(getAID());       // Remove the CLoudCommAgent as receiver

                send(message);
            } else {
                System.out.println("ERROR: Could not find the AID for the given deviceID");
            }

        }
    }

    //region DF Utility methods

    /**
     * Register a new CloudDeviceAgent in the DF
     * @param dfAgentDescription The Agent description
     * @param deviceId Device id
     */
    protected void register(DFAgentDescription dfAgentDescription, String deviceId)
    {
        CloudDeviceAgent cloudDeviceAgent = new CloudDeviceAgent();
        cloudDeviceAgent.setArguments(new Object[] { deviceId });

        dfAgentDescription.setName(cloudDeviceAgent.getAID());
        String cloudDeviceId = CLOUD_AGENT_DEVICE_PREFIX + deviceId;
        try {
            AgentContainer agentContainer = getContainerController();
            AgentController agentController = agentContainer.acceptNewAgent(cloudDeviceId, cloudDeviceAgent);
            agentController.start();
            DFService.register(cloudDeviceAgent, dfAgentDescription);
        } catch (Exception e) {
            System.out.println("ERROR: Failed to start agent: " + cloudDeviceId + ". " + e.getMessage());
        }
    }

    /**
     * Get the CloudDeviceAgent AID related to a given agent description
     * @param dfAgentDescription The Agent description
     * @param deviceId Device id
     * @return AID of the device
     */
    public AID getService(DFAgentDescription dfAgentDescription, String deviceId)
    {
        try
        {
            DFAgentDescription[] result = DFService.search(this, dfAgentDescription);
            if (result.length > 0) {
                // Agent exists, return it's AID
                return result[0].getName();
            }
            else {
                // Agent doesn't exists, so register
                register(dfAgentDescription, deviceId);
                return getService(dfAgentDescription, deviceId);
            }
        }
        catch (FIPAException fe) {
            System.out.println("ERROR: Failed to find or register CloudDeviceAgent. " + fe.getMessage());
            return null;
        }
    }

    //endregion
}
