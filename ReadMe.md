# Mesh Processing, Geodesics and Heat Diffusion

This project is one of the assignments in the Fundamentals of Computer Graphics (COMP557) class at McGill University. The objective is to load the given mesh objects and implement a few geometry processing algorithms to compute the geodesic distances on the mesh manifold. 

It is based on the ACM Transactions on Graphics paper, titled, "Geodesics in Heat: A New Approach to Computing Distance Based on Heat Flow". While it is good to read the paper, complete understanding of it is not required for this project.

## Getting Started
The code is entirely based on Java and some OpenGL, therefore the jogl, vecmath and mintools jar files need to installed for running the code. Inside the "src" folder are some classes that run in tandem to show the diffusion of heat in various objects (given in the "meshdata" folder).

Here is a brief explanation of all the classes:
* MeshProcessingApp - The main function that creates a view.
* HalfEdge - Defines the half edge data structure (commonly used for processing meshes in general).
* Face - Defines a face class. Each half edge is associated to a face (typically called leftFace). Some additional members are also defined to store the position of the center, the area and the gradients of heat value across the mesh.
* Vertex - Defines a vertex class. Each half edge has its own vertex head (which it always points to). Members for storing the surface normal, Laplacian coefficients, heat values etc. are also defined.
* PolygonSoup - A class for parsing the obj files containing mesh data.
* HEDS - The most important class of all. It contains the half edge data structure constructed from the PolygonSoup. Most of the mathematical methods discussed in the paper are implemented here.
* MeshDrawHeatGeo - This class is responsible for loading and linking the vertex and fragment programs, setting up the buffers for drawing.
* MeshDrawPicking - This sets up the 'picking' GLSL program, initializes buffers to draw each triangle in a unique color for easy selection of mesh faces.



 

