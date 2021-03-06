package io.cubyz.world.cubyzgenerators;

import io.cubyz.api.CubyzRegistries;
import io.cubyz.api.Registry;
import io.cubyz.blocks.Block;

// Just a simple Generator to make sure there is grass/snow on top of every dirt block, even in a cave.

public class GrassGenerator implements FancyGenerator {
	private static Registry<Block> br = CubyzRegistries.BLOCK_REGISTRY; // shortcut to BLOCK_REGISTRY
	private static Block grass = br.getByID("cubyz:grass");
	private static Block snow = br.getByID("cubyz:snow");
	private static Block dirt = br.getByID("cubyz:dirt");
	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 262144; // Comes somewhere after almost everything.
	}

	@Override
	public void generate(long seed, int cx, int cy, Block[][][] chunk, float[][] heatMap, int[][] heightMap) {
		for(int px = 0; px < 16; px++) {
			for(int py = 0; py < 16; py++) {
				int height = heightMap[px+8][py+8];
				if(chunk[px][py][height] == null) {
					// Find the lowest non-empty terrain block:
					for(;height >= 0 && chunk[px][py][height] == null; height--) {}
					if(chunk[px][py][height] == dirt) {
						float temperature = heatMap[px+8][py+8];
						if(temperature > 0) {
							chunk[px][py][height] = grass;
						} else {
							chunk[px][py][height] = snow;
						}
					}
				}
			}
		}
	}

}
