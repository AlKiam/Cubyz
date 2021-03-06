package io.cubyz.world.generator;

import java.util.Random;

import org.joml.Vector3i;

import io.cubyz.blocks.Block;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.blocks.Ore;
import io.cubyz.world.Chunk;
import io.cubyz.world.Noise;
import io.cubyz.world.World;
import io.cubyz.world.cubyzgenerators.CaveGenerator;
import io.cubyz.world.cubyzgenerators.FancyGenerator;
import io.cubyz.world.cubyzgenerators.Generator;
import io.cubyz.world.cubyzgenerators.GrassGenerator;
import io.cubyz.world.cubyzgenerators.OreGenerator;
import io.cubyz.world.cubyzgenerators.TerrainGenerator;
import io.cubyz.world.cubyzgenerators.VegetationGenerator;

//TODO: Add more diversity
//TODO: Mod Access(Getting close!)

/**
 * Yep, Cubyz's world is called Lifeland
 */
public class LifelandGenerator extends WorldGenerator {
	// Ore Utilities
	public static Ore[] ores;
	public static void init(Ore[] ores) {
		LifelandGenerator.ores = ores;
	}
	

	FancyGenerator g1 = new TerrainGenerator();
	Generator g2 = new OreGenerator(ores);
	Generator g3 = new CaveGenerator();
	FancyGenerator g4 = new VegetationGenerator();
	FancyGenerator g5 = new GrassGenerator();
	@Override
	public void generate(Chunk ch, World world) {
		int ox = ch.getX();
		int oy = ch.getZ();
		int wx = ox << 4;
		int wy = oy << 4;
		int seed = world.getSeed();
		// Generate some maps:
		float[][] heightMap = Noise.generateMapFragment(wx-8, wy-8, 32, 32, 256, seed);
		float[][] heatMap = Noise.generateMapFragment(wx-8, wy-8, 32, 32, 4096, seed ^ 123456789);
		int[][] realHeight = new int[32][32];
		for(int px = 0; px < 32; px++) {
			for(int py = 0; py < 32; py++) {
				int h = (int)(heightMap[px][py]*world.getHeight());
				if(h > world.getHeight())
					h = world.getHeight();
				realHeight[px][py] = h;
				
				heatMap[px][py] = ((2 - heightMap[px][py] + TerrainGenerator.SEA_LEVEL/(float)world.getHeight())*heatMap[px][py]*120) - 100;
			}
		}
		
		// Go through all generators(TODO: make more general)
		Random r = new Random(seed);
		Block[][][] chunk = new Block[16][16][world.getHeight()];
		g1.generate(r.nextLong(), ox, oy, chunk, heatMap, realHeight);
		g2.generate(r.nextLong(), ox, oy, chunk);
		g3.generate(r.nextLong(), ox, oy, chunk);
		g4.generate(r.nextLong(), ox, oy, chunk, heatMap, realHeight);
		g5.generate(r.nextLong(), ox, oy, chunk, heatMap, realHeight);

		// Place the blocks in the chunk:
		for(int px = 0; px < 16; px++) {
			for(int py = 0; py < 16; py++) {
				for(int h = 0; h < world.getHeight(); h++) {
					Block b = chunk[px][py][h];
					if(b != null) {
						BlockInstance bi = new BlockInstance(b);
						bi.setPosition(new Vector3i(wx + px, h, wy + py));
						ch.rawAddBlock(px, h, py, bi);
						if(bi.getBlock() != null && bi.getBlock().hasBlockEntity()) {
							ch.blockEntities().put(bi, bi.getBlock().createBlockEntity(bi.getPosition()));
						}
					}
				}
			}
		}

		ch.applyBlockChanges();
	}
}
