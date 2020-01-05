/*
 * Name: E M V Naga Karthik
 * McGill ID: 260906923
 */

package comp557.a3;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;

import mintools.parameters.BooleanParameter;
import mintools.parameters.DoubleParameter;
import mintools.parameters.IntParameter;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.EasyViewer;
import mintools.viewer.Interactor;
import mintools.viewer.SceneGraphNode;

/**
 * COMP557 - Compute mesh geodesics using the heat method
 */
public class MeshProcessingApp implements SceneGraphNode, Interactor {

    public static void main(String[] args) {
        new MeshProcessingApp();
    }
    
    /** The loaded soup */
    private PolygonSoup soup;
    
    /** The half edge data structre created from the loaded soup */
    private HEDS heds;
    
    /** A half edge to draw on the mesh, and to move with the keyboard controls for debugging */
    private HalfEdge currentHE;
    
    /** index to identify which soup file should be loaded (used by keyboard controls) */
    private int whichSoup = 0;
    
    private String[] soupFiles = {
    		"meshdata/bunnyLowRes.obj",
    		"meshdata/icosahedron.obj",
    		"meshdata/icoSphere2.obj",
    		"meshdata/icosphere6.obj",
    		"meshdata/torusSmall.obj",		// has quads
    		"meshdata/torus.obj",			// has quads
    		"meshdata/headtri.obj",
    		"meshdata/cow.obj",
    		"meshdata/bunny.obj",
            "meshdata/human.obj",			// has quads
    		"meshdata/cube.obj",			// should preserve sharp features in display
    		"meshdata/cube2obj.obj",		// should preserve sharp features in display
    		"meshdata/werewolf.obj", 		// has quads
        };
    
    public MeshProcessingApp() {    
        loadMesh( soupFiles[whichSoup] );
        EasyViewer ev = new EasyViewer("COMP 557 Geodesics in heat (E M V Naga Karthik)", this, new Dimension(400, 400), new Dimension(600, 650) );
        ev.addInteractor(this);
    }
    
    /** Flag to ensure that init is called on mesh objects to let them set up buffers and shaders for newly loaded meshes */
    boolean newMeshLoaded = false;
    
    /**
     * Loads the specified mesh, and sets a flag so that OpenGL buffers will be set up correctly in the next display call.
     */
    private void loadMesh( String filename ) {          
        soup = new PolygonSoup( filename );
        heds = new HEDS( soup );        
        heds.computeLaplacian();        
        heds.resetHeatAndDistanceSolution();       
        newMeshLoaded = true;
        if ( heds.faces.size() > 0 ) {
        	currentHE = heds.faces.iterator().next().he;
        } else {
        	currentHE = null;
        }
    }

    /** Flag for reseting the solutions for heat and distance to zero */
    boolean resetSolutions = false;

    /** Mouse point clicked in a picking (selection) request */
    private Point clickPoint = new Point();
    /** Flag to request picking is done in the next display call */
    private boolean selectRequest = false;
    
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
    
        if ( newMeshLoaded ) {
        	newMeshLoaded = false;
            meshPicking.setMesh( gl, heds );
            meshDraw.setMesh( gl, heds );
        }
        
        if ( ! cullFace.getValue() ) {             
        	gl.glDisable( GL.GL_CULL_FACE );
        } else {
        	gl.glEnable( GL.GL_CULL_FACE );
        }

        if ( !wireFrame.getValue()) {
            // if drawing with lighting, we'll set the material
            // properties for the font and back surfaces, and set
            // polygons to render filled.
            gl.glEnable(GL2.GL_LIGHTING);
            final float frontColour[] = {.7f,.7f,0,1};
            final float backColour[] = {0,.7f,.7f,1};
            final float[] shinyColour = new float[] {1f, 1f, 1f, 1};            
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glMaterialfv( GL.GL_FRONT,GL2.GL_AMBIENT_AND_DIFFUSE, frontColour, 0 );
            gl.glMaterialfv( GL.GL_BACK,GL2.GL_AMBIENT_AND_DIFFUSE, backColour, 0 );
            gl.glMaterialfv( GL.GL_FRONT_AND_BACK,GL2.GL_SPECULAR, shinyColour, 0 );
            gl.glMateriali( GL.GL_FRONT_AND_BACK,GL2.GL_SHININESS, 50 );
            gl.glLightModelf(GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
            gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_FILL );            
        } else {
            // if drawing without lighting, we'll set the colour to white
            // and set polygons to render in wire frame
            gl.glDisable( GL2.GL_LIGHTING );
            gl.glColor4f(.7f,.7f,0.0f,1);
            gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_LINE );
        }    
        
        if ( resetSolutions == true ) {
        	resetSolutions = false;
        	heds.resetHeatAndDistanceSolution();
        }
        
        heds.solveHeatFlowStep( steps.getValue(), tSol.getValue() );        
        heds.updateGradu();
        heds.updateDivx();
        heds.solveDistanceStep( steps.getValue() );
        
        final Vector3d BBC = new Vector3d();
		if ( selectRequest ) {
			selectRequest = false;
			Face face = meshPicking.pickTriangles( drawable, clickPoint, BBC);
	    	gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
			if ( face != null ) {
				// clear the constrained vertices
				for ( Vertex v : heds.vertices ) {
					v.constrained = false;
					v.u0 = 0;
				}
				// choose the closest vertex based on the barycentric coordinates		
				Vertex selectedVertex;
				if ( BBC.x > BBC.y && BBC.x > BBC.z ) {
					selectedVertex = face.he.head;			        
				} else if ( BBC.y > BBC.x && BBC.y > BBC.z ) {
					selectedVertex = face.he.next.head;					
				} else {
					selectedVertex = face.he.next.next.head;
				}				
				selectedVertex.constrained = true;
				selectedVertex.u0 = 1;
				selectedVertex.ut = 1;
			}
		}

		if ( drawHEDSMesh.getValue() ) {
	        if ( useGLSL.getValue() ) {
	        	if ( drawPicking.getValue() ) {
	    			meshPicking.pickTriangles( drawable, clickPoint, BBC );
	        	} else {
	        		meshDraw.drawVBOs( gl );
	        	}
	        } else {
	    		heds.display( drawable );
	        }
		}
        
        if ( drawPolySoup.getValue() ) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable( GL.GL_BLEND );
            gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA );
            gl.glColor4f(.7f,.7f,7.0f,0.5f);
            gl.glLineWidth(1);
            gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_LINE );
            soup.display( drawable );
            gl.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_FILL );
        }
        
        if ( drawHalfEdge.getValue() ) {
        	if ( currentHE != null ) {
        		currentHE.display(drawable);
        	}
        }
        
        meshDraw.drawGrad( gl );
        
        String msg = soupFiles[whichSoup] + " Faces = " + heds.faces.size() + "\n";
        msg += "max phi = "+ heds.maxphi + "\n";
        
        gl.glColor4f(0.5f,0.5f,0.5f,1);
        EasyViewer.beginOverlay(drawable);
        EasyViewer.printTextLines(drawable, msg, 10,20,15, GLUT.BITMAP_8_BY_13 );
        EasyViewer.endOverlay(drawable);
    }
    
	MeshDrawPicking meshPicking = new MeshDrawPicking();
	MeshDrawHeatGeo meshDraw = new MeshDrawHeatGeo();
	
    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setGL( new DebugGL2(drawable.getGL().getGL2()) );
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(1,1,1,1);    // White Background (necessary for picking)
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glEnable(GL2.GL_NORMALIZE );        
        gl.glShadeModel(GL2.GL_SMOOTH);  // Enable Smooth Shading (only applies to fixed function pipeline)
        meshPicking.init( gl );        
        meshDraw.init(gl);        
    }

    private BooleanParameter drawPolySoup = new BooleanParameter( "draw poly soup mesh", false );
    private BooleanParameter drawHEDSMesh = new BooleanParameter( "draw HEDS mesh", true );  
    private BooleanParameter wireFrame = new BooleanParameter( "wire frame", false );
    private BooleanParameter cullFace = new BooleanParameter( "cull face", true );    
    private BooleanParameter drawHalfEdge = new BooleanParameter( "draw test half edge", true );
    private BooleanParameter useGLSL = new BooleanParameter( "use GLSL", true );
    private BooleanParameter drawPicking = new BooleanParameter( "draw picking" , false );
    
    private DoubleParameter tSol = new DoubleParameter( "time for solution", 1e-3, 1e-3, 1e3 );
    private IntParameter steps = new IntParameter( "GS Steps", 10, 1, 100 );
    
    @Override
    public JPanel getControls() {
        VerticalFlowPanel vfp = new VerticalFlowPanel();
        vfp.add( drawPolySoup.getControls() );
        vfp.add( drawHEDSMesh.getControls() );
        vfp.add( wireFrame.getControls() );            
        vfp.add( cullFace.getControls() );
        vfp.add( drawHalfEdge.getControls() );
        vfp.add( useGLSL.getControls() );
        vfp.add( drawPicking.getControls() );
        vfp.add( meshDraw.getControls() );
        
        vfp.add( tSol.getSliderControls(true) );
        vfp.add( steps.getSliderControls() );
        
        JTextArea ta = new JTextArea(
        		"   space - half edge twin \n" +
        		"   n - half edge next \n" +
        		"   w - walk a bit (he.t.n.t.n.n) \n" +
        		"   page up - previous model\n" +
        		"   page down - next model\n" );                  
        ta.setEditable(false);
        ta.setBorder( new TitledBorder("Keyboard controls") );
        vfp.add( ta );
        return vfp.getPanel();
    }

    @Override
    public void attach(Component component) {
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if ( currentHE.twin != null ) currentHE = currentHE.twin;                    
                } else if (e.getKeyCode() == KeyEvent.VK_N) {
                    if ( currentHE.next != null ) currentHE = currentHE.next;
                } else if (e.getKeyCode() == KeyEvent.VK_W) {
                    if ( currentHE.next != null ) currentHE = currentHE.twin.next.twin.next.next;
                } else if ( e.getKeyCode() == KeyEvent.VK_PAGE_UP ) {
                    if ( whichSoup > 0 ) whichSoup--;                    
                    loadMesh( soupFiles[whichSoup] );
                } else if ( e.getKeyCode() == KeyEvent.VK_PAGE_DOWN ) {
                    if ( whichSoup < soupFiles.length -1 ) whichSoup++;                    
                    loadMesh( soupFiles[whichSoup] );
                } else if ( e.getKeyCode() == KeyEvent.VK_R ) {
                	resetSolutions = true;
                } 
            }
        });
        component.addMouseListener( new MouseListener() {
			public void mouseReleased(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {
				clickPoint.setLocation( e.getPoint() );
				if ( e.getButton() == 1 ) {
					selectRequest = true;
				}
			}
		});
    }

    
}
