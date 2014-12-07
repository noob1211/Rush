package net.rush.model.entity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.rush.RushServer;
import net.rush.api.ChunkCoords;
import net.rush.model.RushChunk;
import net.rush.model.Session;
import net.rush.protocol.Packet;
import net.rush.protocol.packets.ChatMessage;
import net.rush.protocol.packets.DestroyEntity;
import net.rush.protocol.packets.PlayerLookAndPosition;
import net.rush.protocol.packets.SpawnPosition;
import net.rush.utils.MetaParam;

/**
 * Represents an in-game player.
 */
public final class RushPlayer extends RushEntity {

	/**
	 * The normal height of a player's eyes above their feet.
	 */
	public final double NORMAL_EYE_HEIGHT = 1.62D;

	/**
	 * The height of a player's eyes above their feet when they are crouching.
	 */
	public final double CROUCH_EYE_HEIGHT = 1.42D;

	public final String name;
	public final Session session;
	public final RushServer server;
	
	public final HashSet<RushEntity> knownEntities = new HashSet<>();
	public final HashSet<ChunkCoords> knownChunks = new HashSet<>();

	public boolean sprinting = false;
	private boolean crouching = false;

	public RushPlayer(Session session, String name) {
		super(session.server.world);

		this.name = name;
		this.session = session;
		this.server = session.server;
		
		setPosition(0, 80, 0);

		// stream the initial set of blocks and teleport us
		streamBlocks();

		session.sendPacket(new SpawnPosition(0, 80, 0)); // compass
		session.sendPacket(new PlayerLookAndPosition(posX, posY, posZ, posY + NORMAL_EYE_HEIGHT, 0F, 0F, true));

		server.getLogger().info(name + " [" + session.removeAddress() + "] logged in with id " + id + " at ([world] x:? y:? z:?)");
		//server.broadcastMessage("&e" + name + " has joined the game.");
		
		sendMessage("%Rush Welcome to Rush, " + name);
	}

	public void sendMessage(String message) {
		session.sendPacket(new ChatMessage(message));
	}

	@Override
	public void pulse() {
		super.pulse();
		
		streamBlocks();

		for (Iterator<RushEntity> it = knownEntities.iterator(); it.hasNext(); ) {
			RushEntity entity = it.next();
			
			if (entity.active && canSee(entity)) {
				Packet updatePacket = entity.createUpdateMessage();

				if (updatePacket != null)
					session.sendPacket(updatePacket);

			} else {
				session.sendPacket(new DestroyEntity(entity.id));
				it.remove();
			}
		}

		for (RushEntity entity : world.entities) {
			if (entity == this)
				continue;

			if (!knownEntities.contains(entity) && entity.active && canSee(entity)) {
				knownEntities.add(entity);
				session.sendPacket(entity.createSpawnMessage());
			}
		}
	}

	/**
	 * Streams chunks to the player's client.
	 */
	public void streamBlocks() {
		Set<ChunkCoords> previousChunks = new HashSet<>(knownChunks);

		int centralX = ((int) posX) / RushChunk.WIDTH;
		int centralZ = ((int) posZ) / RushChunk.HEIGHT;

		int viewDistance = 10;

		for (int x = (centralX - viewDistance); x <= (centralX + viewDistance); x++)
			for (int z = (centralZ - viewDistance); z <= (centralZ + viewDistance); z++) {
				ChunkCoords key = new ChunkCoords(x, z);

				if (!knownChunks.contains(key)) {
					knownChunks.add(key);
					session.sendPacket(world.getChunkFromChunkCoords(x, z).toPacket());
				}

				previousChunks.remove(key);
			}

		for (ChunkCoords key : previousChunks)
			//session.send(new PreChunkPacketImpl(key.x, key.z, false));
			knownChunks.remove(key);

		previousChunks.clear();
	}


	@Override
	public Packet createSpawnMessage() {
		return null;
	}

	@Override
	public Packet createUpdateMessage() {
		return null;
	}
	
	public void setCrouching(boolean crouching) {
		this.crouching = crouching;
		setMetadata(new MetaParam<Byte>(0, new Byte((byte) (crouching ? 0x02: 0))));
		// FIXME: other bits in the bitmask would be wiped out
		// TODO: update other clients, needs to be figured out
	}

	public void setSprinting(boolean sprinting) {
		this.sprinting = sprinting;
		setMetadata(new MetaParam<Byte>(0, new Byte((byte) (sprinting ? 0x08: 0))));
		// FIXME: other bits in the bitmask would be wiped out
	}

	public double getEyeHeight() {
		return crouching ? CROUCH_EYE_HEIGHT : NORMAL_EYE_HEIGHT;
	}
}

