package io.cubyz.client;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import io.cubyz.blocks.Block;
import io.cubyz.blocks.BlockInstance;
import io.cubyz.entity.Entity;
import io.cubyz.entity.Player;
import io.cubyz.math.Vector3fi;
import io.cubyz.world.Chunk;
import io.jungle.InstancedMesh;
import io.jungle.Mesh;
import io.jungle.Spatial;
import io.jungle.Window;
import io.jungle.game.Context;
import io.jungle.renderers.FrustumCullingFilter;
import io.jungle.renderers.IRenderer;
import io.jungle.renderers.Transformation;
import io.jungle.util.DirectionalLight;
import io.jungle.util.PointLight;
import io.jungle.util.ShaderProgram;
import io.jungle.util.SpotLight;
import io.jungle.util.Utils;

@SuppressWarnings("unchecked")
public class MainRenderer implements IRenderer {

	private ShaderProgram shaderProgram;

	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 1000.0f;
	private boolean inited = false;
	private boolean doRender = true;
	public boolean orthogonal;
	private Transformation transformation;
	private String shaders = "";
	private FrustumCullingFilter filter;
	private Matrix4f prjViewMatrix = new Matrix4f();
	private FrustumIntersection frustumInt = new FrustumIntersection();

	public static final int MAX_POINT_LIGHTS = 0;
	public static final int MAX_SPOT_LIGHTS = 0;
	private float specularPower = 16f;

	public MainRenderer() {

	}

	public Transformation getTransformation() {
		return transformation;
	}

	public void setShaderFolder(String shaders) {
		this.shaders = shaders;
	}

	public void unloadShaders() throws Exception {
		shaderProgram.unbind();
		shaderProgram.cleanup();
		shaderProgram = null;
		System.gc();
	}

	public void setDoRender(boolean doRender) {
		this.doRender = doRender;
	}

	public void loadShaders() throws Exception {
		shaderProgram = new ShaderProgram();
		shaderProgram.createVertexShader(Utils.loadResource(shaders + "/vertex.vs"));
		shaderProgram.createFragmentShader(Utils.loadResource(shaders + "/fragment.fs"));
		shaderProgram.link();
		shaderProgram.createUniform("projectionMatrix");
		shaderProgram.createUniform("modelViewNonInstancedMatrix");
		shaderProgram.createUniform("viewMatrixInstanced");
		shaderProgram.createUniform("texture_sampler");
		shaderProgram.createUniform("ambientLight");
		//shaderProgram.createUniform("selectedInstanced");
		shaderProgram.createUniform("selectedNonInstanced");
		shaderProgram.createUniform("specularPower");
		shaderProgram.createUniform("isInstanced");
		shaderProgram.createMaterialUniform("material");
		shaderProgram.createPointLightListUniform("pointLights", MAX_POINT_LIGHTS);
		shaderProgram.createSpotLightListUniform("spotLights", MAX_SPOT_LIGHTS);
		shaderProgram.createDirectionalLightUniform("directionalLight");
		shaderProgram.createFogUniform("fog");
		System.gc();
	}

	@Override
	public void init(Window window) throws Exception {
		transformation = new Transformation();
		window.setProjectionMatrix(transformation.getProjectionMatrix((float) Math.toRadians(70.0f), window.getWidth(),
				window.getHeight(), Z_NEAR, Z_FAR));
		loadShaders();

		filter = new FrustumCullingFilter();

		inited = true;
	}

	public void clear() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
	}
	//long t = 0;
	//int n = 1;

	Vector3f lastInstancedPosition = new Vector3f();
	List<Spatial>[] map = (List<Spatial>[]) new List[0];
	public synchronized void render(Window window, Context ctx, Vector3f ambientLight, DirectionalLight directionalLight,
			Chunk[] chunks, Block[] blocks, Entity[] entities, Player localPlayer) {
		//long t1 = System.nanoTime();
		if (window.isResized()) {
			glViewport(0, 0, window.getWidth(), window.getHeight());
			window.setResized(false);
			
			if (orthogonal) {
				window.setProjectionMatrix(transformation.getOrthoProjectionMatrix(1f, -1f, -1f, 1f, Z_NEAR, Z_FAR));
			} else {
				window.setProjectionMatrix(transformation.getProjectionMatrix(ctx.getCamera().getFov(), window.getWidth(),
						window.getHeight(), Z_NEAR, Z_FAR));
			}
		}
		if (!doRender)
			return;
		clear();
		ctx.getCamera().setViewMatrix(transformation.getViewMatrix(ctx.getCamera()));
		List<InstancedMesh> instancedMeshes;
		
		Spatial selected = null;
		int selectedBlock = -1;
		if (blocks.length != map.length) {
			map = (List<Spatial>[]) new List[blocks.length];
		}
		
		for (int i = 0; i < map.length; i++) {
			if (map[i] == null) {
				map[i] = new ArrayList<Spatial>();
			} else {
				map[i] = new ArrayList<Spatial>();
			}
		}
		// Uses FrustumCulling on the chunks.
		prjViewMatrix.set(window.getProjectionMatrix());
		prjViewMatrix.mul(ctx.getCamera().getViewMatrix());
		frustumInt.set(prjViewMatrix);
		float blockMeshBoundingRadius = 2.0f; // Is always the same for all blocks.
		if(localPlayer != null) {
			// Store the position locally to prevent glitches when the updateThread changes the position.
			Vector3fi pos = localPlayer.getPosition();
			int x0 = pos.x;
			float relX = pos.relX;
			int z0 = pos.z;
			float relZ = pos.relZ;
			for (Chunk ch : chunks) {
				if (!frustumInt.testAab(ch.getMin(localPlayer), ch.getMax(localPlayer)))
					continue;
				BlockInstance[] vis = ch.getVisibles();
				for (int i = 0; vis[i] != null; i++) {
					Spatial tmp = (Spatial) vis[i].getSpatial();
					float boundingRadius = tmp.getScale() * blockMeshBoundingRadius;
					float x = (vis[i].getX() - x0) - relX;
					float y = vis[i].getY();
					float z = (vis[i].getZ() - z0) - relZ;
					// Do the frustum culling directly here instead of looping 3 times through the data which in the end isn't drawn.
					if(frustumInt.testSphere(x, y, z, boundingRadius)) {
						tmp.setPosition(x, y, z);
						if (tmp.isSelected()) {
							selected = tmp;
							selectedBlock = vis[i].getID();
							continue;
						}
						map[vis[i].getID()].add(tmp);
					}
				}
			}
		}
		//filter.updateFrustum(window.getProjectionMatrix(), ctx.getCamera().getViewMatrix());
		instancedMeshes = new ArrayList<>();
		HashMap<Mesh, List<Spatial>> m = new HashMap<>();
		for (int i = 0; i < blocks.length; i++) {
			if (map[i].size() == 0)
				continue;
			m.put(Meshes.blockMeshes.get(blocks[i]), map[i]);
		}
		for (Mesh mesh : m.keySet()) {
			if (mesh instanceof InstancedMesh) {
				instancedMeshes.add((InstancedMesh) mesh);
			}
		}
		//filter.filter(m);
		renderScene(ctx, ambientLight, null /* point light */, null /* spot light */, directionalLight, map, blocks, entities,
				localPlayer, selected, selectedBlock);
		if (ctx.getHud() != null) {
			ctx.getHud().render(window);
		}
		
		//long t2 = System.nanoTime(); if(t2-t1 > 1000000) { t += t2-t1; n++;
		//System.out.println(t/n); }
		 
	}
	
	public void renderScene(Context ctx, Vector3f ambientLight, PointLight[] pointLightList, SpotLight[] spotLightList,
			DirectionalLight directionalLight, List<Spatial>[] map, Block[] blocks, Entity[] entities, Player p, Spatial selected,
			int selectedBlock) {
		shaderProgram.bind();
		
		shaderProgram.setUniform("fog", ctx.getFog());
		shaderProgram.setUniform("projectionMatrix", ctx.getWindow().getProjectionMatrix());
		shaderProgram.setUniform("texture_sampler", 0);
		Matrix4f viewMatrix = ctx.getCamera().getViewMatrix();
		if (p != null) {
			// for non-chunked instanced rendering
			//Vector3fi pp = p.getPosition();
			//Vector3f l = lastInstancedPosition;
			//shaderProgram.setUniform("viewMatrixInstanced", viewMatrix.translate(-(pp.x()-l.x), 0, -(pp.z()-l.z)));
			
			// for chunked
			shaderProgram.setUniform("viewMatrixInstanced", viewMatrix);
		}
		
		renderLights(viewMatrix, ambientLight, pointLightList, spotLightList, directionalLight);
		for (int i = 0; i < blocks.length; i++) {
			if (map[i] == null)
				continue;
			Mesh mesh = Meshes.blockMeshes.get(blocks[i]);
			shaderProgram.setUniform("material", mesh.getMaterial());
			if (selectedBlock == i) {
				map[i].add(selected);
			}
			if (mesh.isInstanced()) {
				InstancedMesh ins = (InstancedMesh) mesh;
				shaderProgram.setUniform("isInstanced", 1);
				/*
				if (ins.renderListInstancedNC(map[i], transformation)) {
					lastInstancedPosition = p.getPosition().toVector3f();
				}
				*/
				ins.renderListInstanced(map[i], transformation);
				shaderProgram.setUniform("isInstanced", 0);
			} else {
				mesh.renderList(map[i], (Spatial gameItem) -> {
					Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
					if (orthogonal) {
						modelViewMatrix = transformation.getOrtoProjModelMatrix(gameItem, viewMatrix);
					}
					if (gameItem.isSelected())
						shaderProgram.setUniform("selectedNonInstanced", 1f);
					shaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
					return true;
				});
				if (selectedBlock == i) {
					shaderProgram.setUniform("selectedNonInstanced", 0f);
				}
			}
		}
		for (int i = 0; i < entities.length; i++) {
			Entity ent = entities[i];
			if (ent != null && ent != p) { // don't render local player
				Mesh mesh = (Mesh) ent.getRenderablePair().get("meshCache");
				shaderProgram.setUniform("material", mesh.getMaterial());
				mesh.renderList(map[i], (Spatial gameItem) -> {
					if (gameItem.isInFrustum()) {
						Matrix4f modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
						if (orthogonal) {
							modelViewMatrix = transformation.getOrtoProjModelMatrix(gameItem, viewMatrix);
						}
						shaderProgram.setUniform("modelViewNonInstancedMatrix", modelViewMatrix);
						return true;
					}
					return false;
				});
			}
		}
		
		shaderProgram.unbind();
	}

	private void renderLights(Matrix4f viewMatrix, Vector3f ambientLight, PointLight[] pointLightList,
			SpotLight[] spotLightList, DirectionalLight directionalLight) {

		shaderProgram.setUniform("ambientLight", ambientLight);
		shaderProgram.setUniform("specularPower", specularPower);

		// Process Point Lights
		int numLights = pointLightList != null ? pointLightList.length : 0;
		for (int i = 0; i < numLights; i++) {
			// Get a copy of the point light object and transform its position to view
			// coordinates
			PointLight currPointLight = new PointLight(pointLightList[i]);
			Vector3f lightPos = currPointLight.getPosition();
			Vector4f aux = new Vector4f(lightPos, 1);
			aux.mul(viewMatrix);
			lightPos.x = aux.x;
			lightPos.y = aux.y;
			lightPos.z = aux.z;
			shaderProgram.setUniform("pointLights", currPointLight, i);
		}

		// Process Spot Ligths
		numLights = spotLightList != null ? spotLightList.length : 0;
		for (int i = 0; i < numLights; i++) {
			// Get a copy of the spot light object and transform its position and cone
			// direction to view coordinates
			SpotLight currSpotLight = new SpotLight(spotLightList[i]);
			Vector4f dir = new Vector4f(currSpotLight.getConeDirection(), 0);
			dir.mul(viewMatrix);
			currSpotLight.setConeDirection(new Vector3f(dir.x, dir.y, dir.z));
			Vector3f lightPos = currSpotLight.getPointLight().getPosition();

			Vector4f aux = new Vector4f(lightPos, 1);
			aux.mul(viewMatrix);
			lightPos.x = aux.x;
			lightPos.y = aux.y;
			lightPos.z = aux.z;

			shaderProgram.setUniform("spotLights", currSpotLight, i);
		}

		// Get a copy of the directional light object and transform its position to view
		// coordinates
		DirectionalLight currDirLight = new DirectionalLight(directionalLight);
		Vector4f dir = new Vector4f(currDirLight.getDirection(), 0);
		dir.mul(viewMatrix);
		currDirLight.setDirection(new Vector3f(dir.x, dir.y, dir.z));
		shaderProgram.setUniform("directionalLight", currDirLight);

	}

	@Override
	public void cleanup() {
		if (shaderProgram != null) {
			shaderProgram.cleanup();
		}
	}

	@Override
	public void render(Window win, Context ctx, Vector3f ambientLight, PointLight[] pointLightList,
			SpotLight[] spotLightList, DirectionalLight directionalLight) {
		throw new UnsupportedOperationException("Cubyz Renderer doesn't support this method.");
	}

	@Override
	public void setPath(String dataName, String path) {
		if (dataName.equals("shaders") || dataName.equals("shadersFolder")) {
			if (inited) {
				try {
					doRender = false;
					unloadShaders();
					shaders = path;
					loadShaders();
					doRender = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				shaders = path;
			}
		}
	}

}
