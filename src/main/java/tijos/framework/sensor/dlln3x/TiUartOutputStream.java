package tijos.framework.sensor.dlln3x;

import java.io.IOException;
import java.io.OutputStream;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Formatter;
import tijos.framework.util.logging.Logger;

/**
 * Output Stream for UART
 * @author lemon
 *
 */
public class TiUartOutputStream extends OutputStream {

	TiUART  uart = null;
	
	public TiUartOutputStream(TiUART uart) {
		this.uart = uart;
	}
	
	@Override
	public void write(int data) throws IOException {
	  byte [] temp = new byte[1];
	  temp[0] = (byte)data;
		
	  this.uart.write(temp, 0, 1);
	}

	public void write (byte[] b, int off, int len)    throws IOException{
				
		this.uart.write(b, off, len);
	}
}
