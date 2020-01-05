/*
 * Name: E M V Naga Karthik
 * McGill ID: 260906923
 */

package comp557.a3;

import java.awt.Point;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * Class for organizing GLSL programs and mesh data for picking triangles and 
 * likewise vertices based on barycentric coordinates (unused in this assignment).
 * 
 * Picking is done by drawing every triangle with a different colour, and then checking
 * the colour at the selected location in the frame buffer (i.e., where the mouse was clicked).
 * Once the triangle is identified, it is drawn again without depth culling, with the 
 * corners drawn red, green, and blue, and with smooth interpolation of colours in between.
 * The barycentric coordinates of the selected point in the triangle are computed by reading 
 * the colour value.
 * 
 * Note the intial face selection requires the background to be drawn white (i.e., in a colour 
 * different than the IDs of the triangles).
 * 
 * @author kry
 */
public class MeshDrawPicking {
	
	private HEDS heds;
	private int nFaces;	
	
	private int pickingVertexBOID;
	private int pickingFaceIDBOID;
	private int pickingTriangleIndexBOID;
	private FloatBuffer pickingVertexB;
	private ByteBuffer pickingFaceIDBuffer;	
	private IntBuffer pickingTriangleIndexBuffer;
	private int pickingVertexBOStrideBytes;
	private int pickingFaceIDBOStrideBytes;
	
	private ShaderState state = new ShaderState();
	
	private int uProjectionID;
	private int uModelviewID;

	private int attribVertexID;
	private int attribColorID;
	
	/** 
	 * Initializes the GLSL shader (the mesh must also be set before picking)
	 * @param gl
	 */
	public void init( GL2 gl ) {
        String shaderName = "picking";
        ShaderCode vsCode = ShaderCode.create( gl, GL2.GL_VERTEX_SHADER, this.getClass(), "shaders", "shader/bin", shaderName, false );
        ShaderCode fsCode = ShaderCode.create( gl, GL2.GL_FRAGMENT_SHADER, this.getClass(), "shaders", "shader/bin", shaderName, false );	
        ShaderProgram program = new ShaderProgram();
        program.add( vsCode );
        program.add( fsCode );
		if ( !program.link(gl, System.err) ) {
			throw new GLException("Couldn't link program: " + program );
		}	
		state.attachShaderProgram( gl, program, false );
		state.useProgram( gl, true );		
    	uProjectionID = state.getUniformLocation(gl, "projection");
    	uModelviewID = state.getUniformLocation(gl, "modelview");
		attribVertexID = state.getAttribLocation( gl, "vertex" );
		attribColorID = state.getAttribLocation( gl, "color" );
		state.useProgram( gl, false );		
	}
	
	/**
	 * Sets the mesh, allocates and sets VBOs for picking 
	 * @param gl
	 * @param heds
	 */
	public void setMesh( GL2 gl, HEDS heds ) {
		this.heds = heds;
		nFaces = heds.faces.size();
				
		final int[] tmp = new int[3];
		gl.glGenBuffers( 3, tmp, 0 );
    	pickingVertexBOID = tmp[0];
    	pickingFaceIDBOID = tmp[1];
		pickingTriangleIndexBOID = tmp[2];
		
		pickingVertexB = Buffers.newDirectFloatBuffer( 3 * nFaces * 3 );
		pickingVertexBOStrideBytes = 3 * Buffers.SIZEOF_FLOAT;		
		pickingFaceIDBuffer = Buffers.newDirectByteBuffer( 3 * nFaces * 3 );
		pickingFaceIDBOStrideBytes = 3 * Buffers.SIZEOF_BYTE;
		pickingTriangleIndexBuffer = Buffers.newDirectIntBuffer( nFaces * 3 );

		fillPickingBuffers();

		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pickingVertexBOID );		
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, 3 * nFaces * 3 * Buffers.SIZEOF_FLOAT, pickingVertexB, GL2.GL_STATIC_DRAW );
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pickingFaceIDBOID );		
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, 3 * nFaces * 3 * Buffers.SIZEOF_BYTE, pickingFaceIDBuffer, GL2.GL_STATIC_DRAW );
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pickingTriangleIndexBOID );		
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, nFaces * 3 * Buffers.SIZEOF_INT, pickingTriangleIndexBuffer, GL2.GL_STATIC_DRAW );
	}
	
	/**
	 * Sets up the picking buffers by creating vertex data for each triangle with
	 * independent colours set for each vertex.
	 */
    public void fillPickingBuffers( ) {
    	pickingVertexB.rewind();
    	pickingFaceIDBuffer.rewind();
    	pickingTriangleIndexBuffer.rewind();
    	int faceIndex = 0; // will be converted into a colour
    	for ( Face f : heds.faces ) {
    		byte r = (byte) (faceIndex & 0xff);
    		byte g = (byte) ((faceIndex >> 8) & 0xff);
    		byte b = (byte) ((faceIndex >> 16) & 0xff);
    		HalfEdge he = f.he;
    		int i = 0;
    		do {
    			Vertex v = he.head;
    			pickingVertexB.put( (float) v.p.x );
    			pickingVertexB.put( (float) v.p.y );
    			pickingVertexB.put( (float) v.p.z );    		
    			pickingFaceIDBuffer.put( r );
    			pickingFaceIDBuffer.put( g );
    			pickingFaceIDBuffer.put( b );
    			pickingTriangleIndexBuffer.put( faceIndex * 3 + i );
    			i++;
    			he = he.next;
    		} while ( he != f.he );
    		faceIndex++;
    	}    
    	pickingVertexB.rewind();
    	pickingFaceIDBuffer.rewind();
    	pickingTriangleIndexBuffer.rewind();			
    }
    
    /**
	 * Draws for picking with a unique colour for every triangle
     * @param gl
     * @param p
     * @param BCC
     * @return
     */
    public Face pickTriangles( GLAutoDrawable drawable, Point p, Vector3d BCC ) {
        GL2 gl = drawable.getGL().getGL2();
    	gl.glClearColor(1, 1, 1, 1);
    	gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

        state.useProgram( gl, true );

		final float[] P = new float[16];
		final float[] V = new float[16];                
		gl.glGetFloatv( GL2.GL_PROJECTION_MATRIX, P, 0 );
		gl.glGetFloatv( GL2.GL_MODELVIEW_MATRIX, V, 0 );
		gl.glUniformMatrix4fv( uProjectionID, 1, false, P, 0);
		gl.glUniformMatrix4fv( uModelviewID, 1, false, V, 0);

		gl.glEnableVertexAttribArray( attribVertexID );
		gl.glEnableVertexAttribArray( attribColorID );
		
        gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pickingVertexBOID );		
		gl.glVertexAttribPointer( attribVertexID, 3, GL.GL_FLOAT, false, pickingVertexBOStrideBytes, 0 );
		
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pickingFaceIDBOID );		
		gl.glVertexAttribPointer( attribColorID, 3, GL.GL_UNSIGNED_BYTE, false, pickingFaceIDBOStrideBytes, 0 );
		
		gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, pickingTriangleIndexBOID );
		gl.glDrawElements( GL2.GL_TRIANGLES, pickingTriangleIndexBuffer.capacity(), GL2.GL_UNSIGNED_INT, 0 );

		gl.glDisableVertexAttribArray( attribVertexID );
		gl.glDisableVertexAttribArray( attribColorID );

        state.useProgram( gl, false );

    	final ByteBuffer colorPixels = ByteBuffer.allocate( 4 );
    	
        if ( p != null ) {
    		int x = p.x;
    		int y = drawable.getSurfaceHeight() - p.y;
    		colorPixels.rewind();
    		gl.glReadPixels( x, y, 1, 1, GL2.GL_RGB, GL.GL_UNSIGNED_BYTE, colorPixels );
    		colorPixels.rewind();
    		int r, g, b;
    		r = colorPixels.get() & 0xff;
    		g = colorPixels.get() & 0xff;
    		b = colorPixels.get() & 0xff;
    		int ID = r + (g << 8) + (b << 16);
        
    		if ( ID >= nFaces ) {
    			return null;
    		}
    		
	        gl.glDisable( GL.GL_DEPTH_TEST );
	        gl.glDisable( GL2.GL_LIGHTING );
	        gl.glBegin(GL.GL_TRIANGLES);
	        Face face = heds.faces.get(ID);
	        HalfEdge he = face.he;
	        Point3d v1 = he.head.p;
	        Point3d v2 = he.next.head.p;
	        Point3d v3 = he.next.next.head.p;
	        
	        gl.glColor3f( 1, 0, 0 );
	        gl.glVertex3d( v1.x, v1.y, v1.z );
	        gl.glColor3f( 0, 1, 0 );
	        gl.glVertex3d( v2.x, v2.y, v2.z );
	        gl.glColor3f( 0, 0, 1 );
	        gl.glVertex3d( v3.x, v3.y, v3.z );
	        gl.glEnd();
	        gl.glEnable( GL.GL_DEPTH_TEST );
	        gl.glEnable( GL2.GL_LIGHTING );
	     
    		colorPixels.rewind();
    		gl.glReadPixels( x, y, 1, 1, GL2.GL_RGB, GL.GL_UNSIGNED_BYTE, colorPixels );
    		colorPixels.rewind();
    		r = colorPixels.get() & 0xff;
    		g = colorPixels.get() & 0xff;
    		b = colorPixels.get() & 0xff;
    		BCC.set( r / 255.0, g / 255.0, b / 255.0 );	
    		return face;
        }
        return null;
    }

}
