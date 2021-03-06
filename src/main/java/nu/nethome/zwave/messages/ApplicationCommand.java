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

package nu.nethome.zwave.messages;

import nu.nethome.zwave.messages.commandclasses.CommandArgument;
import nu.nethome.zwave.messages.commandclasses.framework.*;
import nu.nethome.zwave.messages.framework.DecoderException;
import nu.nethome.zwave.messages.framework.Message;
import nu.nethome.zwave.messages.framework.MessageAdaptor;
import nu.nethome.zwave.messages.framework.MessageProcessorAdaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 */
public class ApplicationCommand {

    public static final byte REQUEST_ID = (byte) 0x04;

    public static class Request extends MessageAdaptor {
        public final int node;
        public final Command command;

        public Request(byte node, Command command) {
            super(REQUEST_ID, Type.REQUEST);
            this.node = node;
            this.command = command;
        }

        @Override
        protected void addRequestData(ByteArrayOutputStream result) throws IOException {
            byte[] commandData = command.encode();
            super.addRequestData(result);
            result.write(0);
            result.write(node);
            result.write(commandData.length);
            result.write(commandData);
        }

        public Request(final byte[] data) throws IOException, DecoderException {
            this(data, new CommandProcessor() {
                @Override
                public CommandCode getCommandCode() {
                    try {
                        return CommandAdapter.decodeCommandCode(data);
                    } catch (DecoderException e) {
                        return null;
                    }
                }

                @Override
                public Command process(byte[] commandData, CommandArgument argument) throws DecoderException {
                    return new UndecodedCommand(commandData);
                }
            });
        }

        public Request(byte[] data, CommandProcessor processor) throws DecoderException {
            super(data, REQUEST_ID, Type.REQUEST);
            try {
                in.read(); // ?? Seems to be zero
                node = in.read();
                int commandLength = in.read();
                byte[] commandData = new byte[commandLength];
                in.read(commandData);
                command = processor.process(commandData, new CommandArgument(node));
            } catch (IOException e) {
                throw new DecoderException(e.getMessage());
            }
        }

        @Override
        public String toString() {
            return String.format("{\"ApplicationCommand.Request\":{\"node\":\"%d\", \"command\":%s}}", node, command.toString());
        }

        public static class Processor extends MessageProcessorAdaptor<Request> {

            private CommandProcessor commandProcessor;

            public Processor(CommandProcessor commandProcessor) {
                this.commandProcessor = commandProcessor;
            }

            public Processor() {
                this.commandProcessor = new MultiCommandProcessor();
            }

            @Override
            public Message process(byte[] message) throws DecoderException {
                return process(new Request(message, commandProcessor));
            }
        }
    }
}
