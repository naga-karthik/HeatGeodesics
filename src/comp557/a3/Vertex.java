/*
 * Name: E M V Naga Karthik
 * McGill ID: 260906923
 */

package comp557.a3;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Simple vertex class
 */
public class Vertex {
    
	/** We must know the vertex index when assembling openGL buffers */
	public int index;
	
	/** The position of this vertex */
	public Point3d p = new Point3d();

	/** Vertex normals, to be computed as an average of adjacent faces */
    public Vector3d n;
    
    /** an example half edge */
    public HalfEdge he;

    /** 1/3 area of surrounding faces */
    double area = 0;

    /** diagonal Laplacian weights */
    double LCii;
    
    /** off diagonal Laplacian weights */
    double[] LCij;
    
    /** True when this vertex is the heat source vertex */
    boolean constrained = false;

    /** Heat initial conditions */
    double u0 = 0;

    /** Heat value after time t */
    double ut = 0;
   
    /** divergence of the normalized gradient of u on adjacent faces */
    double divX = 0;
    
    /** distance */
    double phi = 0;
    
    
    /** 
     * @returns the valence of this vertex 
     */
    public int valence() {
    	// TODO: 5 compute the valence of this vertex
    	HalfEdge h = he;
    	int i = 0;
    	do {
    		h = h.next.twin;
    		i++;
    	}while(h != he);
    	   	
    	return i;
    }
    
}
