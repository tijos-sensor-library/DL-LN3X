package tijos.framework.sensor.dlln3x;

/**
 * Ack command for waiting 
 * @author tijos
 *
 */
class WaitAckCmd {
	int destAddr;
	int srcPort;
	int destPort;
	
	byte[] data;
	
	public WaitAckCmd() {
		
	}
	
	public WaitAckCmd(int destAddr, int srcPort, int destPort) {
		this.destAddr = destAddr;
		this.srcPort = srcPort;
		this.destPort = destPort;
	}

	public void setCmd(int destAddr, int srcPort, int destPort) {
		this.destAddr = destAddr;
		this.srcPort = srcPort;
		this.destPort = destPort;
		this.data = null;
	}

	public boolean ackArrived(int destAddr, int srcPort, int destPort) {
		if((this.destAddr == destAddr)
				&& this.srcPort == srcPort 
				&& this.destPort == destPort)
			return true;
		
		return false;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void reset() {
		this.destAddr = 0;
		this.srcPort = 0;
		this.destPort = 0;
	}
}