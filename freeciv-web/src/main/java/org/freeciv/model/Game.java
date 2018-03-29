package org.freeciv.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class Game.
 */
public class Game {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LogManager.getLogger(Game.class);

	/** The host. */
	private String host;

	/** The port. */
	private int port;

	/** The version. */
	private String version;

	/** The patches. */
	private String patches;

	/** The state. */
	private String state;

	/** The message. */
	private String message;

	/** The duration. */
	private long duration;

	/** The players. */
	private int players;

	/** The turn. */
	private int turn;

	/** The flag. */
	private String flag;

	/** The player. */
	private String player;

	/**
	 * Gets the duration.
	 *
	 * @return the duration
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Gets the flag.
	 *
	 * @return the flag
	 */
	public String getFlag() {
		return flag;
	}

	/**
	 * Gets the host.
	 *
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Gets the message.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the patches.
	 *
	 * @return the patches
	 */
	public String getPatches() {
		return patches;
	}

	/**
	 * Gets the player.
	 *
	 * @return the player
	 */
	public String getPlayer() {
		return player;
	}

	/**
	 * Gets the players.
	 *
	 * @return the players
	 */
	public int getPlayers() {
		return players;
	}

	/**
	 * Gets the port.
	 *
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * Gets the turn.
	 *
	 * @return the turn
	 */
	public int getTurn() {
		return turn;
	}

	/**
	 * Gets the version.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Checks if is protected.
	 *
	 * @return true, if is protected
	 */
	public boolean isProtected() {
		return (message != null) && message.contains("password-protected");
	}

	/**
	 * Sets the duration.
	 *
	 * @param duration
	 *            the duration
	 * @return the game
	 */
	public Game setDuration(long duration) {
		this.duration = duration;
		return this;
	}

	/**
	 * Sets the flag.
	 *
	 * @param flag
	 *            the flag
	 * @return the game
	 */
	public Game setFlag(String flag) {
		this.flag = flag;
		return this;
	}

	/**
	 * Sets the host.
	 *
	 * @param host
	 *            the host
	 * @return the game
	 */
	public Game setHost(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Sets the message.
	 *
	 * @param message
	 *            the message
	 * @return the game
	 */
	public Game setMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Sets the patches.
	 *
	 * @param patches
	 *            the patches
	 * @return the game
	 */
	public Game setPatches(String patches) {
		this.patches = patches;
		return this;
	}

	/**
	 * Sets the player.
	 *
	 * @param player
	 *            the player
	 * @return the game
	 */
	public Game setPlayer(String player) {
		this.player = player;
		return this;
	}

	/**
	 * Sets the players.
	 *
	 * @param players
	 *            the players
	 * @return the game
	 */
	public Game setPlayers(int players) {
		this.players = players;
		return this;
	}

	/**
	 * Sets the port.
	 *
	 * @param port
	 *            the port
	 * @return the game
	 */
	public Game setPort(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Sets the state.
	 *
	 * @param state
	 *            the state
	 * @return the game
	 */
	public Game setState(String state) {
		this.state = state;
		return this;
	}

	/**
	 * Sets the turn.
	 *
	 * @param turn
	 *            the turn
	 * @return the game
	 */
	public Game setTurn(int turn) {
		this.turn = turn;
		return this;
	}

	/**
	 * Sets the version.
	 *
	 * @param version
	 *            the version
	 * @return the game
	 */
	public Game setVersion(String version) {
		this.version = version;
		return this;
	}

}
