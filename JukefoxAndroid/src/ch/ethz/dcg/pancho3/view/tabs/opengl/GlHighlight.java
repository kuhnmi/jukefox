/* 
 * Copyright 2008-2013, ETH Zürich, Samuel Welten, Michael Kuhn, Tobias Langner,
 * Sandro Affentranger, Lukas Bossard, Michael Grob, Rahul Jain, 
 * Dominic Langenegger, Sonia Mayor Alonso, Roger Odermatt, Tobias Schlueter,
 * Yannick Stucki, Sebastian Wendland, Samuel Zehnder, Samuel Zihlmann,       
 * Samuel Zweifel
 *
 * This file is part of Jukefox.
 *
 * Jukefox is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or any later version. Jukefox is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Jukefox. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ethz.dcg.pancho3.view.tabs.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import ch.ethz.dcg.pancho3.R;


public class GlHighlight {
	
	public static final String TAG = GlHighlight.class.getSimpleName();
	
	private final static int VERTS = 4;
	private final static float HIGHLIGHT_SIZE = 2f;	

    private FloatBuffer mFVertexBuffer;
    private FloatBuffer mTexBuffer;
    private ShortBuffer mIndexBuffer;
    private boolean horizontal = false;
    private boolean textureLoaded = false;
    private int textureId;
    private Context context;
    
    /**
     * Constructor
     * @param x xposition of the highlight
     * @param y y position of the highlight
     * @param z z position of the highlight
     * @param size size of the highlight
     */
    public GlHighlight(float x, float y, float z, boolean horizontal, Context context) {
    	
    	this.context = context;
    	this.horizontal = horizontal;
    	
        createBuffers();

        float coords[] = null;
        
        if (horizontal){
        	coords = getHorizontalCoords(x,y,0);
        } else {
        	coords = getVerticalCoords(x,y,0);
        }

        fillVertexBuffer(coords);            
        fillTextureBuffer();
        fillIndexBuffer();

        mFVertexBuffer.position(0);
        mTexBuffer.position(0);
        mIndexBuffer.position(0);
    }
    
    private float[] getVerticalCoords(float x, float y, float z) {
		
    	float[] coords = {
    		// x,y,z
    		x-HIGHLIGHT_SIZE, y-HIGHLIGHT_SIZE, z,
    		x+HIGHLIGHT_SIZE, y-HIGHLIGHT_SIZE, z,
            x-HIGHLIGHT_SIZE, y+HIGHLIGHT_SIZE, z,                                
            x+HIGHLIGHT_SIZE, y+HIGHLIGHT_SIZE, z
             
    	};
		
		return coords;
	}

	private float[] getHorizontalCoords(float x, float y, float z) {
		float[] coords = {
    		// x,y,z
    		x-2*HIGHLIGHT_SIZE, y, z+2*HIGHLIGHT_SIZE,
    		x+2*HIGHLIGHT_SIZE, y, z+2*HIGHLIGHT_SIZE,
            x-2*HIGHLIGHT_SIZE, y, z-2*HIGHLIGHT_SIZE,                                
            x+2*HIGHLIGHT_SIZE, y, z-2*HIGHLIGHT_SIZE
             
    	};
		return coords;
	}
    
    private void createBuffers() {
    	// Buffers to be passed to gl*Pointer() functions
        // must be direct, i.e., they must be placed on the
        // native heap where the garbage collector cannot
        // move them.
        //
        // Buffers with multi-byte datatypes (e.g., short, int, float)
        // must have their byte order set to native order

        ByteBuffer vbb = ByteBuffer.allocateDirect(VERTS * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        mFVertexBuffer = vbb.asFloatBuffer();

        ByteBuffer tbb = ByteBuffer.allocateDirect(VERTS * 2 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexBuffer = tbb.asFloatBuffer();

        ByteBuffer ibb = ByteBuffer.allocateDirect(VERTS * 2);
        ibb.order(ByteOrder.nativeOrder());
        mIndexBuffer = ibb.asShortBuffer();
	}
    
    private void fillTextureBuffer() {
		mTexBuffer.put(0f);
        mTexBuffer.put(1f);
        
        mTexBuffer.put(1f);
        mTexBuffer.put(1f);
        
        mTexBuffer.put(0f);
        mTexBuffer.put(0f);               
        
        mTexBuffer.put(1f);
        mTexBuffer.put(0f);
	}
	
	private void fillIndexBuffer() {
		for(int i = 0; i < VERTS; i++) {
            mIndexBuffer.put((short) i);
        }
	}
	
	private void fillVertexBuffer(float[] coords) {
		for (int i = 0; i < VERTS; i++) {
            for(int j = 0; j < 3; j++) {
                mFVertexBuffer.put(coords[i*3+j] * 1.0f);
            }
        }   
	}

	/**
     * Move the highlight to another location
     * @param x new x position
     * @param y new y position
     * @param z new z position
     * @param size new size
     */
    public void setCoords(float x, float y, float z) {
    	
    	float coords[] = null;
        
        if (horizontal){
        	coords = getHorizontalCoords(x,y,z);
        } else {
        	coords = getVerticalCoords(x,y,z);
        }
    	
    	for (int i = 0; i < VERTS; i++) {
            for(int j = 0; j < 3; j++) {
                mFVertexBuffer.put(i*3+j, coords[i*3+j] * 1.0f);
            }
        }
    }

    /**
     * draw the highlight
     * @param gl the OpenGl object
     */
    public void draw(GL10 gl, int tid) {
    	
    	if (!textureLoaded) {
    		loadTexture(gl);
    	}
    	gl.glEnable(GL10.GL_TEXTURE_2D);
    	gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuffer);
    	gl.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
        gl.glFrontFace(GL10.GL_CCW);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFVertexBuffer);                
        gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, VERTS,GL10.GL_UNSIGNED_SHORT, mIndexBuffer);
    }        
    
    private void loadTexture(GL10 gl) {    	
    	int[] tempTextureId = new int[1];
    	gl.glGenTextures(1, tempTextureId, 0);
    	this.textureId = tempTextureId[0];
		gl.glBindTexture(GL10.GL_TEXTURE_2D, this.textureId);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
				GL10.GL_REPEAT);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
				GL10.GL_REPEAT);
		gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
				GL10.GL_REPLACE);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, BitmapFactory.decodeResource(
				context.getResources(), R.drawable.d045_highlight_noscale), 0);  
		textureLoaded = true;
	}

	/**
     * Flip the highlight for the overview mode and back
     */
    public void flipHighlight() {
    	if (!horizontal) {    		
    		
    		float posX = mFVertexBuffer.get(0) + HIGHLIGHT_SIZE;
    		float posY = mFVertexBuffer.get(1) + HIGHLIGHT_SIZE;
    		float posZ = mFVertexBuffer.get(2);    		
    		
    		mFVertexBuffer.put(0, posX - 2*HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(1, posY);
    		mFVertexBuffer.put(2, posZ + 2*HIGHLIGHT_SIZE);
    		
    		mFVertexBuffer.put(3, posX + 2*HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(4, posY);
    		mFVertexBuffer.put(5, posZ + 2*HIGHLIGHT_SIZE);

    		mFVertexBuffer.put(6, posX - 2*HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(7, posY);
    		mFVertexBuffer.put(8, posZ - 2*HIGHLIGHT_SIZE);

    		mFVertexBuffer.put(9, posX + 2*HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(10, posY);
    		mFVertexBuffer.put(11, posZ - 2*HIGHLIGHT_SIZE);
    		
    		horizontal = true;

    	} else {    		    		
    		
    		float posX = mFVertexBuffer.get(0) + 2*HIGHLIGHT_SIZE;
    		float posY = mFVertexBuffer.get(1);
    		float posZ = mFVertexBuffer.get(2) - 2*HIGHLIGHT_SIZE;
    		
    		mFVertexBuffer.put(0, posX - HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(1, posY - HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(2, posZ);
    		
    		mFVertexBuffer.put(3, posX + HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(4, posY - HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(5, posZ);

    		mFVertexBuffer.put(6, posX - HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(7, posY + HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(8, posZ);

    		mFVertexBuffer.put(9, posX + HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(10, posY + HIGHLIGHT_SIZE);
    		mFVertexBuffer.put(11, posZ);
    		
    		horizontal = false;
    	}
    }
    
    public boolean isHorizontal() {
    	return horizontal;
    }
}
