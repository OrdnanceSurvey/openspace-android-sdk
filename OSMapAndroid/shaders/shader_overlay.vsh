uniform mat4 uMVPMatrix;
attribute vec4 vCoord;
varying highp vec4 fragVCoord;
void main()
{
    fragVCoord = vCoord;
    gl_Position = uMVPMatrix * vCoord;
    
}

