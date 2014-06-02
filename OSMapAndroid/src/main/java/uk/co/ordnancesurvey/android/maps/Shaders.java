// Shader hash: 73b8fad7a3b5e3d9c59bfea67fadca682025c7c0d2b5ce2fbf96bded2f7c853e8287895d3f937c395e14b130fdfc7817
package uk.co.ordnancesurvey.android.maps;

final class Shaders {
  public static final String shader_circle_fsh = "precision mediump float;uniform mediump vec4 uStrokeColor;uniform mediump vec4 uFillColor;varying vec4 fragVCoord;uniform vec4 uCenterRadius;void main(){vec2 dvec=fragVCoord.xy-uCenterRadius.xy;float r1=uCenterRadius.z-0.5;float r12=uCenterRadius.z+0.5;float r2=uCenterRadius.w-0.5;float r20=uCenterRadius.w+0.5;float scale=exp2(floor(log2(max(1.0,r1))));float d =length(dvec/scale)*scale;if(d<r1){gl_FragColor=uFillColor;}else if(d<r12){gl_FragColor=mix(uFillColor,uStrokeColor,d-r1);}else if(d<r2){gl_FragColor=uStrokeColor;}else if(d<r20){gl_FragColor=mix(uStrokeColor,vec4(0,0,0,0),d-r2);}else{gl_FragColor=vec4(0,0,0,0);}}";
  public static final String shader_fsh = "precision mediump float;uniform sampler2D texture;uniform mediump vec4 uTintColor;varying mediump vec2 fragTextureCoord;void main(){vec4 col=texture2D(texture,fragTextureCoord);if(uTintColor.rgb!=vec3(-1,-1,-1)){float chroma=col.r-col.g;float screen=col.g;col.rgb=chroma*uTintColor.rgb+screen;}else{col*=uTintColor.a;}gl_FragColor=col;}";
  public static final String shader_overlay_fsh = "precision mediump float;uniform mediump vec4 uColor;void main(){vec4 col=uColor;gl_FragColor=uColor;}";
  public static final String shader_overlay_vsh = "uniform mat4 uMVPMatrix;attribute vec4 vCoord;varying highp vec4 fragVCoord;void main(){fragVCoord=vCoord;gl_Position=uMVPMatrix*vCoord;}";
  public static final String shader_vsh = "uniform mat4 uMVPMatrix;attribute vec4 vCoord,vOffset;varying highp vec2 fragTextureCoord;attribute vec2 textureCoord;void main(){vec4 p =vCoord;p.xyz+=vOffset.xyz;gl_Position=uMVPMatrix*p;fragTextureCoord=textureCoord;}";
}
