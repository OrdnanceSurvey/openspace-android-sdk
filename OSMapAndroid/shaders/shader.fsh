precision mediump float;
uniform sampler2D texture;
uniform mediump vec4 uTintColor;
varying mediump vec2 fragTextureCoord;

void main()
{
	vec4 col = texture2D(texture, fragTextureCoord);
	if (uTintColor.rgb != vec3(-1,-1,-1)) {
		// tchan: Colour hack: Assume that the marker is red.
		// This calculation works regardless of whether source image alpha is premultiplied.
		float chroma = col.r-col.g;
		float screen = col.g;
		// We do not support tints with alpha. This could be achieved by multupling screen and col.a by uTintColor.a.
		col.rgb = chroma*uTintColor.rgb + screen;
	} else {
		// Alpha is premultiplied!
		col *= uTintColor.a;
	}

	gl_FragColor = col;
}
