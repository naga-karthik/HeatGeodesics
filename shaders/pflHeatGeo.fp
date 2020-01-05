/*
 * Name: E M V Naga Karthik
 * McGill ID: 260906923
 */

#version 330 core

in vec3 camSpacePosition;
in vec3 camSpaceNormal;
in float utv;
in float phiv;

uniform vec3 lightCamSpacePosition;
uniform vec3 lightColor;
uniform vec3 materialDiffuse;

uniform int shininessVal;

void main(void) {
	
	vec3 v = normalize(-camSpacePosition);
	vec3 n = normalize(camSpaceNormal);
	vec3 l = normalize(lightCamSpacePosition - camSpacePosition);

	// TODO: 4, 11 Implement your GLSL per fragement lighting, heat colouring, and distance stripes here!
	
	// calculating the Lambertian component of the light
	vec3 lambertian;
	lambertian = lightColor * materialDiffuse * max(0.0, dot(n,l));

	// calculating the Blinn-Phong shading model and combining it with the Lambertian model to get the color.
	vec3 color;
	vec3 h = normalize(v+l);
	float val = max(0.0, dot(n,h));
	color = lambertian + (lightColor * pow(val,shininessVal));

	// initially used to see the vertex in "red" color 	
//	color.x = color.x + utv;
	
	vec3 gray = vec3(0.1,0.1,0.1);
	vec3 red = vec3(1.0,0.0,0.0);
	vec3 blue = vec3(0.0,0.0,1.0);
	vec3 interp;
	float blend;
	
	// this is part where the linear interpolation is done
	if (utv >= 0.0 && utv <= 0.5){
		interp = mix(blue, gray, (2*utv));   
	}
	else {
		interp = mix(gray, red, (2*utv) -1 );
	}
	
	// using the smoothstep function to create the stripes. Here, 'd' is any parameter that is tweaked to get different concentric circles
	float d = 0.7;
    blend = smoothstep(0,d,mod(phiv,d)) * (1 - smoothstep(0,d,mod(phiv,d)) );
	
	
	// can use this to initially visualize the normal	
//	gl_FragColor = vec4( n.xyz * 0.5 + vec3( 0.5, 0.5,0.5 ), 1 );
    
    gl_FragColor = vec4(color.xyz + interp + (blend), 1.0);
    
    
        
}
