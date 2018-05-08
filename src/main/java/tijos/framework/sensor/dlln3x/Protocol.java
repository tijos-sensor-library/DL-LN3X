package tijos.framework.sensor.dlln3x;

import java.io.IOException;

class Protocol {

	int srcAddress = 0;
	int destAddress = 0;
	
	int srcPort = 0;
	int destPort = 0;
	
	int packLength = 0;
	
	
	public Protocol(int localAddr, int sourcePort){
		this.srcAddress = localAddr;
		this.srcPort = sourcePort;
	}
	
	public void setSrcAddress(int srcAddr) {
		this.srcAddress = srcAddr;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public void setRemoteAddress(int remoteAddr) {
		this.destAddress = remoteAddr;
	}

	public void setDestinationPort(int destinationPort) {
		this.destPort = destinationPort;
	}
	
	
	public int getSourcePort() {
		return this.srcPort;
	}
	
	public int getDestinationPort() {
		return this.destPort;
	}
	
	public int getDestinationAddress() {
		return this.destAddress;
	}
	
	public int getDataPackLength() {
		return this.packLength;
	}
	
	/**
	 * Generate data pack based on the protocol
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public byte [] genDataPack(byte [] data) throws IOException{
		int pos = 0;
		int estLen = 7 + estimateLength(data);

		byte [] pack = new byte[estLen];
		pack[pos ++] = (byte)0xFE;
		pack[pos ++] = (byte)(data.length + 4);
		pack[pos ++] = (byte)srcPort;
		pack[pos ++] = (byte)destPort;
		
		pack[pos] = (byte)(destAddress & 0xFF);
		pos = escape(pack, pos);
		pack[pos] = (byte)((destAddress >>> 8)& 0xFF);
		pos = escape(pack, pos);
		
		for(int i = 0 ; i < data.length ; i ++ ) {
			pack[pos] = data[i];
			pos = escape(pack, pos);
		}
		
		pack[pos ++] = (byte)0xFF;
		
		this.packLength = pos;
		return pack;
	}
	
	public byte [] parseDataPack(byte [] data , int packlength) throws IOException {
		int pos = 0;
		if(data[pos ++] != (byte)0xFE) 
			throw new IOException("Invalid package head.");
		
		if(data[packlength - 1] != (byte)0xFF)
			throw new IOException("Invalid package tail.");

		int length = data[pos ++];
		if(length > data.length - 4)
			throw new IOException("Invalid data length.");

		srcPort = data[pos ++];
		destPort = data[pos ++];
		
		int [] val = new int[1];
		
		pos = unescape(data, pos, val);
		int addrL = val[0];
		pos = unescape(data, pos, val);
		int addrH = val[0];
		
		this.destAddress = addrH *256 + addrL;
		
		byte [] out = new byte[length - 4];
		for(int i = 0; i < length - 4; i ++)
		{
			pos = (byte)unescape(data, pos, val);
			out[i] = (byte)val[0];
		}
		
		return out;
	}

	
	int estimateLength(byte [] data) {
		int total = data.length;
		for(int i = 0 ; i < data.length ; i ++ ) {
			if(data[i] == (byte)0xFF)
			{
				total ++;
			}
		}		
		
		return total;
	}
	
	int escape(byte [] pack, int pos) throws IOException
	{
		if(((pack[pos] == 0xFF) ||(pack[pos] == 0xFE)) 
				&& pos >= pack.length -1)
			throw new IOException("Invalid data");

		if(pack[pos] == (byte)0xFF) {
			pack[pos++] = (byte)0xFE;
			pack[pos++] = (byte)0xFD;
		}
		else if(pack[pos] == (byte)0xFE) {
			pack[pos++] = (byte)0xFE;
			pack[pos++] = (byte)0xFC;			
		}
		else {
			pos ++;
		}
		return pos;
	}
	
	int unescape(byte [] data, int pos, int [] val) throws IOException {
		val[0] = data[pos];
		if(data[pos] == (byte)0xFE && pos >= data.length -1)
			throw new IOException("Invalid data");
		
		if(data[pos] == (byte)0xFE && data[pos + 1] == (byte)0xFD) {
			val[0] = 0xFF;
			pos ++;
		}
		else if(data[pos] ==(byte) 0xFE && data[pos + 1] == (byte)0xFC) {
			val[0] = 0xFE;
			pos ++;
		}
		pos ++;
		return pos;
	}
	
}
