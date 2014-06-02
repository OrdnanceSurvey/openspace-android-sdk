/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package uk.co.ordnancesurvey.android.maps;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.annotation.TargetApi;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

final class DebugHelpers {

	public static void logAndroidBuild() {
		if (BuildConfig.DEBUG)
		{
			Log.v("Build.BOARD", Build.BOARD);
			Log.v("Build.BOOTLOADER", Build.BOOTLOADER);
			Log.v("Build.BRAND", Build.BRAND);
			Log.v("Build.CPU_ABI", Build.CPU_ABI);
			Log.v("Build.CPU_ABI2", Build.CPU_ABI2);
			Log.v("Build.DEVICE", Build.DEVICE);
			Log.v("Build.DISPLAY", Build.DISPLAY);
			Log.v("Build.FINGERPRINT", Build.FINGERPRINT);
			Log.v("Build.HARDWARE", Build.HARDWARE);
			Log.v("Build.HOST", Build.HOST);
			Log.v("Build.ID", Build.ID);
			Log.v("Build.MANUFACTURER", Build.MANUFACTURER);
			Log.v("Build.MODEL", Build.MODEL);
			Log.v("Build.PRODUCT", Build.PRODUCT);
			Log.v("Build.SERIAL", Build.SERIAL);
			Log.v("Build.TAGS", Build.TAGS);
			Log.v("Build.TIME", ""+Build.TIME);
			Log.v("Build.TYPE", Build.TYPE);
			Log.v("Build.USER", Build.USER);
			Log.v("Build.getRadioVersion()", getRadioVersionHelper());
			Log.v("Build.VERSION.CODENAME", Build.VERSION.CODENAME);
			Log.v("Build.VERSION.INCREMENTAL", Build.VERSION.INCREMENTAL);
			Log.v("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
			Log.v("Build.VERSION.SDK_INT", ""+Build.VERSION.SDK_INT);
		}
	}

	@SuppressWarnings("deprecation")
	private static String getRadioVersionHelper() {
		if (Build.VERSION.SDK_INT >= 14) {
			getRadioVersionAPI14();
		}
		return Build.RADIO;
	}

	@TargetApi(14)
	private static String getRadioVersionAPI14() {
		return Build.getRadioVersion();
	}

	public static GLSurfaceView.EGLConfigChooser loggingConfigChooser() {
		GLSurfaceView.EGLConfigChooser ret = null;
		if (BuildConfig.DEBUG) {
			ret = new GLSurfaceView.EGLConfigChooser() {
				@Override
				public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
					int[] temp = new int[1];
					int[] attribs = new int[] {
							EGL10.EGL_RED_SIZE, 8,
							EGL10.EGL_GREEN_SIZE, 8,
							EGL10.EGL_BLUE_SIZE, 8,
							EGL10.EGL_DEPTH_SIZE, 1,
							EGL10.EGL_NONE,
						};
					egl.eglChooseConfig(display, attribs, null, 0, temp);
					int numConfigs = temp[0];
					EGLConfig[] configs = new EGLConfig[numConfigs];
					egl.eglChooseConfig(display, attribs, configs, numConfigs, temp);
					assert temp[0] == numConfigs;
					for (EGLConfig cfg : configs) {
						logConfig(egl, display, cfg);
					}
					return configs[0];
				}

				private String[] EGL_ATTRIBUTE_NAMES = new String[]{
						"EGL_ALPHA_SIZE",
						"EGL_ALPHA_MASK_SIZE",
						"EGL_BIND_TO_TEXTURE_RGB",
						"EGL_BIND_TO_TEXTURE_RGBA",
						"EGL_BLUE_SIZE",
						"EGL_BUFFER_SIZE",
						"EGL_COLOR_BUFFER_TYPE",
						"EGL_CONFIG_CAVEAT",
						"EGL_CONFIG_ID",
						"EGL_CONFORMANT",
						"EGL_DEPTH_SIZE",
						"EGL_GREEN_SIZE",
						"EGL_LEVEL",
						"EGL_LUMINANCE_SIZE",
						"EGL_MAX_PBUFFER_WIDTH",
						"EGL_MAX_PBUFFER_HEIGHT",
						"EGL_MAX_PBUFFER_PIXELS",
						"EGL_MAX_SWAP_INTERVAL",
						"EGL_MIN_SWAP_INTERVAL",
						"EGL_NATIVE_RENDERABLE",
						"EGL_NATIVE_VISUAL_ID",
						"EGL_NATIVE_VISUAL_TYPE",
						"EGL_RED_SIZE",
						"EGL_RENDERABLE_TYPE",
						"EGL_SAMPLE_BUFFERS",
						"EGL_SAMPLES",
						"EGL_STENCIL_SIZE",
						"EGL_SURFACE_TYPE",
						"EGL_TRANSPARENT_TYPE",
						"EGL_TRANSPARENT_RED_VALUE",
						"EGL_TRANSPARENT_GREEN_VALUE",
						"EGL_TRANSPARENT_BLUE_VALUE",
				};

				private String errorString(Class<? extends EGL10> eglClass, int error) throws Exception {
					String ret = null;
					for (Field f : eglClass.getFields()) {
						if (f.getType() == Integer.TYPE && Modifier.isStatic(f.getModifiers())) {
							if (f.getInt(eglClass) == error) {
								ret = (ret == null ? "" : ret+"/") + f.getName();
							}
						}
					}
					return (ret == null ? Integer.toString(error) : ret);
				}

				private String errorString(EGL10 egl) {
					try {
						String ret = null;
						for (int error; EGL10.EGL_SUCCESS != (error = egl.eglGetError()); ) {
							ret = (ret == null ? "" : ret+" ") + errorString(egl.getClass(), error);
						}
						return (ret == null ? "no_error" : ret);
					} catch (Exception e) {
						return e.toString();
					}
				}

				private void logConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
					int[] temp = new int[1];
					Log.v("Preexisting errors", errorString(egl));
					Class<? extends EGL10> eglClass = egl.getClass();
					for (String name : EGL_ATTRIBUTE_NAMES) {
						try {
							int attribute = eglClass.getField(name).getInt(eglClass);
							boolean success = egl.eglGetConfigAttrib(display, config, attribute, temp);
							if (!success) {
								throw new Exception(errorString(egl));
							}
							Log.v(name, "" + temp[0]);
						} catch (Exception e) {
							Log.v(name, "Failed with " + e.toString());
						}
					}
				}
			};
		}
		return ret;
	}
}
