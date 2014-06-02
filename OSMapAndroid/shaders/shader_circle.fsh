precision mediump float;
uniform mediump vec4 uStrokeColor;
uniform mediump vec4 uFillColor;
varying vec4 fragVCoord;
uniform vec4 uCenterRadius;

void main()
{
	vec2 dvec = fragVCoord.xy-uCenterRadius.xy;
	// Some very basic AA: Just smooth the function by 1px across the boundary.
	float r1 = uCenterRadius.z-0.5;
	float r12 = uCenterRadius.z+0.5;
	float r2 = uCenterRadius.w-0.5;
	float r20 = uCenterRadius.w+0.5;
	// Fudge to avoid reduce the chance of exponent overflow in mediump length(). This pretty horrible.
	//float scale = 128.0;
	// An alternative is to scale by the log of the inner radius, which is also pretty horrible.
	float scale = exp2(floor(log2(max(1.0,r1))));
	float d = length(dvec/scale)*scale;
	if (d < r1) {
		gl_FragColor = uFillColor;
	} else if (d < r12) {
		gl_FragColor = mix(uFillColor,uStrokeColor,d-r1);
	} else if (d < r2) {
		gl_FragColor = uStrokeColor;
	} else if (d < r20) {
		gl_FragColor = mix(uStrokeColor,vec4(0,0,0,0),d-r2);
	} else {
		gl_FragColor = vec4(0,0,0,0);
	}
}
