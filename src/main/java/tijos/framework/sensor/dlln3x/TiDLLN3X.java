package tijos.framework.sensor.dlln3x;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;
import tijos.framework.util.LittleBitConverter;
import tijos.framework.util.logging.Logger;

/**
 * DL-LN3X Series 2.4G Ad-hoc Network Wireless Communication Module Driver for TiJOS 
 *
 */
public class TiDLLN3X extends Thread {

	Protocol protocol = null;

	// IO stream for UART
	InputStream input;
	OutputStream output;

	// Keep the UART read thread running
	private boolean keeprunning = true;

	IDeviceEventListener eventLisener = null;

	WaitAckCmd ackCmd = new WaitAckCmd();

	/**
	 * Initialize with uart and source port, source address 0 by default
	 * @param uart
	 * @param srcPort source port
	 */
	public TiDLLN3X(TiUART uart, int srcPort) {

		this(uart, 0, srcPort);
	}

	/**
	 * Initialize with uart, source address and source port 
	 * @param uart 
	 * @param srcAddr source address
	 * @param srcPort source port
	 */
	public TiDLLN3X(TiUART uart, int srcAddr, int srcPort) {

		TiUartInputStream in = new TiUartInputStream(uart);
		this.input = new BufferedInputStream(in, 256);
		this.output = new TiUartOutputStream(uart);

		this.protocol = new Protocol(srcAddr, srcPort);

		this.setDaemon(true);
		this.start();
	}

	/**
	 * Set source address and port 
	 * @param srcAddr
	 * @param srcPort
	 */
	public void setSrcAddress(int srcAddr, int srcPort) {
		this.protocol.setSrcAddress(srcAddr);
		this.protocol.setSrcPort(srcPort);
	}

	/**
	 * Event listener for data arrived from remote node
	 * @param listener
	 */
	public void setEventListener(IDeviceEventListener listener) {
		this.eventLisener = listener;
	}

	@Override
	public void run() {

		while (true) {
			try {
				while (keeprunning) {
					Delay.msDelay(10);

					while (input.available() < 6) {
						Delay.msDelay(10);
						continue;
					}

					int val = input.read();
					// head
					if (val == 0xFE) {
						int len = input.read();
						if (len < 4)
							continue;

						int srcPort = input.read();
						int destPort = input.read();

						int addrL = input.read();
						int addrH = input.read();

						len -= 4;
						while (input.available() < len + 1) {
							Delay.msDelay(10);
							continue;
						}

						byte[] buffer = new byte[len];
						int pos = 0;
						while (pos < len) {
							val = input.read();
							buffer[pos++] = (byte) val;
						}

						int end = (input.read() & 0xFF);
						if (end == 0xFF) {
							uartDataHandler(srcPort, destPort, (addrH * 256 + addrL), buffer);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * A package data handler
	 * 
	 * @param srcPort
	 * @param destPort
	 * @param remoteAddr
	 * @param buff
	 * @param length
	 */
	private void uartDataHandler(int srcPort, int destPort, int srcAddr, byte[] buff) {

		if (!responseAckNotify(srcAddr, srcPort, destPort, buff)) {
			if (this.eventLisener != null)
				this.eventLisener.onDataArrived(srcAddr,  srcPort, destPort, buff);
		}
	}

	private boolean responseAckNotify(int srcAddr, int srcPort, int destPort, byte[] data) {
		synchronized (this.ackCmd) {
			if (this.ackCmd.ackArrived(srcAddr, srcPort, destPort)) {
				this.ackCmd.setData(data);
				this.ackCmd.notifyAll();

				return true;
			}
		}

		return false;
	}

	/**
	 * Wait the ack response from cloud, time out is 2 seconds
	 * 
	 * @return payload data from ack command, if time out, null will be returned
	 */
	private byte[] waitAckNotify() {
		synchronized (this.ackCmd) {
			try {
				this.ackCmd.wait(5000);
				// this.ackCmd.wait();

				if (ackCmd.data == null) {
					System.out.println("Time out");
				}

				this.ackCmd.reset();

				return ackCmd.data;
			} catch (InterruptedException ie) {

			}
		}

		return null;

	}

	/**
	 * Red LED control
	 * 
	 * @param destAddress
	 *            remote or local module
	 * @param time
	 *            for LED on in ms
	 * @throws IOException
	 */
	public void turnOnLED(int destAddress, int time) throws IOException {

		time /= 100;
		this.protocol.setRemoteAddress(destAddress);
		this.protocol.setDestinationPort(0x20);

		byte[] data = new byte[] { (byte) time };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);

	}

	/**
	 * Transmit data to the remote module
	 * 
	 * @param destAddress
	 *            module address
	 * @param port
	 *            port
	 * @param data
	 *            data to be transmitted
	 * @throws IOException
	 */
	public void transmit(int destAddress, int port, byte[] data) throws IOException {
		this.protocol.setRemoteAddress(destAddress);
		this.protocol.setDestinationPort(port);

		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);
	}

	/**
	 * Get device information
	 * 
	 * @param type
	 *            information type
	 * @return
	 * @throws IOException
	 */
	private byte[] getDevInfo(int type) throws IOException {

		this.protocol.setRemoteAddress(0x00); // local
		this.protocol.setDestinationPort(0x21);

		byte[] data = new byte[] { (byte) type };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);

		this.ackCmd.setCmd(0x00, 0x21, this.protocol.getSourcePort());

		byte[] resp = waitAckNotify();

		if (resp == null)
			throw new IOException("No response from device.");

		if (resp.length == 1 && resp[0] != 0)
			throw new IOException("Error code " + resp[0]);

		return resp;
	}

	/**
	 * Set device information
	 * 
	 * @param type
	 *            information type
	 * @param value
	 *            value in byte
	 * @return
	 * @throws IOException
	 */
	private void setDevInfo(int type, byte value) throws IOException {

		this.protocol.setRemoteAddress(0x00); // local
		this.protocol.setDestinationPort(0x21);

		byte[] data = new byte[] { (byte) type, value };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);

		this.ackCmd.setCmd(0x00, 0x21, this.protocol.getSourcePort());
		byte[] resp = waitAckNotify();

		if (resp == null)
			throw new IOException("No response from device.");

		if (resp.length == 1 && resp[0] != 0)
			throw new IOException("Error code " + resp[0]);

	}

	/**
	 * Set device information
	 * 
	 * @param type
	 *            information type
	 * @param value
	 *            value in word
	 * @return
	 * @throws IOException
	 */
	private void setDevInfo(int type, int value) throws IOException {

		this.protocol.setRemoteAddress(0x00); // local
		this.protocol.setDestinationPort(0x21);

		byte[] data = new byte[] { (byte) type, (byte) value, (byte) (value >>> 8) };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);

		this.ackCmd.setCmd(0x00, 0x21, this.protocol.getSourcePort());
		byte[] resp = waitAckNotify();

		if (resp == null)
			throw new IOException("No response from device.");

		if (resp[0] != 0)
			throw new IOException("Error code " + resp[0]);
	}

	/**
	 * Get device address
	 * 
	 * @return
	 * @throws IOException
	 */
	public int getAddress() throws IOException {
		byte[] addr = getDevInfo(1);
		if (addr.length != 3 || addr[0] != 0x21)
			throw new IOException("Invalid response length");

		return LittleBitConverter.ToUInt16(addr, 1);
	}

	/**
	 * Get device network id
	 * 
	 * @return
	 * @throws IOException
	 */
	public int getNetworkID() throws IOException {
		byte[] network = getDevInfo(2);
		if (network.length != 3 || network[0] != 0x22)
			throw new IOException("Invalid response length");

		return LittleBitConverter.ToUInt16(network, 1);
	}

	/**
	 * Get channel ID of communication
	 * 
	 * @return
	 * @throws IOException
	 */
	public int getChannelID() throws IOException {
		byte[] info = getDevInfo(3);
		if (info.length != 2 || info[0] != 0x23)
			throw new IOException("Invalid response length");

		return info[1];
	}

	/**
	 * Get baudrate of UART
	 * 
	 * @return
	 * @throws IOException
	 */
	public int getUartBaudrate() throws IOException {
		byte[] br = getDevInfo(4);
		if (br.length != 2 || br[0] != 0x24) {
			throw new IOException("Invalid response length");
		}

		return br[1];
	}

	/**
	 * Set new address of the device
	 * 
	 * @param addr
	 * @throws IOException
	 */
	public void setAddress(int addr) throws IOException {
		this.setDevInfo(0x11, addr);
	}

	/**
	 * Set new network ID of the device
	 * 
	 * @param networkID
	 * @throws IOException
	 */
	public void setNetworkID(int networkID) throws IOException {
		this.setDevInfo(0x12, networkID);
	}

	/**
	 * Set communication channel
	 * 
	 * @param chn
	 * @throws IOException
	 */
	public void setChannel(byte chn) throws IOException {
		this.setDevInfo(0x13, chn);
	}

	/**
	 * Set baud rate for communication
	 * 
	 * @param baudrate
	 * @throws IOException
	 */
	public void setBaudrate(byte baudrate) throws IOException {
		this.setDevInfo(0x14, baudrate);
	}

	/**
	 * Restart
	 * 
	 * @throws IOException
	 */
	public void restart() throws IOException {
		this.protocol.setRemoteAddress(0x00);
		this.protocol.setDestinationPort(0x21);

		byte[] data = new byte[] { 0x10 };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);
	}

	/**
	 * Detect connection quality of modules
	 * 
	 * @param remoteAddr
	 * @param address2
	 * @throws IOException
	 */
	public int detectConnectionQuality(int remoteAddr, int address2) throws IOException {
		this.protocol.setRemoteAddress(remoteAddr);
		this.protocol.setDestinationPort(0x23);

		byte[] data = new byte[] { (byte) (address2 >>> 3), (byte) address2 };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);

		this.ackCmd.setCmd(0x00, 0x23, this.protocol.getSourcePort());
		byte[] resp = waitAckNotify();

		if (resp == null)
			throw new IOException("No response from device.");

		return resp[2];
	}

	/**
	 * TTL Control command
	 * 
	 * @param remoteAddr
	 * @param port
	 * @param val
	 * @throws IOException
	 */
	public void TTLControlWrite(int remoteAddr, int port, int val) throws IOException {
		this.protocol.setRemoteAddress(remoteAddr);
		this.protocol.setDestinationPort(port);

		byte[] data = new byte[] { (byte) val };
		byte[] pack = this.protocol.genDataPack(data);

		this.output.write(pack);
	}

}
