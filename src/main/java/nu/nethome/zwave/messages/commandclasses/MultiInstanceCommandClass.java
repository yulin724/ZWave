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

package nu.nethome.zwave.messages.commandclasses;

import nu.nethome.zwave.messages.commandclasses.framework.*;
import nu.nethome.zwave.messages.framework.DecoderException;

import java.io.ByteArrayOutputStream;

/**
 *
 */
public class MultiInstanceCommandClass implements CommandClass {

    // Version 1
    public static final int GET = 0x04;
    public static final int REPORT = 0x05;
    public static final int ENCAP_V1 = 0x06;

    // Version 2
    public static final int ENDPOINT_GET = 0x07;
    public static final int ENDPOINT_REPORT = 0x08;
    public static final int CAPABILITY_GET = 0x09;
    public static final int CAPABILITY_REPORT = 0x0a;
    public static final int ENDPOINT_FIND = 0x0b;
    public static final int ENDPOINT_FIND_REPORT = 0x0c;
    public static final int ENCAP_V2 = 0x0d;

    public static final int COMMAND_CLASS = 0x60;
    public static final int ENCAPSULATION_HEADER_LENGTH = 4;

    public static class GetV2 extends CommandAdapter {
        public GetV2() {
            super(COMMAND_CLASS, ENDPOINT_GET);
        }
    }

    public static class Report extends CommandAdapter {
        private static final int DYNAMIC_ENDPOINTS = 0x80;
        private static final int IDENTICAL_ENDPOINTS = 0x40;
        public final boolean hasDynamicEndpoints;
        public final boolean hasOnlyIdenticalEndpoints;
        public final int numberOfEndpoints;


        public Report(byte[] data) throws DecoderException {
            super(data, COMMAND_CLASS, ENDPOINT_REPORT);
            int flags = in.read();
            hasDynamicEndpoints = (flags & DYNAMIC_ENDPOINTS) != 0;
            hasOnlyIdenticalEndpoints = (flags & IDENTICAL_ENDPOINTS) != 0;
            numberOfEndpoints = in.read() & 0x7F;
        }

        public static class Processor extends CommandProcessorAdapter<Report> {

            @Override
            public CommandCode getCommandCode() {
                return new CommandCode(COMMAND_CLASS, ENDPOINT_REPORT);
            }

            @Override
            public Report process(byte[] command, CommandArgument argument) throws DecoderException {
                return process(new Report(command), argument);
            }
        }

        @Override
        public String toString() {
            return String.format("{\"MultiInstanceV2.Report\":{\"hasDynamic\": \"%b\", \"identical\": \"%b\", \"numberOfEndpoints\": %d}}", hasDynamicEndpoints, hasOnlyIdenticalEndpoints, numberOfEndpoints);
        }
    }


    public static class EncapsulationV2 extends CommandAdapter {
        public final int instance;
        public final Command command;

        public EncapsulationV2(int instance, Command command) {
            super(COMMAND_CLASS, ENCAP_V2);
            this.instance = instance;
            this.command = command;
        }

        @Override
        protected void addCommandData(ByteArrayOutputStream result) {
            super.addCommandData(result);
            result.write(1); // ??
            result.write(instance);
            byte[] commandData = command.encode();
            result.write(commandData, 0, commandData.length);
        }

        public EncapsulationV2(byte[] data) throws DecoderException {
            super(data, COMMAND_CLASS, ENCAP_V2);
            in.read(); // ?? Seems to be 0
            instance = in.read();
            int commandLength = data.length - ENCAPSULATION_HEADER_LENGTH;
            byte[] commandData = new byte[commandLength];
            in.read(commandData, 0, commandLength);
            command = new UndecodedCommand(commandData);
        }

        @Override
        public String toString() {
            return String.format("{\"MultiInstance.EncapsulationV2\": {\"instance\": \"%d\", \"command\": %s}}", instance, command.toString());
        }

        public static class Processor extends CommandProcessorAdapter<EncapsulationV2> {

            private CommandProcessor commandProcessor;

            public Processor(CommandProcessor commandProcessor) {
                this.commandProcessor = commandProcessor;
            }

            public Processor() {
                this.commandProcessor = new MultiCommandProcessor();
            }

            @Override
            public CommandCode getCommandCode() {
                return new CommandCode(COMMAND_CLASS, ENCAP_V2);
            }

            @Override
            public EncapsulationV2 process(byte[] command, CommandArgument argument) throws DecoderException {
                return process(new EncapsulationV2(command), argument);
            }

            @Override
            protected EncapsulationV2 process(EncapsulationV2 command, CommandArgument node) throws DecoderException {
                Command realCommand = commandProcessor.process(command.command.encode(), new CommandArgument(node.sourceNode, command.instance));
                return new EncapsulationV2(command.instance, realCommand);
            }
        }
    }
}
