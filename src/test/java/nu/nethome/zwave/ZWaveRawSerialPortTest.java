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

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import nu.nethome.zwave.messages.framework.DecoderException;
import nu.nethome.zwave.messages.framework.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

/**
 * 0120F9819C1C012F
 */
public class ZWaveRawSerialPortTest {

    public static final int MESSAGE_LENGTH = 10;
    private ZWaveRawSerialPort zWaveRawSerialPort;

    class Receiver implements ZWaveRawSerialPort.Receiver {

        public byte[] message = new byte[1];
        public byte frameByte = 0;

        @Override
        public void receiveMessage(byte[] message) {
            this.message = message;
        }

        @Override
        public void receiveFrameByte(byte frameByte) {
            this.frameByte = frameByte;
        }
    }

    private SerialPort port;
    private Receiver receiver;

    @Before
    public void setUp() throws Exception {
        port = mock(SerialPort.class);
        receiver = new Receiver();
        zWaveRawSerialPort = new ZWaveRawSerialPort("Name", port);
        zWaveRawSerialPort.setReceiver(receiver);
    }

    @Test
    public void canPassOnACK() throws Exception {
        byte[] ack = {ZWaveRawSerialPort.ACK};
        verifyPassOnFrameByte(ack);
    }

    @Test
    public void canPassOnNAK() throws Exception {
        byte[] ack = {ZWaveRawSerialPort.NAK};
        verifyPassOnFrameByte(ack);
    }

    @Test
    public void canPassOnCAN() throws Exception {
        byte[] ack = {ZWaveRawSerialPort.CAN};
        verifyPassOnFrameByte(ack);
    }

    @Test
    public void doesNotPassOnRandomFrameByte() throws Exception {
        byte[] randomFrameByte = {17};
        receiver.frameByte = 0;
        doReturn(randomFrameByte).when(port).readBytes(eq(1), anyInt());
        zWaveRawSerialPort.readMessage(port);
        assertThat(receiver.frameByte, is((byte)0));
    }

    private void verifyPassOnFrameByte(byte[] frameByte) throws SerialPortException, SerialPortTimeoutException, DecoderException, IOException {
        doReturn(frameByte).when(port).readBytes(eq(1), anyInt());
        zWaveRawSerialPort.readMessage(port);
        assertThat(receiver.frameByte, is(frameByte[0]));
    }

    @Test
    public void canReceiveAndAcknowledge10ByteMessage() throws Exception {
        byte[] sof = {ZWaveRawSerialPort.SOF};
        byte[] length = {MESSAGE_LENGTH + 1};
        when(port.readBytes(eq(1), anyInt())).thenReturn(sof, length);
        byte[] portData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        byte[] data = Arrays.copyOf(portData, MESSAGE_LENGTH);
        when(port.readBytes(MESSAGE_LENGTH, 1000)).thenReturn(portData);

        zWaveRawSerialPort.readMessage(port);

        assertThat(receiver.message, is(data));
        verify(port).writeByte((byte) ZWaveRawSerialPort.ACK);
    }

    @Test
    public void canSendMessageWithChecksum() throws Exception {
        byte[] interceptedMessage = Hex.hexStringToByteArray("01080120F9819C1C012F");
        byte[] message = Hex.hexStringToByteArray("0120F9819C1C01");
        byte checksum = Hex.hexStringToByteArray("2F")[0];

        zWaveRawSerialPort.sendMessage(message);

        verify(port).writeByte((byte) ZWaveRawSerialPort.SOF);
        verify(port).writeByte((byte)(message.length + 1));
        verify(port).writeBytes(message);
//        verify(port).writeBytes(interceptedMessage);
        verify(port).writeByte(checksum);
    }

}
