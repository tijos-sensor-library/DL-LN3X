package tijos.framework.sensor.dlln3x;

import java.io.IOException;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;

/**
 * DL-LN3X Series 2.4G communication module sample 
 *
 */

class DLLN3XEventListener implements IDeviceEventListener {

	@Override
	public void onDataArrived(int srcAddr,  int srcPort, int destPort,byte[] buff) {

		System.out.println("onDataArrived srcPort" + srcPort + "  destPort " + destPort + " srcAddr " + srcAddr);
		System.out.println(Formatter.toHexString(buff));
	}

}

public class TiDLLN3XSample {

	public static void main(String[] args) {

		System.out.println("Hello DLLN3X!");

		TiUART uart;
		try {
			
			/**
			 * Open UART Port 0
			 */
			uart = TiUART.open(0);
			uart.setWorkParameters(8, 1, TiUART.PARITY_NONE, 115200);

			
			DLLN3XEventListener listener = new DLLN3XEventListener();

			/**
			 * Set source port for 0x80 by default
			 */
			TiDLLN3X ln3x = new TiDLLN3X(uart, 0X80);
			ln3x.setEventListener(listener);
			
			//Red on for 2 seconds
			ln3x.turnOnLED(0x0000, 2000); // on 2 second

			/**
			 * Get configuration 
			 */
			try {
				System.out.println(Integer.toHexString(ln3x.getAddress()));
				System.out.println(Integer.toHexString(ln3x.getChannelID()));
				System.out.println(Integer.toHexString(ln3x.getNetworkID()));
				System.out.println(Integer.toHexString(ln3x.getUartBaudrate()));
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			/**
			 * Set source address and port for transmission
			 */
			ln3x.setSrcAddress(0x0000, 0x90);

			/*
			 * Set data to the ports 
			 */
			byte[] buff = new byte[1];

			int val = 0;
			while (true) {

				buff[0] = (byte) (val++);
				/**
				 * Transmit data to the port of the remote address 
				 */
				ln3x.transmit(0x001F, 0xA0, buff); 
				buff[0] = (byte) (val + 10);
				ln3x.transmit(0x001F, 0xA1, buff);

				Delay.msDelay(5000);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
