package io.jungle;

import java.nio.FloatBuffer;
import java.util.List;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;
import org.lwjgl.system.MemoryUtil;

import io.jungle.renderers.Transformation;

public class InstancedMesh extends Mesh {

	private static final int FLOAT_SIZE_BYTES = 4;

	private static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;

	private static final int MATRIX_SIZE_FLOATS = 4 * 4;

	private static final int MATRIX_SIZE_BYTES = MATRIX_SIZE_FLOATS * FLOAT_SIZE_BYTES;

	private static final int INSTANCE_SIZE_BYTES = MATRIX_SIZE_BYTES + FLOAT_SIZE_BYTES;

	private static final int INSTANCE_SIZE_FLOATS = MATRIX_SIZE_FLOATS + 1;

	private int numInstances;

	private final int modelViewVBO;

	private FloatBuffer instanceDataBuffer;

	public boolean isInstanced() {
		return true;
		// XXX
		//return false;
	}
	
	public InstancedMesh(int vao, int count, List<Integer> vaoIds, int numInstances) {
		super(vao, count, vaoIds);
		glBindVertexArray(vaoId);
		modelViewVBO = glGenBuffers();
		initInstances(numInstances);
	}
	
	protected void initInstances(int numInstances) {
		this.numInstances = numInstances;
		vboIdList.add(modelViewVBO);
		instanceDataBuffer = MemoryUtil.memAllocFloat(numInstances * INSTANCE_SIZE_FLOATS);
		glBindBuffer(GL_ARRAY_BUFFER, modelViewVBO);
		int start = 3;
		int strideStart = 0;
		for (int i = 0; i < 4; i++) {
			glVertexAttribPointer(start, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
			glVertexAttribDivisor(start, 1);
			start++;
			strideStart += VECTOR4F_SIZE_BYTES;
		}
		glVertexAttribPointer(start, 1, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
		glVertexAttribDivisor(start, 1);
		start++;
		strideStart += FLOAT_SIZE_BYTES;

		// Light view matrix
		/*
		for (int i = 0; i < 4; i++) {
			glVertexAttribPointer(start, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
			glVertexAttribDivisor(start, 1);
			start++;
			strideStart += VECTOR4F_SIZE_BYTES;
		}
		*/

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}
	
	private float[] positions, textCoords, normals;
	private int[] indices;
	
	public InstancedMesh(float[] positions, float[] textCoords, float[] normals, int[] indices, int numInstances) {
		super(positions, textCoords, normals, indices);
		this.positions = positions;
		this.textCoords = textCoords;
		this.normals = normals;
		this.indices = indices;
		glBindVertexArray(vaoId);
		modelViewVBO = glGenBuffers();
		initInstances(numInstances);
	}
	
	public int getInstances() {
		return numInstances;
	}
	
	public void setInstances(int numInst) {
		this.numInstances = numInst;
		glBindVertexArray(vaoId);
		instanceDataBuffer = MemoryUtil.memRealloc(instanceDataBuffer, numInstances * INSTANCE_SIZE_FLOATS);
		glBindBuffer(GL_ARRAY_BUFFER, modelViewVBO);
		int start = 3;
		int strideStart = 0;
		for (int i = 0; i < 4; i++) {
			glVertexAttribPointer(start, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
			glVertexAttribDivisor(start, 1);
			start++;
			strideStart += VECTOR4F_SIZE_BYTES;
		}
		glVertexAttribPointer(start, 1, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
		glVertexAttribDivisor(start, 1);
		start++;
		strideStart += FLOAT_SIZE_BYTES;

		// Light view matrix
		/*
		for (int i = 0; i < 4; i++) {
			glVertexAttribPointer(start, 4, GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
			glVertexAttribDivisor(start, 1);
			start++;
			strideStart += VECTOR4F_SIZE_BYTES;
		}*/

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	}

	public Mesh cloneNoMaterial() {
		Mesh clone = new InstancedMesh(positions, textCoords, normals, indices, 0);
		clone.boundingRadius = boundingRadius;
		clone.cullFace = cullFace;
		clone.frustum = frustum;
		return clone;
	}
	
	@Override
	public void cleanUp() {
		super.cleanUp();
		if (this.instanceDataBuffer != null) {
			MemoryUtil.memFree(this.instanceDataBuffer);
			this.instanceDataBuffer = null;
		}
	}

	@Override
	protected void initRender() {
		super.initRender();
		int start = 3;
		int numElements = 4 + 1;
		for (int i = 0; i < numElements; i++) {
			glEnableVertexAttribArray(start + i);
		}
		glBindBuffer(GL_ARRAY_BUFFER, modelViewVBO);
	}

	@Override
	protected void endRender() {
		int start = 3;
		int numElements = 4 + 1;
		for (int i = 0; i < numElements; i++) {
			glDisableVertexAttribArray(start + i);
		}
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		super.endRender();
	}

	int oldSize = 0;
	
	/**
	 * Render list instanced in a non-chunked way
	 * @param spatials
	 * @param transformation
	 * @param viewMatrix
	 */
	public boolean renderListInstancedNC(List<Spatial> spatials, Transformation transformation) {
		if (numInstances == 0)
			return false;
		initRender();
		boolean bool = false;
		int curSize = spatials.size();
		if (curSize != oldSize) {
			oldSize = curSize;
			if (numInstances < curSize) {
				setInstances(curSize);
			}
			uploadData(spatials, transformation);
			bool = true;
		}
		renderChunkInstanced(spatials, transformation);
		
		endRender();
		return bool;
	}
	
	public void renderListInstanced(List<Spatial> spatials, Transformation transformation) {
		if (numInstances == 0)
			return;
		initRender();

		int chunkSize = numInstances;
		int length = spatials.size();
		for (int i = 0; i < length; i += chunkSize) {
			int end = Math.min(length, i + chunkSize);
			List<Spatial> subList = spatials.subList(i, end);
			uploadData(subList, transformation);
			renderChunkInstanced(subList, transformation);
		}

		endRender();
	}
	
	public void uploadData(List<Spatial> gameItems, Transformation transformation) {
		this.instanceDataBuffer.clear();
		int i = 0;
		for (Spatial gameItem : gameItems) {
			Matrix4f modelMatrix = transformation.getModelMatrix(gameItem);
			modelMatrix.get(INSTANCE_SIZE_FLOATS * i, instanceDataBuffer);
			instanceDataBuffer.put(INSTANCE_SIZE_FLOATS * i + 16, gameItem.isSelected() ? 1 : 0);
			i++;
		}
		
		glBufferData(GL_ARRAY_BUFFER, instanceDataBuffer, GL_DYNAMIC_DRAW);
	}
	
	private void renderChunkInstanced(List<Spatial> spatials, Transformation transformation) {
		glDrawElementsInstanced(GL_TRIANGLES, getVertexCount(), GL_UNSIGNED_INT, 0, spatials.size());
	}
}
