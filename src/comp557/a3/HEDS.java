/*
 * Name: E M V Naga Karthik
 * McGill ID: 260906923
 */

package comp557.a3;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * Half edge data structure.
 * Maintains a list of faces (i.e., one half edge of each) to allow
 * for easy display of geometry.
 */
public class HEDS {
	
    /** List of faces */
    ArrayList<Face> faces = new ArrayList<Face>();
    
    /** List of vertices */
    ArrayList<Vertex> vertices;

    /** Convenience member for keeping track of half edges you make or need */
    Map<String,HalfEdge> halfEdges = new TreeMap<String,HalfEdge>();
    
    /**
     * Builds a half edge data structure from the polygon soup   
     * @param soup
     */
    public HEDS( PolygonSoup soup ) {
        vertices = soup.vertexList;
        for ( int[] face : soup.faceList ) {
        	// TODO: 2 Build the half edge data structure from the polygon soup, triangulating non-triangular faces
        	
        	// Building the halfedge data structure for triangles
        	// This has been put in a try-catch structure because of the "throws exception" 
        	// in the definition of the "createHalfEdge" function.
        	if (face.length == 3)
        	{ 	        	
        		try {
        			HalfEdge he1 = createHalfEdge(soup, face[0], face[1]);
					HalfEdge he2 = createHalfEdge(soup, face[1], face[2]);
					HalfEdge he3 = createHalfEdge(soup, face[2], face[0]);
					
					// assigning the next pointers
					he1.next = he2;
					he2.next = he3;
					he3.next = he1;
					
					Face faceHalfEdge = new Face(he1);
					he1.leftFace = faceHalfEdge;
					he2.leftFace = faceHalfEdge;
					he3.leftFace = faceHalfEdge;
					
					faces.add(faceHalfEdge);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	// here, the halfedge data structures for shapes other than triangles is
        	// calculated
        	else {
        		
        		// Triangulation for non-triangle shapes (quads, pentagons, etc.)
        		// face.length-2 because for each n-gon there are n-2 triangles formed.
        		for (int i = 0; i < face.length-2; i++) {
            		try {
    					HalfEdge he1 = createHalfEdge(soup, face[0], face[i+1]);
    					HalfEdge he2 = createHalfEdge(soup, face[i+1], face[i+2]);
    					HalfEdge he3 = createHalfEdge(soup, face[i+2], face[0]);
    					
    					he1.next = he2;
    					he2.next = he3;
    					he3.next = he1;
    					
    					Face faceHalfEdge = new Face(he1);
    					he1.leftFace = faceHalfEdge;
    					he2.leftFace = faceHalfEdge;
    					he3.leftFace = faceHalfEdge;
    					
    					faces.add(faceHalfEdge);
    					
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
        		}
        	}	
        }

        // TODO: 3 Compute vertex normals
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            v.n = new Vector3d();
            HalfEdge loop = v.he;
            do {
            	
            	v.n.add(loop.leftFace.n);
            	loop = loop.next.twin;
            	
            }while(loop != v.he);
            
            v.n.normalize();
        }
    }
    
    /**
     * Helper function for creating a half edge, and pairing it up with its twin if the
     * twin half edge has already been created.
     * @param soup 
     * @param i tail vertex index
     * @param j head vertex index
     * @return the half edge, paired with its twin if the twin was created.
     * @throws Exception
     */
    private HalfEdge createHalfEdge( PolygonSoup soup, int i, int j ) throws Exception {
        String p = i+","+j;
        if ( halfEdges.containsKey( p ) ){
            throw new Exception("non orientable manifold");
        }
        String twin = j+","+i;
        HalfEdge he = new HalfEdge();
        he.head = soup.vertexList.get(j);
        he.head.he = he; // make sure the vertex has at least one half edge that points to it.
        he.twin = halfEdges.get( twin );
        if ( he.twin != null ) he.twin.twin = he;
        halfEdges.put( p, he );        
        return he;        
    }    
    
    /** 
     * Reset the solutions for heat and distance
     */
    public void resetHeatAndDistanceSolution() {
    	for ( Vertex v : vertices ) {
    		v.u0 = v.constrained? 1 : 0;
    		v.ut = v.u0;
    		v.phi = 0;
    	}
    }
    
    /** 
     * Perform a specified number of projected Gauss-Seidel steps of the heat diffusion equation.
     * The current ut values stored in the vertices will be refined by this call.
     * @param GSSteps number of steps to take
     * @param t solution time
     */
    public void solveHeatFlowStep( int GSSteps, double t ) {    	
    	// Solve (A - t LC) u_t = u_0 with constrained vertices holding their ut value fixed
    	// Note that this is a backward Euler step of the heat diffusion.
    	for ( int i = 0; i < GSSteps; i++ ) {
    		for ( Vertex v : vertices ) {
    			if ( v.constrained ) continue;  // do nothing for the constrained vertex!
    			
    			// TODO: 7 write inner loop code for the PGS heat solve
    			double sum3 = 0;
    			HalfEdge he = v.he;
    			
    			for(int j = 0; j < v.valence(); j++) {
    				
    				sum3 += (t * v.LCij[j] * he.twin.head.ut);
    				he = he.next.twin;
    				// we need to update the vertices around the vertex i, that is ut_j, with
    				// each loop variable. Therefore, he.twin.head.ut is used. 
    			}
    			
    			v.ut = ( (v.u0 + sum3)/(v.area - (t * v.LCii)) );
//    			System.out.println(v.ut);
    			    			
    		}	
    	}
    }
    
    /**
     * Compute the gradient of heat at each face
     */
    public void updateGradu() {
    	// TODO: 8 update the gradient of u from the heat values, i.e., f.gradu for each Face f
    	
    	Vector3d v = new Vector3d();
    	Vector3d v1 = new Vector3d();
    	Point3d a = new Point3d();
    	Point3d b = new Point3d();
    	
    	for (int i = 0; i < faces.size(); i++) {
        	
    		Face f = faces.get(i);
    		HalfEdge fhe = f.he;
    		f.gradu = new Vector3d();
    		   		
    		do {
    			
        		a = fhe.head.p;
        		b = fhe.next.head.p;
        		v.sub(b,a);
        		
    			v1.cross(f.n,v);    			
    			v1.scale(fhe.next.next.head.ut);
   			
    			f.gradu.add(v1);
    			
    			fhe = fhe.next;
    			
    		}while(fhe != f.he);
    		
    		// the scaling (to make it negative) and normalization 
    		f.gradu.scale(-1);
    		f.gradu.normalize();
    	}
    }
    
    /** 
     * Compute the divergence of normalized gradients at the vertices
     */
    public void updateDivx() {
    	// TODO: 9 Update the divergence of the normalized gradients, ie., v.divX for each Vertex v
    	
    	Point3d vi = new Point3d();
    	Point3d temp1 = new Point3d();
    	Point3d temp2 = new Point3d();

    	Vector3d e1 = new Vector3d();
    	Vector3d e2 = new Vector3d();
    	    	
    	for (Vertex v: vertices) {
    		    		
    		HalfEdge he = v.he;

    		double sumx = 0;
    		for (int j = 0; j < v.valence(); j++) {

    			Face f = he.leftFace;
    			
    			// setting the vertices
        		vi = he.head.p;
        		temp2 = he.next.head.p;
        		temp1 = he.next.next.head.p;
        		
        		// creating vectors from those vertices
        		e1.sub(temp2, vi);
        		e2.sub(temp1, vi);
    			
        		// calculating angles by carefully considering the halfedges to be given as an 
        		// argument to the angleWithNext function 
        		double theta2 = angleWithNext(he);
        		double theta1 = angleWithNext(he.next);

    			sumx += ( ( (1.0/Math.tan(theta1)) * e1.dot(f.gradu) ) + ( (1.0/Math.tan(theta2)) * e2.dot(f.gradu) ) );
//    			System.out.println(sumx);

    			he = he.next.twin;
	
    		}
    		v.divX = (0.5)*sumx; 
//    		System.out.println(v.divX);
    	}
    }
    
    /** Keep track of the maximum distance for debugging and colour map selection */
    double maxphi = 0 ;

    /**
     * Solves the distances
     * Uses Poisson equation, Laplacian of distance equal to divergence of normalized heat gradients.
     * This is step III in Algorithm 1 of the Geodesics in Heat paper, but here is done iteratively 
     * with a Gauss-Seidel solve of some number of steps to refine the solution whenever this method 
     * is called.
     * @param GSSteps number of Gauss-Seidel steps to take
     */
    public void solveDistanceStep( int GSSteps ) {		
    	for ( int i = 0; i < GSSteps; i++ ) {
    		for ( Vertex v : vertices ) {
    			// TODO: 10 Implement the inner loop of the Gauss-Seidel solve to compute the distances to each vertex, phi
    			    			
    			HalfEdge he = v.he;
    			    			
    			double lcijSum = 0;
    			for (int j = 0; j < v.valence(); j++) {
    				
    				lcijSum += (v.LCij[j] * he.twin.head.phi);
    				he = he.next.twin;
    				// similar to the previous Gauss-Seidel step, we need to update the phi value at 
    				// each of the adjacent vertices.
    				    				
    			}    		
    			v.phi = (v.divX - lcijSum)/v.LCii;
//    			System.out.println(v.phi);
    			
    		}    		
    	}
    	
    	// Note that the solution to step III is unique only up to an additive constant,
    	// final values simply need to be shifted such that the smallest distance is zero. 
    	// We also identify the max phi value here to identify the maximum geodesic and to 
    	// use adjusting the colour map for rendering
    	double minphi = Double.MAX_VALUE;
    	maxphi = Double.MIN_VALUE;
		for ( Vertex v : vertices ) {
			
		// if v.phi is not set to zero, then before selecting a heat vertex, the value of v.phi becomes NaN. Once it becomes NaN,
		// it does not change even after selecting the vertex (and recomputing the values). Therefore, to avoid that, an if-else
		// structure is used to check for NaN values. If yes, then it is set to 0.
			if(Double.isNaN(v.phi)) {
				v.phi= 0;
			}
			
			if ( v.phi < minphi ) minphi = v.phi;
			if ( v.phi > maxphi ) maxphi = v.phi;
		}	
		maxphi -= minphi;
		for ( Vertex v : vertices ) {
			v.phi -= minphi;
		}
    }
  
// Method for computing the face area    
    public double faceArea(Face f) {
    	
    	Point3d a = new Point3d();
    	Point3d b = new Point3d();
    	Point3d c = new Point3d();

    	Vector3d n = new Vector3d();
    	Vector3d v1 = new Vector3d();
    	Vector3d v2 = new Vector3d();
    	Vector3d v3 = new Vector3d();
    	
    	n = f.n;
    	a = f.he.head.p;
    	b = f.he.next.head.p;
    	c = f.he.next.next.head.p;
    	v1.sub(b, a);
    	v2.sub(c, b);
    	v3.cross(v1, v2);
    	
    	return (0.5*n.dot(v3)); 	
    }
    
    /**
     * Computes the cotangent Laplacian weights at each vertex.
	 * You can assume no boundaries and a triangular mesh! 
	 * You should store these weights in an array at each vertex,
	 * and likewise store the associated "vertex area", i.e., 1/3 of
	 * the surrounding triangles and NOT scale your Laplacian weights
	 * by the vertex area (see heat solve objective requirements).
     */
        
    public void computeLaplacian() {
    	for ( Vertex v : vertices ) {
    		// TODO: 6 Compute the Laplacian and store as vertex weights, and cotan operator diagonal LCii and off diagonal LCij terms.
    		v.area = 0;
    		v.LCii = 0;
    		v.LCij = new double[ v.valence() ];
    		
    		
    		// Calculation of vertex areas
    		double sum = 0;    		
    		for (int i = 0; i < v.valence(); i++) {
    			// Area of a vertex
    			sum += (faceArea(faces.get(i)));
    			// this calls the method defined just above this function, which calculates the 
    			// area of each face and sums it all up for all the adjacent faces of a particular
    			// vertex
    		}
    		v.area = (1.0/3.0)*sum;  // vertex area is 1/3rd the areas of the adjacent vertices
    		
    		
    		// Calculation of LCii
    		double sum1 = 0;
    		for (int j = 0; j < v.valence(); j++) {
    			
    			sum1 += (1.0/Math.tan(angleWithNext(v.he)) + 1.0/Math.tan(angleWithNext(v.he.twin)));
    			v.he = v.he.next.twin;
    		
    		}
//    		v.LCii = (-1.0/(2*v.area))*sum1;
    		v.LCii = (-1.0/2.0)*sum1;
//    		System.out.println(v.LCii);    		
    		
    		
    		// Calculation of LCij
    		double val = 0;    		
    		for (int j = 0; j < v.LCij.length; j++) {
  			
//    			val = ( (1.0/(2*v.area)) * (1.0/Math.tan(angleWithNext(v.he)) + 1.0/Math.tan(angleWithNext(v.he.twin))));
    			val = ( (1.0/2.0) * ( 1.0/Math.tan(angleWithNext(v.he)) + 1.0/Math.tan(angleWithNext(v.he.twin)) ) );
    			v.LCij[j] = val;
    			v.he = v.he.next.twin;
	
    		}
//    		System.out.println(v.LCij[1]);    		
    	}
    }
    
    /** 
     * Computes the angle between the provided half edge and the next half edge
     * @param he specify which half edge
     * @return the angle in radians
     */
    private double angleWithNext( HalfEdge he ) {
    	// TODO: 6 Implement this function to compute the angle with next edge... you'll want to use this in a few places

    	// so the way this is implemented is that, given a halfedge, it computes the angle that is exactly opposite to that half edge. That is,
    	// say there's a triangle with top vertex A, the bottom vertices B and C. If there is a halfedge between vertices B and C (head at C), 
    	// then, this function computes the angle at A (exactly opposite).
    	// this has been carefully considered in the objective of updating the divergence (divX) and the required angles are only called. 
    	double angle = 0;
    	
    	Point3d vi = new Point3d();
    	Point3d temp1 = new Point3d();
    	Point3d vj = new Point3d();
    	Vector3d v1 = new Vector3d();
    	Vector3d v2 = new Vector3d();
    	    	
        vi = he.head.p;
        temp1 = he.next.head.p;
        vj = he.next.next.head.p;
        v1.sub(vi, temp1);
        v2.sub(vj, temp1);
        v1.normalize();
        v2.normalize();
        angle = Math.acos(v1.dot(v2));    	
    	    	
    	return angle;
    }
    
    /**
     * Legacy drawing code for the half edge data structure by drawing each of its faces.
     * Legacy in that this code uses immediate mode OpenGL.  Per vertex normals are used
     * to draw the smooth surface if they are set in the vertices. 
     * @param drawable
     */
    public void display( GLAutoDrawable drawable ) {
        GL2 gl = drawable.getGL().getGL2();
        for ( Face face : faces ) {
            HalfEdge he = face.he;
            if ( he.head.n == null ) { // don't have per vertex normals? use the face
                gl.glBegin( GL2.GL_POLYGON );
                Vector3d n = he.leftFace.n;
                gl.glNormal3d( n.x, n.y, n.z );
                HalfEdge e = he;
                do {
                	Point3d p = e.head.p;
                    gl.glVertex3d( p.x, p.y, p.z );
                    e = e.next;
                } while ( e != he );
                gl.glEnd();
            } else {
                gl.glBegin( GL2.GL_POLYGON );                
                HalfEdge e = he;
                do {
                	Point3d p = e.head.p;
                    Vector3d n = e.head.n;
                    gl.glNormal3d( n.x, n.y, n.z );
                    gl.glVertex3d( p.x, p.y, p.z );
                    e = e.next;
                } while ( e != he );
                gl.glEnd();
            }
        }
    }
    
}
