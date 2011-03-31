package popjava.combox;

import popjava.base.*;
import popjava.baseobject.AccessPoint;
import popjava.baseobject.POPAccessPoint;
import popjava.buffer.*;
import popjava.util.Configuration;
import popjava.util.LogWriter;

import java.net.*;
import java.io.*;
/**
 * This combox implement the protocol Socket
 */
public class ComboxSocket extends Combox {
	protected Socket peerConnection = null;
	protected byte[] receivedBuffer;
	public static int BufferLength = 1024 * 32;
	protected InputStream inputStream = null;
	protected OutputStream outputStream = null;

	/**
	 * Create a new combox on the given socket
	 * @param socket	The socket to create the combox 
	 * @throws IOException	Thrown is any IO exception occurred during the creation
	 */
	public ComboxSocket(Socket socket) throws IOException {
		peerConnection = socket;
		receivedBuffer = new byte[BufferLength];
		inputStream = peerConnection.getInputStream();
		outputStream = peerConnection.getOutputStream();
	}

	
	public ComboxSocket(POPAccessPoint accesspoint, int timeout) {
		super(accesspoint, timeout);
		receivedBuffer = new byte[BufferLength];
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	@Override
	public void close() {
		try {
			if (peerConnection != null && !peerConnection.isClosed()) {
				{
					peerConnection.sendUrgentData(-1);
					outputStream.close();
					inputStream.close();
					peerConnection.close();
				}
			}

		} catch (IOException e) {
		}
	}

	@Override
	public boolean connect() {
		available = false;
		int accessPointSize = accessPoint.size();
		for (int i = 0; i < accessPointSize && !available; i++) {
			AccessPoint ap = accessPoint.get(i);
			if (ap.getProtocol().compareToIgnoreCase(
					ComboxSocketFactory.Protocol) != 0)
				continue;
			String host = ap.getHost();
			int port = ap.getPort();
			try {
				// Create an unbound socket
				if (timeOut > 0) {
					SocketAddress sockaddress = new InetSocketAddress(host,
							port);
					peerConnection = new Socket();
					peerConnection.connect(sockaddress, timeOut);
				} else {
					peerConnection = new Socket(host, port);
				}
				inputStream = peerConnection.getInputStream();
				outputStream = peerConnection.getOutputStream();
				available = true;
			} catch (UnknownHostException e) {
				available = false;
			} catch (SocketTimeoutException e) {
				available = false;
			} catch (IOException e) {
				available = false;
			}
		}
		return available;
	}

	@Override
	public int receive(Buffer buffer) {
		int result = 0;
		try {
			buffer.resetToReceive();
			// Receive message length
			byte[] temp = new byte[4];
			inputStream.read(temp);
			int messageLength = buffer.getTranslatedInteger(temp);
			if (messageLength <= 0) {
				close();
				return -1;
			}
			result = 4;
			buffer.putInt(messageLength);
			messageLength = messageLength - 4;
			int receivedLength = 0;
			while (messageLength > 0) {
				int count = messageLength < BufferLength ? messageLength
						: BufferLength;
				receivedLength = inputStream.read(receivedBuffer, 0, count);
				if (receivedLength > 0) {
					messageLength -= receivedLength;
					result += receivedLength;
					buffer.put(receivedBuffer, 0, receivedLength);

				} else
					break;
			}

			int headerLength = MessageHeader.HeaderLength;
			if (result < headerLength) {

				if (Configuration.DebugCombox) {
					String logInfo = String.format(
							"%s.fail to receive header.receivedLength=%d<%d",
							this.getClass().getName(), result, headerLength);
					LogWriter.writeDebugInfo(logInfo);
				}
				close();
			} else {
				buffer.extractHeader();				
			}
			return result;
		} catch (Exception e) {
			if (Configuration.DebugCombox)
				popjava.util.LogWriter
						.writeDebugInfo("ComboxSocket Error while receiving data:"
								+ e.getMessage());
			close();
			return -1;
		}

	}

	@Override
	public int send(Buffer buffer) {
		try {
			buffer.packMessageHeader();
			int length = buffer.size();
			byte[] dataSend = buffer.array();
			outputStream.write(dataSend, 0, length);
			outputStream.flush();
			
			return length;
		} catch (IOException e) {
			if (Configuration.DebugCombox)
				LogWriter.writeDebugInfo(this.getClass().getName()
						+ "-Send:  Error while send data - " + e.getMessage());
			return -1;
		}

	}

}