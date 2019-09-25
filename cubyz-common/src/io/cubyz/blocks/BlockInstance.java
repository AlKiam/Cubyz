package io.cubyz.blocks;

import org.joml.Vector3i;

import io.cubyz.ClientOnly;
import io.cubyz.items.Inventory;
import io.cubyz.world.Chunk;
import io.cubyz.world.World;

public class BlockInstance {

	private Block block;
	private IBlockSpatial spatial;
	private Vector3i pos;
	private World world;
	private Inventory blockInventory = null;
	
	public World getWorld() {
		return world;
	}
	
	public void setWorld(World world) {
		this.world = world;
	}
	
	public BlockInstance(Block block) {
		this.block = block;
		if(block != null && block.inventorySize != 0) {
			blockInventory = new Inventory(block.inventorySize);
		}
	}
	
	public int getID() {
		return block.ID;
	}
	
	public Vector3i getPosition() {
		return pos;
	}
	
	public int getX() {
		return pos.x;
	}
	
	public int getY() {
		return pos.y;
	}
	
	public int getZ() {
		return pos.z;
	}
	
	public Object getMesh() {
		if (block.getBlockPair().get("textureCache") == null) {
			ClientOnly.createBlockMesh.accept(this.getBlock());
		}
		return block.getBlockPair().get("meshCache");
	}
	
	public Block getBlock() {
		return block;
	}
	
	public void setBlock(Block b) {
		block = b;
		if(block.inventorySize != 0) {
			blockInventory = new Inventory(block.inventorySize);
		} else {
			blockInventory = null;
		}
	}
	
	public void setPosition(Vector3i pos) {
		this.pos = pos;
	}
	
	public Inventory getInventory() {
		return blockInventory;
	}
	

	
	public BlockInstance[] getNeighbors(Chunk ch) {
		BlockInstance[] inst = new BlockInstance[6];
		// 0 = EAST  (x - 1)
		// 1 = WEST  (x + 1)
		// 2 = NORTH (z + 1)
		// 3 = SOUTH (z - 1)
		// 4 = DOWN
		// 5 = UP
		inst[5] = ch.getBlockInstanceAt(pos.x & 15, pos.y + 1, pos.z & 15);
		inst[4] = ch.getBlockInstanceAt(pos.x & 15, pos.y - 1, pos.z & 15);
		if((pos.z & 15) != 0)
			inst[3] = ch.getBlockInstanceAt(pos.x & 15, pos.y, (pos.z - 1) & 15);
		else
			inst[3] = world.getBlock(pos.x, pos.y, pos.z - 1);
		if((pos.z & 15) != 15)
			inst[2] = ch.getBlockInstanceAt(pos.x & 15, pos.y, (pos.z + 1) & 15);
		else
			inst[2] = world.getBlock(pos.x, pos.y, pos.z + 1);
		if((pos.x & 15) != 15)
			inst[1] = ch.getBlockInstanceAt((pos.x + 1) & 15, pos.y, pos.z & 15);
		else
			inst[1] = world.getBlock(pos.x + 1, pos.y, pos.z);
		if((pos.x & 15) != 0)
			inst[0] = ch.getBlockInstanceAt((pos.x - 1) & 15, pos.y, pos.z & 15);
		else
			inst[0] = world.getBlock(pos.x - 1, pos.y, pos.z);
		return inst;
	}
	
	public BlockInstance getNeighbor(int i, Chunk ch) {
		// 0 = EAST  (x - 1)
		// 1 = WEST  (x + 1)
		// 2 = NORTH (z + 1)
		// 3 = SOUTH (z - 1)
		// 4 = DOWN
		// 5 = UP
		switch(i) {
			case 5:
				return ch.getBlockInstanceAt(pos.x & 15, pos.y + 1, pos.z & 15);
			case 4:
				return ch.getBlockInstanceAt(pos.x & 15, pos.y - 1, pos.z & 15);
			case 3:
				if((pos.z & 15) != 0)
					return ch.getBlockInstanceAt(pos.x & 15, pos.y, (pos.z - 1) & 15);
				return world.getBlock(pos.x, pos.y, pos.z - 1);
			case 2:
				if((pos.z & 15) != 15)
					return world.getBlock(pos.x & 15, pos.y, (pos.z - 1) & 15);
				return world.getBlock(pos.x, pos.y, pos.z + 1);
			case 1:
				if((pos.x & 15) != 0)
					return world.getBlock((pos.x + 1) & 15, pos.y, pos.z & 15);
				return world.getBlock(pos.x + 1, pos.y, pos.z);
			case 0:
				if((pos.x & 15) != 0)
					return ch.getBlockInstanceAt((pos.x - 1) & 15, pos.y, pos.z & 15);
				return world.getBlock(pos.x - 1, pos.y, pos.z);
		}
		return null;
	}
	
	public IBlockSpatial getSpatial() {
		if (spatial == null) {
			spatial = ClientOnly.createBlockSpatial.apply(this);
		}
		return spatial;
	}
	
}
