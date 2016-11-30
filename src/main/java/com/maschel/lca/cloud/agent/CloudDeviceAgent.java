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

import com.google.gson.Gson;
import com.maschel.lca.cloud.agent.message.request.ActuatorRequestMessage;
import com.maschel.lca.cloud.device.Device;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CloudDeviceAgent extends Agent {

    private static final String SENSOR_ONTOLOGY = "sensor";
    private static final String ACTUATOR_ONTOLOGY = "actuator";

    private Gson gson = new Gson();

    private Device agentDevice;

    protected void setup() {

        // Get agent arguments
        Object[] args = getArguments();
        // Get agentDevice id
        if (args != null && args.length > 0) {
            String agentDeviceId = (String) args[0];
            agentDevice = new Device(agentDeviceId);
        } else {
            System.out.println("ERROR: No device id specified.");
            this.doDelete();
            return;
        }

        addBehaviour(new MessagePerformer(this));
    }

    private class MessagePerformer extends CyclicBehaviour {

        public MessagePerformer(Agent a) {
            super(a);
        }

        @Override
        public void action() {

            MessageTemplate mtPerformative = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mtPerformative);

            if(msg != null) {
                switch(msg.getOntology()) {
                    case SENSOR_ONTOLOGY:
                        myAgent.addBehaviour(new SensorBehaviour(myAgent, msg));
                        break;
                    case ACTUATOR_ONTOLOGY:
                        myAgent.addBehaviour(new ActuatorBehaviour(myAgent, msg));
                        break;
                }
            }
            else {
                block();
            }
        }

    }

    /**
     * SensorBehaviour, is called on a sensor request message
     */
    private class SensorBehaviour extends OneShotBehaviour {

        private ACLMessage message;

        public SensorBehaviour(Agent a, ACLMessage msg) {
            super(a);
            this.message = msg;
        }

        @Override
        public void action() {
            // TODO: Send content back to http API
        }
    }

    /**
     * ActuatorBehaviour, is called on a actuator command message
     */
    private class ActuatorBehaviour extends OneShotBehaviour {

        private ACLMessage message;

        public ActuatorBehaviour(Agent a, ACLMessage msg) {
            super(a);
            this.message = msg;
        }

        @Override
        public void action() {
            // TODO: Send content back to http API
        }
    }


    protected void takeDown()
    {
        try { DFService.deregister(this); }
        catch (Exception e) {
            System.out.println("ERROR: Failed to deregister CloudDeviceAgent from DF. " + e.getMessage());
        }
    }
}
