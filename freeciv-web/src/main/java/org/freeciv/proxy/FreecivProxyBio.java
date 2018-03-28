/*******************************************************************************
 * Freeciv-web - the web version of Freeciv. http://play.freeciv.org/
 * Copyright (C) 2009-2018 The Freeciv-web project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.freeciv.proxy;

import java.io.*;
import java.lang.Throwable;
import java.net.URI;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.*;
import javax.websocket.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * This class is responsible for proxying a websocket connection to a backend freeciv server.
 *
 * URL: /civsocket
 */
// @ServerEndpoint("/civsocket/{port}")
public class FreecivProxyBio {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(FreecivProxyBio.class);

	/** The Constant BUF_SIZE. */
	private static final int BUF_SIZE = 65536;

	/** The async remote. */
	private RemoteEndpoint.Async asyncRemote;

	/** The backend. */
	private Socket backend;

	/** The backend out. */
	private OutputStream backendOut;

	/** The writing client. */
	private AtomicBoolean writingClient = new AtomicBoolean();

	/** The client out. */
	private ConcurrentLinkedQueue<byte[]> clientOut;

	/** The client write handler. */
	private final ClientWriteHandler clientWriteHandler = this.new ClientWriteHandler();

	/** The loop. */
	private Thread loop;

	/**
	 * The Class BackendReadHandler. private class BackendReadHandler implements CompletionHandler<Integer, Object> {
	 * 
	 * /* (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#completed(java.lang.Object, java.lang.Object) public void completed(Integer result, Object v) { if (result == -1) { LOGGER.warn("eol"); return; }
	 *      readBuffer.flip(); while (true) { if (readBuffer.remaining() < 2) { break; } readBuffer.mark(); int packetSize = readBuffer.getShort(); if (packetSize > 32767 || packetSize <= 0) {
	 *      LOGGER.warn("invalid packet size", packetSize); return; }
	 * 
	 *      if (readBuffer.remaining() < packetSize - 2) { readBuffer.reset(); break; }
	 * 
	 *      byte[] bytes = new byte[packetSize - 1]; bytes[0] = '['; bytes[bytes.length - 1] = ']'; readBuffer.get(bytes, 1, packetSize - 3); readBuffer.get(); clientOut.offer(new String(bytes)); }
	 *      nextWriteClient(); readBuffer.compact(); backend.read(readBuffer, null, this); }
	 * 
	 *      /* (non-Javadoc)
	 * 
	 * @see java.nio.channels.CompletionHandler#failed(java.lang.Throwable, java.lang.Object) public void failed(Throwable exc, Object v) { LOGGER.warn("read failed", exc); } }
	 */

	/**
	 * The Class ClientWriteHandler.
	 */
	private class ClientWriteHandler implements SendHandler {

		/**
		 * On result.
		 *
		 * @param sendResult
		 *            the send result
		 */
		public void onResult(SendResult sendResult) {
			if (!sendResult.isOK()) {
				LOGGER.warn("client send failure", sendResult.getException());
			}
			byte[] bytes = clientOut.poll();
			if (bytes == null) {
				writingClient.set(false);
				return;
			}
			asyncRemote.sendText(new String(bytes), this);
		}
	}

	/**
	 * Next write client.
	 */
	private void nextWriteClient() {
		if (writingClient.compareAndSet(false, true)) {
			byte[] bytes = clientOut.poll();
			if (bytes == null) {
				writingClient.set(false);
				return;
			}
			asyncRemote.sendText(new String(bytes), clientWriteHandler);
		}
	}

	/**
	 * On open.
	 *
	 * @param port
	 *            the port
	 * @param session
	 *            the session
	 * @param config
	 *            the config
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@OnOpen
	public void onOpen(@PathParam("port") Integer port, Session session, EndpointConfig config) throws IOException {
		URI uri = session.getRequestURI();

		backend = new Socket("localhost", port);
		backend.setReceiveBufferSize(4096 * 128);
		backendOut = backend.getOutputStream();
		final InputStream in = backend.getInputStream();
		clientOut = new ConcurrentLinkedQueue<>();

		asyncRemote = session.getAsyncRemote();

		loop = new Thread(new Runnable() {
			public void run() {
				int packetSize = -1;
				int offset = 0;
				byte[] head = new byte[2];
				byte[] packet = new byte[1];
				try {
					while (!loop.isInterrupted()) {
						if (packetSize == -1) {
							int read = in.read(head, offset, 2 - offset);
							if (read + offset != 2) {
								continue;
							}
							packetSize = head[1] & 0xFF;
							packetSize += (head[0] & 0xFF) << 8;
							offset = 1;
							packet = new byte[packetSize - 1];
						}
						int read = in.read(packet, offset, packetSize - 1 - offset);
						offset += read;
						if (offset != packetSize - 1) {
							continue;
						}
						packet[0] = '[';
						packet[packetSize - 2] = ']';
						packetSize = -1;
						offset = 0;
						clientOut.offer(packet);
						nextWriteClient();
					}
				} catch (IOException exc) {
					LOGGER.error("ERROR! loop", exc);
				}
			}
		});
		loop.start();
	}

	/**
	 * On message.
	 *
	 * @param s
	 *            the s
	 * @param last
	 *            the last
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@OnMessage
	public void onMessage(String s, boolean last) throws IOException {
		byte[] bytes = s.getBytes();
		ByteBuffer buf = ByteBuffer.allocate(bytes.length + 3);
		int l = bytes.length + 3;
		backendOut.write(l >> 8);
		backendOut.write(l);
		backendOut.write(bytes);
		backendOut.write(0);
		backendOut.flush();
	}

	/**
	 * On close.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@OnClose
	public void onClose() throws IOException {
		backend.close();
		writingClient.set(false);
		loop.interrupt();
	}

	/**
	 * On error.
	 *
	 * @param exc
	 *            the exc
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@OnError
	public void onError(Throwable exc) throws IOException {
		LOGGER.error("ERROR! websocket error", exc);
		loop.interrupt();
		if (backend != null && !backend.isClosed()) {
			backend.close();
		}
	}

}
