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

import java.io.IOException;
import java.lang.Throwable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.*;
import javax.websocket.server.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freeciv.utils.Constants;

// TODO: Auto-generated Javadoc
/**
 * This class is responsible for proxying a websocket connection to a backend freeciv server.
 *
 * URL: /civsocket
 */
@ServerEndpoint("/civsocket/{port}")
public class FreecivProxy {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(FreecivProxy.class);

	/** The Constant BUF_SIZE. */
	private static final int BUF_SIZE = 65536;

	/** The read buffer. */
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_SIZE);

	/** The async remote. */
	private RemoteEndpoint.Async asyncRemote;

	/** The backend. */
	private AsynchronousSocketChannel backend;

	/** The writing backend. */
	private AtomicBoolean writingBackend = new AtomicBoolean();

	/** The writing client. */
	private AtomicBoolean writingClient = new AtomicBoolean();

	/** The write buffer. */
	private ByteBuffer writeBuffer;

	/** The backend out. */
	private ConcurrentLinkedQueue<ByteBuffer> backendOut;

	/** The client out. */
	private ConcurrentLinkedQueue<String> clientOut;

	/** The backend read handler. */
	private final BackendReadHandler backendReadHandler = this.new BackendReadHandler();

	/** The backend write handler. */
	private final BackendWriteHandler backendWriteHandler = this.new BackendWriteHandler();

	/** The client write handler. */
	private final ClientWriteHandler clientWriteHandler = this.new ClientWriteHandler();

	/**
	 * The Class BackendReadHandler.
	 */
	private class BackendReadHandler implements CompletionHandler<Integer, Object> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.nio.channels.CompletionHandler#completed(java.lang.Object, java.lang.Object)
		 */
		public void completed(Integer result, Object v) {
			if (result == -1) {
				LOGGER.warn("eol");
				return;
			}
			readBuffer.flip();
			while (true) {
				if (readBuffer.remaining() < 2) {
					break;
				}
				readBuffer.mark();
				int packetSize = readBuffer.getShort();
				if (packetSize > 32767 || packetSize <= 0) {
					LOGGER.warn("invalid packet size", packetSize);
					return;
				}

				if (readBuffer.remaining() < packetSize - 2) {
					readBuffer.reset();
					break;
				}

				byte[] bytes = new byte[packetSize - 1];
				bytes[0] = '[';
				bytes[bytes.length - 1] = ']';
				readBuffer.get(bytes, 1, packetSize - 3);
				readBuffer.get();
				clientOut.offer(new String(bytes));
			}
			nextWriteClient();
			readBuffer.compact();
			backend.read(readBuffer, null, this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.nio.channels.CompletionHandler#failed(java.lang.Throwable, java.lang.Object)
		 */
		public void failed(Throwable exc, Object v) {
			LOGGER.warn("read failed", exc);
		}
	}

	/**
	 * The Class BackendWriteHandler.
	 */
	private class BackendWriteHandler implements CompletionHandler<Integer, Object> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.nio.channels.CompletionHandler#completed(java.lang.Object, java.lang.Object)
		 */
		public void completed(Integer result, Object v) {
			if (!writeBuffer.hasRemaining()) {
				writeBuffer = backendOut.poll();
				if (writeBuffer == null) {
					writingBackend.set(false);
					return;
				}
			}
			backend.write(writeBuffer, null, this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.nio.channels.CompletionHandler#failed(java.lang.Throwable, java.lang.Object)
		 */
		public void failed(Throwable exc, Object v) {
			LOGGER.warn("write failed", exc);
		}
	}

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
			String s = clientOut.poll();
			if (s == null) {
				writingClient.set(false);
				return;
			}
			asyncRemote.sendText(s, this);
		}
	}

	/**
	 * Next write backend.
	 */
	@SuppressWarnings("unused")
	private void nextWriteBackend() {
		if (writingBackend.compareAndSet(false, true)) {
			writeBuffer = backendOut.poll();
			if (writeBuffer == null) {
				writingBackend.set(false);
				return;
			}
			backend.write(writeBuffer, null, backendWriteHandler);
		}
	}

	/**
	 * Next write client.
	 */
	private void nextWriteClient() {
		if (writingClient.compareAndSet(false, true)) {
			String s = clientOut.poll();
			if (s == null) {
				writingClient.set(false);
				return;
			}
			asyncRemote.sendText(s, clientWriteHandler);
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

		InetAddress myLocalIp = InetAddress.getLocalHost();

		LOGGER.debug("IP of my system is := " + myLocalIp.getHostAddress());

		// InetSocketAddress addr = new InetSocketAddress(Constants.HOSTNAME, port);

		InetSocketAddress addr = null;
		if ("0.0.0.0".equals(myLocalIp.getHostAddress())) {
			addr = new InetSocketAddress(Constants.HOSTNAME, port);
		} else {
			addr = new InetSocketAddress(myLocalIp.getHostAddress(), port);
		}

		backend = AsynchronousSocketChannel.open();
		backendOut = new ConcurrentLinkedQueue<>();
		clientOut = new ConcurrentLinkedQueue<>();

		asyncRemote = session.getAsyncRemote();

		backend.connect(addr, null, new CompletionHandler<Void, Object>() {
			public void completed(Void v, Object w) {
				backend.read(readBuffer, null, backendReadHandler);
			}

			public void failed(Throwable exc, Object v) {
				LOGGER.warn("backend connect error", exc);
			}
		});
	}

	/**
	 * On message.
	 *
	 * @param s
	 *            the s
	 * @param last
	 *            the last
	 */
	@OnMessage
	public void onMessage(String s, boolean last) {
		byte[] bytes = s.getBytes();
		ByteBuffer buf = ByteBuffer.allocate(bytes.length + 3);
		buf.putShort((short) (bytes.length + 3));
		buf.put(bytes);
		buf.rewind();
		if (writingBackend.compareAndSet(false, true)) {
			writeBuffer = buf;
			backend.write(writeBuffer, null, backendWriteHandler);
			return;
		}
		backendOut.offer(buf);
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
		readBuffer.clear();
		writingBackend.set(false);
		writingClient.set(false);
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
		if (backend != null && backend.isOpen()) {
			backend.close();
		}
	}

}
