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

import nu.nethome.zwave.Hex;
import nu.nethome.zwave.messages.commandclasses.SwitchBinaryCommandClass;
import org.junit.Test;

/**
 *
 */
public class ApplicationCommandRequestTest {

    @Test
    public void binarySwitch() throws Exception {
        ApplicationCommand.Request request = new ApplicationCommand.Request((byte) 2, new SwitchBinaryCommandClass.Set(false));
        String result = Hex.asHexString(request.encode());
        System.out.println("event,ZWave_Message,Direction,Out,Value," + result);
        request = new ApplicationCommand.Request((byte) 2, new SwitchBinaryCommandClass.Get());
        result = Hex.asHexString(request.encode());
        System.out.println("event,ZWave_Message,Direction,Out,Value," + result);
    }
}
