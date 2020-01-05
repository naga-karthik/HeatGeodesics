/*
 * Name: E M V Naga Karthik
 * McGill ID: 260906923
 */

package comp557.a3;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

import mintools.parameters.BooleanParameter;
import mintools.parameters.DoubleParameter;
import mintools.parameters.IntParameter;
import mintools.swing.VerticalFlowPanel;

/**
 * Class for organizing GLSL programs and mesh data for drawing
 * @author kry
 */
public class MeshDrawHeatGeo {

	/** Keep a copy of the half edge data structure being drawn. */
	private HEDS heds;

    /** Vertex and normal data */
	private int pvdVBOID;
    /** Dynamic vertex data */
    private int dvdVBOID;
    /** Index buffer for drawing */
	private int indexVBOID;

	/** Total count of vertices with attributes in the vertex buffer */
	private int pvdCount;
	
	/** Total stride of vertex and static attribute data in bytes */
	private int pvdStrideBytes;
	/** Total stride of dynamic vertex data attributes in bytes */
	private int dvdStrideBytes;
	
	/** native IO buffer for vertex positions, normals (static, does not change) */
    private FloatBuffer pvdBuffer;
    /** native IO buffer for other vertex attributes (e.g., heat) */
    private FloatBuffer dvdBuffer;
    /** native IO buffer of triangles defined by vertex indicies */
	private IntBuffer indexBuffer;
	
	/** GLSL program */
	private ShaderState state = new ShaderState();
	
	// TODO: 4 Add and use a uniform for shininess as per the assignment objectives
	/** Uniform IDs for the GLSL program */
	private int uProjectionID;
	private int uModelviewID;		
	private int uLightPosID;
	private int uLightColorID;
	private int uMaterialDiffuseID;
	
	// defining the uniform for shininess
	private int uShininessID;
	
	/** Attribute IDs for the GLSL programs (i.e., "in" declarations of the vertex program) */ 
	private int attribVertexID = 0;
	private int attribNormalID = 0;
	private int attribUtID = 0;
	private int attribPhiID = 0;
	
	public MeshDrawHeatGeo() {
		// nothing to do in the constructor.  Instead, will set everything up in an init call.
	}
	
	/** 
	 * Initializes the GLSL shader (the mesh must be set prior to this call!)
	 * @param gl
	 */
	public void init( GL2 gl ) {
		// Set up the GLSL program 
        String shaderName = "pflHeatGeo";
        ShaderCode vsCode = ShaderCode.create( gl, GL2.GL_VERTEX_SHADER, this.getClass(), "shaders", "shader/bin", shaderName, false );
        ShaderCode fsCode = ShaderCode.create( gl, GL2.GL_FRAGMENT_SHADER, this.getClass(), "shaders", "shader/bin", shaderName, false );	
        ShaderProgram program = new ShaderProgram();
        program.add( vsCode );
        program.add( fsCode );
		if ( !program.link(gl, System.err) ) {
			throw new GLException("Couldn't link program: " + program );
		}	
		// Once compiled and linked, attach the program and then figure out the 
		// IDs of all the uniforms and attributes we will need to set for this program.		
		state.attachShaderProgram( gl, program, false );
		state.useProgram( gl, true );
    	uProjectionID = state.getUniformLocation(gl, "projection");
    	uModelviewID = state.getUniformLocation(gl, "modelview");    	
    	uLightPosID = state.getUniformLocation(gl, "lightCamSpacePosition");
    	uLightColorID = state.getUniformLocation(gl, "lightColor");
    	uMaterialDiffuseID = state.getUniformLocation(gl, "materialDiffuse");
    	
    	// 
    	uShininessID = state.getUniformLocation(gl, "shininessVal");
    	
		attribVertexID = state.getAttribLocation( gl, "vertex" );
		attribNormalID = state.getAttribLocation( gl, "normal" );
		attribUtID = state.getAttribLocation( gl, "ut" );
		attribPhiID = state.getAttribLocation( gl, "phi" );
		state.useProgram( gl, false );
	}
	
	/**
	 * Sets the mesh to be drawn by this object.
	 * The GL context is needed for setting up the OpenGL buffer objects.
	 * @param gl
	 * @param heds
	 */
	public void setMesh( GL2 gl, HEDS heds ) {
		this.heds = heds;

    	final int [] tmp = new int[3];
    	gl.glGenBuffers( 3, tmp, 0 );
    	pvdVBOID = tmp[0];
    	dvdVBOID = tmp[1];
    	indexVBOID = tmp[2];
		
    	pvdCount = heds.vertices.size();
    	int pvdSize = 3 + 3; // vertex, normal
    	int dvdSize = 1 + 1; // u and phi 
		pvdStrideBytes = pvdSize * Buffers.SIZEOF_FLOAT;
		dvdStrideBytes = dvdSize * Buffers.SIZEOF_FLOAT;

    	pvdBuffer = Buffers.newDirectFloatBuffer( pvdSize * pvdCount );
    	dvdBuffer = Buffers.newDirectFloatBuffer( dvdSize * pvdCount );
		indexBuffer = Buffers.newDirectIntBuffer( heds.faces.size() * 3 );
		
		fillStaticVBOs();
		fillDynamicVBO();
		
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pvdVBOID );
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, pvdStrideBytes * pvdCount, pvdBuffer, GL2.GL_STATIC_DRAW );
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, dvdVBOID );
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, dvdStrideBytes * pvdCount, dvdBuffer, GL2.GL_DYNAMIC_DRAW );
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, indexVBOID );
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, heds.faces.size() * 3 * Buffers.SIZEOF_INT, indexBuffer, GL2.GL_STATIC_DRAW );
    }		
	
	/** 
	 * Sets vertex buffer data that needs to be updated on each display call
	 * (i.e., the dynamic vertex attributes, such as heat, and distance)
	 * @param gl
	 */
	public void fillDynamicVBO() {
    	dvdBuffer.rewind();
    	
    	double s = scale.getValue();
    	double p = power.getValue();
    	for ( Vertex v : heds.vertices ) {
    		dvdBuffer.put( (float) Math.pow( Math.max( v.ut, 0 ), p ) );
    		dvdBuffer.put( (float) ( s * v.phi ) );
    	}    	
    	dvdBuffer.rewind();
	}
	
    public void fillStaticVBOs() {
    	pvdBuffer.rewind();
    	for ( Vertex v : heds.vertices ) {
    		pvdBuffer.put( (float) v.p.x );
    		pvdBuffer.put( (float) v.p.y );
    		pvdBuffer.put( (float) v.p.z );
    		if ( v.n == null ) { // just in case nobody set the normals yet!
    			pvdBuffer.put( 0f );
    			pvdBuffer.put( 0f );
    			pvdBuffer.put( 1f );
    		} else {
	    		pvdBuffer.put( (float) v.n.x );
	    		pvdBuffer.put( (float) v.n.y );
	    		pvdBuffer.put( (float) v.n.z );
    		}    		
    	}    	
    	pvdBuffer.rewind();
    
    	indexBuffer.rewind();
    	for ( Face f : heds.faces ) {
    		indexBuffer.put( f.he.head.index );
    		indexBuffer.put( f.he.next.head.index );
    		indexBuffer.put( f.he.next.next.head.index );
    	}    	
    	indexBuffer.rewind();
    }
    
    public void drawVBOs( GL2 gl ) {
    	
        state.useProgram( gl, true );

		final float[] P = new float[16];
		final float[] V = new float[16];                
		gl.glGetFloatv( GL2.GL_PROJECTION_MATRIX, P, 0 );
		gl.glGetFloatv( GL2.GL_MODELVIEW_MATRIX, V, 0 );
		gl.glUniformMatrix4fv( uProjectionID, 1, false, P, 0);
		gl.glUniformMatrix4fv( uModelviewID, 1, false, V, 0);

		final float [] camSpaceLightPos = new float[] { 0, 20, 20 };
        final float [] camSpaceLightColor = new float[] { 1, 1, 1 };
        final float [] materialDiffuse = new float[] { 0.3f, .3f, .3f };
        
        // getting the shininess values from the slider controls and sending them to the fragment shader
        gl.glUniform1i(uShininessID, shininess.getValue());
        
		gl.glUniform3fv( uLightPosID, 1, camSpaceLightPos, 0);
		gl.glUniform3fv( uLightColorID, 1, camSpaceLightColor, 0);
		gl.glUniform3fv( uMaterialDiffuseID, 1, materialDiffuse, 0);
		
		gl.glEnableVertexAttribArray( attribVertexID );
		gl.glEnableVertexAttribArray( attribNormalID );
		if ( attribUtID != -1 ) gl.glEnableVertexAttribArray( attribUtID );
		if ( attribPhiID != -1 ) gl.glEnableVertexAttribArray( attribPhiID );

		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pvdVBOID );
		gl.glVertexAttribPointer( attribVertexID, 3, GL.GL_FLOAT, false, pvdStrideBytes, 0*4 ); // last parameter is byte offset
		gl.glVertexAttribPointer( attribNormalID, 3, GL.GL_FLOAT, false, pvdStrideBytes, 3*4 );

		fillDynamicVBO(); // refill the vertex data for heat diffusion info
		gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, dvdVBOID );
		gl.glBufferData( GL2.GL_ARRAY_BUFFER, dvdStrideBytes * pvdCount, dvdBuffer, GL2.GL_DYNAMIC_DRAW ); // update each time?
		if ( attribUtID != -1 ) gl.glVertexAttribPointer( attribUtID, 1, GL.GL_FLOAT, false, dvdStrideBytes, 0*4 ); // last parameter is byte offset
		if ( attribPhiID != -1 ) gl.glVertexAttribPointer( attribPhiID, 1, GL.GL_FLOAT, false, dvdStrideBytes, 1*4 );

		gl.glBindBuffer( GL2.GL_ELEMENT_ARRAY_BUFFER, indexVBOID );
		gl.glDrawElements( GL2.GL_TRIANGLES, indexBuffer.capacity(), GL2.GL_UNSIGNED_INT, 0 );

		gl.glDisableVertexAttribArray( attribVertexID );
		gl.glDisableVertexAttribArray( attribNormalID );
		if ( attribUtID != -1 ) gl.glDisableVertexAttribArray( attribUtID );
		if ( attribPhiID != -1 ) gl.glDisableVertexAttribArray( attribPhiID );
		
        state.useProgram( gl, false );
    }
    
    /**
     * Draws the gradients computed at each face.
     * @param gl
     */
    public void drawGrad( GL2 gl ) {
    	if ( drawGrad.getValue() ) {
	        gl.glDisable( GL2.GL_LIGHTING );
	        gl.glColor4f(0.5f,0.5f,0.5f,1);
	        double h = gradHeight.getValue();
	        double s = gradScale.getValue();
	        for ( Face f : heds.faces ) {
	        	f.drawGradu(gl, h, s);
	        }
        }
    }
    
    private BooleanParameter drawGrad = new BooleanParameter( "draw grad u", false );
    private DoubleParameter gradHeight = new DoubleParameter( "grad u viz offset", 0.1, 1e-3, 1e2 );
    private DoubleParameter gradScale = new DoubleParameter( "grad u viz scale", 5e-2, 1e-3, 1e3 ); 
    private DoubleParameter scale = new DoubleParameter( "scale distance solution values", 0.1, 1e-2, 1e2 );
    private DoubleParameter power = new DoubleParameter( "power for visualizing heat", 1, 1e-3, 1e3 );

    private IntParameter shininess = new IntParameter( "shininess", 255, 1, 255 );
    
    JPanel getControls() {
    	VerticalFlowPanel vfp = new VerticalFlowPanel();
    	vfp.setBorder( new TitledBorder("Heat Geodesic Drawing Controls" ) );
        vfp.add( scale.getSliderControls(true) );
        vfp.add( power.getSliderControls(true) );
        vfp.add( drawGrad.getControls() );
        vfp.add( gradHeight.getSliderControls(true) );
        vfp.add( gradScale.getSliderControls(true) );
        vfp.add( shininess.getSliderControls() );
    	return vfp.getPanel();
    }
    
}
