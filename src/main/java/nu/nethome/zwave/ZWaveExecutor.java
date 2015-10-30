/**
 * Copyright (C) 2005-2015, Stefan Strömberg <stestr@nethome.nu>
 *
 * This file is part of OpenNetHome.
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nu.nethome.zwave;

import nu.nethome.zwave.messages.*;
import nu.nethome.zwave.messages.commandclasses.*;
import nu.nethome.zwave.messages.commandclasses.framework.Command;
import nu.nethome.zwave.messages.commandclasses.framework.CommandCode;
import nu.nethome.zwave.messages.commandclasses.framework.MultiCommandProcessor;
import nu.nethome.zwave.messages.framework.DecoderException;
import nu.nethome.zwave.messages.framework.Message;
import nu.nethome.zwave.messages.framework.MultiMessageProcessor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class ZWaveExecutor {

    public interface MessageSender {
        void sendZWaveMessage(byte[] zWaveMessage);
    }

    public interface Printer {
        void println(String message);
        void print(String message);
    }

    private MultiMessageProcessor messageProcessor;
    private final MessageSender sender;
    private final Printer printer;

    public ZWaveExecutor(MessageSender sender, Printer printer) {
        this.sender = sender;
        this.printer = printer;
        MultiCommandProcessor commandProcessor = new MultiCommandProcessor();
        commandProcessor.addCommandProcessor(new CommandCode(MultiInstanceAssociationCommandClass.COMMAND_CLASS, MultiInstanceAssociationCommandClass.ASSOCIATION_REPORT), new MultiInstanceAssociationCommandClass.Report.Processor());
        commandProcessor.addCommandProcessor(new CommandCode(ConfigurationCommandClass.COMMAND_CLASS, ConfigurationCommandClass.REPORT_CONFIGURATION), new ConfigurationCommandClass.Report.Processor());
        commandProcessor.addCommandProcessor(new CommandCode(SwitchBinaryCommandClass.COMMAND_CLASS, SwitchBinaryCommandClass.SWITCH_BINARY_REPORT), new SwitchBinaryCommandClass.Report.Processor());
        commandProcessor.addCommandProcessor(new CommandCode(SwitchBinaryCommandClass.COMMAND_CLASS, SwitchBinaryCommandClass.SWITCH_BINARY_SET), new SwitchBinaryCommandClass.Set.Processor());
        commandProcessor.addCommandProcessor(new CommandCode(BasicCommandClass.COMMAND_CLASS, BasicCommandClass.REPORT), new BasicCommandClass.Report.Processor());
        commandProcessor.addCommandProcessor(new CommandCode(BasicCommandClass.COMMAND_CLASS, BasicCommandClass.SET), new BasicCommandClass.Set.Processor());
        commandProcessor.addCommandProcessor(new CommandCode(MultiInstanceCommandClass.COMMAND_CLASS, MultiInstanceCommandClass.ENCAP_V2), new MultiInstanceCommandClass.EncapsulationV2.Processor(commandProcessor));
        messageProcessor = new MultiMessageProcessor();
        messageProcessor.addMessageProcessor(MemoryGetId.MEMORY_GET_ID, Message.Type.RESPONSE, new MemoryGetId.Response.Processor());
        messageProcessor.addMessageProcessor(SendData.REQUEST_ID, Message.Type.REQUEST, new SendData.Request.Processor());
        messageProcessor.addMessageProcessor(SendData.REQUEST_ID, Message.Type.RESPONSE, new SendData.Response.Processor());
        messageProcessor.addMessageProcessor(AddNode.REQUEST_ID, Message.Type.REQUEST, new AddNode.Event.Processor());
        messageProcessor.addMessageProcessor(GetInitData.REQUEST_ID, Message.Type.RESPONSE, new GetInitData.Response.Processor());
        messageProcessor.addMessageProcessor(ApplicationCommand.REQUEST_ID, Message.Type.REQUEST, new ApplicationCommand.Request.Processor(commandProcessor));
        printPrompt();
    }

    public String executeCommandLine(String commandLine) {
        try {
            CommandLineParser parameters = new CommandLineParser(commandLine);
            String command = parameters.getString();
            if (command.equalsIgnoreCase("MemoryGetId") || command.equalsIgnoreCase("MGI")) {
                sendRequest(new MemoryGetId.Request());
            } else if (command.equalsIgnoreCase("GetInitData") || command.equalsIgnoreCase("GID")) {
                sendRequest(new GetInitData.Request());
            } else if (command.equalsIgnoreCase("MultiInstanceAssociation.Get") || command.equalsIgnoreCase("MIA.G")) {
                sendCommand(parameters.getInt(1), new MultiInstanceAssociationCommandClass.Get(parameters.getInt(2)));
            } else if (command.equalsIgnoreCase("MultiInstanceAssociation.Set") || command.equalsIgnoreCase("MIA.S")) {
                sendCommand(parameters.getInt(1), new MultiInstanceAssociationCommandClass.Set(parameters.getInt(2), Collections.singletonList(parseAssociatedNode(parameters.getString(3)))));
            } else if (command.equalsIgnoreCase("MultiInstanceAssociation.Remove") || command.equalsIgnoreCase("MIA.R")) {
                sendCommand(parameters.getInt(1), new MultiInstanceAssociationCommandClass.Remove(parameters.getInt(2), Collections.singletonList(parseAssociatedNode(parameters.getString(3)))));
            } else if (command.equalsIgnoreCase("Configuration.Get") || command.equalsIgnoreCase("C.G")) {
                sendCommand(parameters.getInt(1), new ConfigurationCommandClass.Get(parameters.getInt(2)));
            } else if (command.equalsIgnoreCase("Configuration.Set") || command.equalsIgnoreCase("C.S")) {
                sendCommand(parameters.getInt(1), new ConfigurationCommandClass.Set(parameters.getInt(2), new Parameter(parameters.getInt(3), parameters.getInt(4))));
            } else if (command.equalsIgnoreCase("SwitchBinary.Set") || command.equalsIgnoreCase("SB.S")) {
                sendCommand(parameters.getInt(1), new SwitchBinaryCommandClass.Set(parameters.getInt(2) != 0));
            } else if (command.equalsIgnoreCase("SwitchBinary.Get") || command.equalsIgnoreCase("SB.G")) {
                sendCommand(parameters.getInt(1), new SwitchBinaryCommandClass.Get());
            } else if (command.equalsIgnoreCase("AddNode")) {
                sendRequest(new AddNode.Request(AddNode.Request.InclusionMode.fromName(parameters.getString(1))));
            }else if (command.equalsIgnoreCase("RequestNodeInfo") || command.equalsIgnoreCase("RNI")) {
                sendRequest(new RequstNodeInfo.Request(parameters.getInt(1)));
            } else if (command.equalsIgnoreCase("Help") || command.equalsIgnoreCase("h")) {
                println("MemoryGetId");
                println("GetInitData");
                println("AddNode [ANY CONTROLLER SLAVE EXISTING STOP STOP_FAILED]");
                println("MultiInstanceAssociation.Get node association");
                println("MultiInstanceAssociation.Set node association associatedNode");
                println("MultiInstanceAssociation.Remove node association associatedNode");
                println("Configuration.Get node parameter");
                println("Configuration.Set node parameter value length");
                println("SwitchBinary.Get node");
                println("SwitchBinary.Set node [0 1]");
            } else {
                println("Error: Unknown command");
            }
        } catch (Exception | DecoderException e) {
            println("Error: " + e.getMessage());
        }
        printPrompt();
        return "";
    }

    private void printPrompt() {
        print("> ");
    }

    private void sendCommand(int node, Command command) {
        sendRequest(new SendData.Request((byte) node, command, SendData.TRANSMIT_OPTIONS_ALL));
    }


    private void sendRequest(Message request) {
        sender.sendZWaveMessage(request.encode());
    }

    public void processZWaveMessage(byte[] message) {
        print("<< ");
        if (message.length == 1) {
            if (message[0] == ZWavePort.ACK) {
                println("ACK");
            } else if (message[0] == ZWavePort.NAK) {
                println("NAK");
            } else if (message[0] == ZWavePort.CAN) {
                println("CAN");
            } else if (message[0] == ZWavePort.SOF) {
                println("SOF");
            }
        } else {
            try {
                Message received = messageProcessor.process(message);
                if (received != null) {
                    println(received.toString());
                } else {
                    println("Unknown message: " + Hex.asHexString(message));
                }
            } catch (DecoderException|IOException e) {
                println(e.getMessage());
            }
        }
        printPrompt();
    }

    private AssociatedNode parseAssociatedNode(String s) {
        String[] parts = s.split("\\.");
        if (parts.length == 2) {
            return new AssociatedNode(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return new AssociatedNode(Integer.parseInt(s));
    }

    private void println(String string) {
        printer.println(string);
    }

    private void print(String string) {
        printer.print(string);
    }

    static class CommandLineParser {
        Iterator<String> elementsIterator;
        List<String> elements;

        CommandLineParser(String line) {
            elements = Arrays.asList(line.split(" "));
            this.elementsIterator = elements.iterator();
        }

        public String getString() {
            return elementsIterator.next();
        }

        public int getInt() {
            return Integer.parseInt(getString());
        }

        public int getInt(int position) {
            return Integer.parseInt(getString(position));
        }

        private String getString(int position) {
            return elements.get(position);
        }
    }
}
