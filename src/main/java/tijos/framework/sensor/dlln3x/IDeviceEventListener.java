package tijos.framework.sensor.dlln3x;

/**
 * DLLN3X Event Listener 
 * @author TiJOS
 *
 */
public interface IDeviceEventListener {

	/**
	 * Data arrived from the remote node
	 * @param srcPort  source port
	 * @param destPort target port 
	 * @param srcAddr  remote address 
	 * @param buff data buffer arrived
	 */
	void onDataArrived(int srcAddr, int srcPort, int destPort,  byte[] buff);
	
	
}
