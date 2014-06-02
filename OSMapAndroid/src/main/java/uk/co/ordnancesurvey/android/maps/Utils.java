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

import static android.opengl.GLES20.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.opengl.ETC1Util;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

final class Utils {
	private static final String TAG = "GLUtils";

	/**
	* Whether to enable code that works around bugs in the Android Emulator's emulated GL hardware.
	* Use this instead of checking BuildConfig.DEBUG, since we want released SDKs to work on the emulator too.
	* This allows emulator workarounds to be easily removed if necessary (e.g. by pointing it to a build-generated file in gen/).
	*/
	public static final boolean EMULATOR_GLES_WORKAROUNDS_ENABLED = true;

	public static void throwIfErrors()
	{
		String errors = null;
		for (int error; (error = glGetError()) != GL_NO_ERROR; ) {
			String s;
			switch (error)
			{
			case GL_NO_ERROR:
				s = "GL_NO_ERROR";
				break;
			case GL_INVALID_ENUM:
				s = "GL_INVALID_ENUM";
				break;
			case GL_INVALID_VALUE:
				s = "GL_INVALID_VALUE";
				break;
			case GL_INVALID_OPERATION:
				s = "GL_INVALID_OPERATION";
				break;
			case GL_INVALID_FRAMEBUFFER_OPERATION:
				s = "GL_INVALID_FRAMEBUFFER_OPERATION";
				break;
			case GL_OUT_OF_MEMORY:
				s = "GL_OUT_OF_MEMORY";
				break;
			default:
				s = "Unknown error " + Integer.toHexString(error);
				break;
			}
			
			errors = (errors == null ? "glGetError() returned" : errors) + " " + s;
		}
		if (errors != null)
		{
			Log.w(TAG, errors);
			throw new Error(errors);
		}
	}


	public static int generateTexture()
	{
		int textureId[] = new int[1];
		glGenTextures(1, textureId, 0);
		throwIfErrors();
		glBindTexture(GL_TEXTURE_2D, textureId[0]);
		throwIfErrors();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		throwIfErrors();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		throwIfErrors();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		throwIfErrors();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		throwIfErrors();
		
		return textureId[0];
	}

	public static void deleteTexture(int textureId) {
		int textureIds[] = new int[]{textureId};
		glDeleteTextures(1, textureIds, 0);
	}

	public static int generateTexture(Bitmap bmp)
	{
		int textureId = Utils.generateTexture();

		// Texture is bound.
		texImage2DPremultiplied(bmp);

		return textureId;
	}

	public static void texImage2DPremultiplied(Bitmap bmp) {
		if (Build.VERSION.SDK_INT >= 17)
		{
			if (bmp.hasAlpha() && !UtilsAPI17.isBitmapPremultiplied(bmp)) {
				Log.w(TAG, "Texture-uploading a non-premultiplied bitmap");
				assert false;
			}
		}

		if (bmp.isRecycled())
		{
			throw new IllegalArgumentException("Tried to texture-upload a recycled bitmap");
		}

		GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
		throwIfErrors();
	}

	@TargetApi(17)
	static class UtilsAPI17 {
		public static boolean isBitmapPremultiplied(Bitmap bmp) {
			return bmp.isPremultiplied();
		}
	}

	public static int compileProgram(String vertexSource, String fragmentSource)
	{
		int program = glCreateProgram();
		int programToDelete = program;
		int vertexShader = 0;
		int fragmentShader = 0;
		try
		{
			vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
			glAttachShader(program, vertexShader);
	
			fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
			glAttachShader(program, fragmentShader);
			glLinkProgram(program);



			int[] buf = new int[1];
			glGetProgramiv(program, GL_LINK_STATUS, buf, 0);
			throwIfErrors();
			if (buf[0] != GL_TRUE) {
				Log.e(TAG, glGetProgramInfoLog(program));
				throw new Error("Program linking failed");
			}
			else if (BuildConfig.DEBUG)
			{
				// This sometimes breaks on emulated video cards, with an error log like
				//   05-16 13:59:41.500: E/(1343): QemuPipeStream::readFully failed, buf=NULL, len 4
				//   05-16 13:59:41.500: V/GLUtils(1343): J
				// A semi-reliable way to detect this should be GL_RENDERER, which looks like
				//   Android Emulator OpenGL ES Translator (ATI Radeon HD 4670 OpenGL Engine)
				if (!(EMULATOR_GLES_WORKAROUNDS_ENABLED && glGetString(GL_RENDERER).startsWith("Android Emulator OpenGL ES Translator (")))
				{
					Log.v(TAG, glGetProgramInfoLog(program));
				}
			}

			programToDelete = 0;
			return program;
		}
		finally
		{
			// tchan: The Android 4.2 emulator is buggy: Attaching the shader to a program doesn't stop it from being deleted.
			// Deleting after glLinkProgram() appears to work.
			// We do this in a finally block to ensure they both get deleted if the second compileShader() throws.
			glDeleteShader(vertexShader);
			glDeleteShader(fragmentShader);

			glDeleteProgram(programToDelete);
			Utils.throwIfErrors();
		}
	}
	public static int compileShader(int type, String source)
	{
		throwIfErrors();
		int shader = glCreateShader(type);
		int shaderToDelete = shader;
		try {
			
			glShaderSource(shader, source);
			glCompileShader(shader);

			int[] buf = new int[1];
			glGetShaderiv(shader, GL_COMPILE_STATUS, buf, 0);
			throwIfErrors();
			if (buf[0] != GL_TRUE)
			{
				Log.e(TAG, glGetShaderInfoLog(shader));
				throw new Error("Shader compilation failed");
			}
			else if (BuildConfig.DEBUG)
			{
				// tchan: Always print the shader log; it makes reduced precision much easier to find.
				Log.v(TAG, glGetShaderInfoLog(shader));
			}

			shaderToDelete = 0;
			return shader;
		}
		finally
		{
			glDeleteShader(shaderToDelete);
		}
	}

	public static FloatBuffer directFloatBuffer(int length) {
		ByteBuffer buf = ByteBuffer.allocateDirect(4*length);
		buf.order(ByteOrder.nativeOrder());

		FloatBuffer ret = buf.asFloatBuffer();
		assert ret.position() == 0;
		assert ret.order() == ByteOrder.nativeOrder();
		assert ret.isDirect();
		return ret;
	}

	public static FloatBuffer directFloatBuffer(float[] a)
	{
		FloatBuffer ret = directFloatBuffer(a.length);
		ret.put(a);
		ret.position(0);
		assert ret.order() == ByteOrder.nativeOrder();
		assert ret.isDirect();
		return ret;
	}

	/**
	* Decodes Android's integer ARGB colour and calls glUniform4f(uniform,r,g,b,a).
	*/
	public static void setUniformPremultipliedColorARGB(int uniform, int color) {
		float a = ((color>>>24)&0xFF)/255.0f;
		float r = ((color>>>16)&0xFF)/255.0f;
		float g = ((color>>>8)&0xFF)/255.0f;
		float b = ((color>>>0)&0xFF)/255.0f;
		glUniform4f(uniform, r*a, g*a, b*a, a);
	}

	/**
	* Proof-of-concept ETC1 texture compression. Incredibly slow, mainly because ETC1Util.compressTexture() does not support using bitmap data directly.
	* @param bmp
	* @return
	*/
	public static ETC1Util.ETC1Texture compressBitmapETC1(Bitmap bmp)
	{
		if (bmp.hasAlpha() || !ETC1Util.isETC1Supported()) {
			return null;
		}
		if (bmp.getConfig() == Bitmap.Config.ARGB_8888) {
			int stride = bmp.getRowBytes()/4;
			int width = bmp.getWidth();
			int height = bmp.getHeight();
			// Bitmap.getByteCount() requires API level 12. Use this instead.
			int[] pixels = new int[stride*height];
			bmp.copyPixelsToBuffer(IntBuffer.wrap(pixels));

			int rgbRowBytes = (width*3+15)&~15;
			assert rgbRowBytes%4 == 0;
			byte[] outBytes = new byte[rgbRowBytes*height];
			for (int yy = 0; yy < height; yy++)
			{
				for (int xx = 0; xx < width; xx++)
				{
					int pixel = pixels[stride*yy+xx];
					byte r = (byte)(pixel>>16);
					byte g = (byte)(pixel>>8);
					byte b = (byte)(pixel);
					outBytes[yy*rgbRowBytes+3*xx] = r;
					outBytes[yy*rgbRowBytes+3*xx+1] = g;
					outBytes[yy*rgbRowBytes+3*xx+2] = b;
				}
			}
			ByteBuffer rgbBuf = ByteBuffer.allocateDirect(rgbRowBytes*height);
			rgbBuf.put(outBytes);
			ETC1Util.ETC1Texture tex = ETC1Util.compressTexture(rgbBuf.position(0), width, height, 3, rgbRowBytes);
			return tex;
		}
		return null;
	}

	public static void logGLInfo()
	{
		throwIfErrors();

		{
			int[] string_ids = {GL_VERSION, GL_VENDOR, GL_RENDERER, GL_SHADING_LANGUAGE_VERSION};
			String[] string_names = {"GL_VERSION", "GL_VENDOR", "GL_RENDERER", "GL_SHADING_LANGUAGE_VERSION"};
			for (int i = 0; i < string_ids.length; i++)
			{
				Log.v(TAG, string_names[i] + ": " + glGetString(string_ids[i]));
			}
		}

		int[] buf = new int[3];

		glGetIntegerv(GL_MAX_TEXTURE_SIZE, buf, 0);
		Log.v(TAG, "Max texture size " + buf[0]);

		int[] shader_types = new int[]{GL_VERTEX_SHADER, GL_FRAGMENT_SHADER};
		String[] shader_names = new String[]{"GL_VERTEX_SHADER", "GL_FRAGMENT_SHADER"};
		int[] precision_types = new int[]{GL_LOW_FLOAT, GL_MEDIUM_FLOAT, GL_HIGH_FLOAT, GL_LOW_INT, GL_MEDIUM_INT, GL_HIGH_INT};
		String[] precision_names = new String[]{"GL_LOW_FLOAT", "GL_MEDIUM_FLOAT", "GL_HIGH_FLOAT", "GL_LOW_INT", "GL_MEDIUM_INT", "GL_HIGH_INT"};
		for (int shader = 0; shader < shader_types.length; shader++) {
			for (int precision = 0; precision < precision_types.length; precision++) {
				glGetShaderPrecisionFormat(shader_types[shader], precision_types[precision], buf, 0, buf, 2);
				Log.v(TAG, String.format(Locale.ENGLISH, "%s %s range [%d,%d] precision %d", shader_names[shader], precision_names[precision], buf[0], buf[1], buf[2]));
			}
		}
		throwIfErrors();
	}
}
