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
import jade.wrapper.AgentContainer;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;

/**
 * Agent class to forward messages between a local device agent and a cloud agent. Needed to perform
 * mapping between a local Device ID -> cloud AID. All communication goes through this agent.
 */
public class CloudCommAgent extends Agent {

    private AgentContainer c;

    protected void setup()
    {
        c = getContainerController();

        addBehaviour(new MessagePerformer(this));
    }

    // RECEIVE MESSAGES
    private class MessagePerformer extends CyclicBehaviour {

        public MessagePerformer(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if(msg != null) {
                myAgent.addBehaviour(new SorRBehaviour(myAgent, msg));
            }
            else {
                block();
            }
        }

    }

    /**
     * SorRBehaviour, is called on a search or register device request
     * This will search or register the cloud agent related to a device
     * and then forward the received message from this device to it's related cloud agent
     */
    private class SorRBehaviour extends OneShotBehaviour {

        private ACLMessage message;

        public SorRBehaviour(Agent a, ACLMessage msg) {
            super(a);
            this.message = msg;
        }

        @Override
        public void action() {
            // (json) string containing deviceId
            //String deviceId = message.getContent();
            String deviceId = message.getUserDefinedParameter("deviceId");

            // Create agent description
            DFAgentDescription dfd = new DFAgentDescription();
            //dfd.setName(message.getSender());

            // Register a local device (if not already exists)
            // Create service description
            ServiceDescription sd  = new ServiceDescription();
            // Use device id as type
            sd.setType("device");

            // Add deviceAID
            //Property deviceAID = new Property("deviceAID", message.getSender().getName());
            //sd.addProperties(deviceAID);

            // AID as name
            sd.setName(deviceId);
            dfd.addServices(sd);

            // Search or register cloud agent
            AID aid = getService( dfd, deviceId );

            // Forward to message to the cloud device agent
            if(aid != null) {
                // Set the sender to original sender of this message
                message.setSender(message.getSender());
                // Add receiver (the cloud agent we searched/registered above)
                message.addReceiver(aid);
                // Remove current receiver (CloudCommAgent)
                message.removeReceiver(getAID());
                // send the message to the cloud agent
                send(message);
            }

        }
    }

// -------------------- Utility methods to access DF ----------------


    protected void register( DFAgentDescription dfd, String deviceId)
    {

        String name = "Cloud" + deviceId;
        Object [] args = new Object[1];
        args[0] = deviceId;

        CloudDeviceAgent cda = new CloudDeviceAgent();
        cda.setArguments(args);

        dfd.setName(cda.getAID());

        System.out.println(name + " en " + deviceId);

        try {
            //AgentController a = c.createNewAgent(name, CloudDeviceAgent.class.getName(), args);
            AgentController a1 = c.acceptNewAgent(name, cda);
            //a.start();
            a1.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            DFService.register(cda,dfd);
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Get the CloudDeviceAgent AID related to a given agent description
     * @param dfd The Agent description
     * @param deviceId Device id
     * @return AID of the device
     */
    protected AID getService( DFAgentDescription dfd, String deviceId)
    {

        try
        {
            DFAgentDescription[] result = DFService.search(this, dfd);
            // Check if cloud agent already exists for this device id
            if (result.length>0) {
                // Agent exists, return it's AID
                return result[0].getName();
            }
            else {
                // Agent doesn't exists, so register it so it can be find in the next call
                register(dfd, deviceId);
                // Now the agent exists and can be returned
                return getService(dfd, deviceId);
            }
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
        return null;
    }



}
