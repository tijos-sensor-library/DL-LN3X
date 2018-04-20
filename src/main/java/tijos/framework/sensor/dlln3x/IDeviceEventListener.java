package tijos.framework.sensor.dlln3x;

public interface IDeviceEventListener {

	void onDataArrived(int srcPort, int destPort, int destAddr, byte[] buff);
	
	
}
