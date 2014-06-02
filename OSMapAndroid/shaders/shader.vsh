uniform mat4 uMVPMatrix;
attribute vec4 vCoord, vOffset;
varying highp vec2 fragTextureCoord;
attribute vec2 textureCoord;
void main()
{
    vec4 p = vCoord;
    p.xyz += vOffset.xyz;
    gl_Position = uMVPMatrix * p;
    fragTextureCoord = textureCoord;
}

