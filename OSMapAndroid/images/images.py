#!/usr/bin/python

from __future__ import print_function,division
import os,os.path,re,sys,hashlib,base64,subprocess,glob

def b64url_encode(data):
	'''"URL-safe" base-64 ("base64url") encoding and decoding. Padding is stripped.
	See http://tools.ietf.org/html/rfc4648#section-5'''
	return base64.urlsafe_b64encode(data).rstrip('=')

def readfile(path):
	with open(path,'rb') as f:
		return f.read()

def bytedumpjava(s,width=200,indent=' '):
	ret = []
	for c in s:
		s = str((ord(c)+128)%256-128) + ','
		if ret and len(ret[-1]) + len(s) <= width:
			ret[-1] += s
		else:
			ret.append(indent+s)
	return '\n'.join(ret)

def JavaStringLiteral(s):
	assert isinstance(s,unicode)
	ret = ['"']
	special_map = {
		# Java's peculiar backslash-escape mechanism means four characters cannot use \uNNNN escapes:
		u'\n': r'\n',
		u'\r': r'\r',
		u'\"': r'\"',
		u'\\': r'\\',
		# There are four others which we can use for size.
		u'\x08': r'\b',
		u'\x09': r'\t',
		u'\x0c': r'\f',
		u'\x27': r'\'',
	}.get
	for c in s:
		mapped = special_map(c)
		if mapped is not None:
			ret.append(mapped)
		elif u' ' <= c <= u'~':
			assert c not in '\\"'
			ret.append(c)
		elif ord(c) <= 0xFF:
			ret.append('\\%03o'%c)
		elif ord(c) <= 0xFFFF:
			assert c not in '\r\n\\"'
			ret.append('\\u%04x'%ord(c))
		else:
			raise ValueError("non-BMP characters not supported yet")
	ret.append('"')
	return ''.join(ret)

def camelcase(s):
	l = s.replace('_',' ').split(' ',1)
	if len(l) == 1:
		return l[0]
	head,tail = l
	return head+tail.title().replace(' ','')

def safepath(path):
	return os.path.join(os.path.curdir,path)

def get_aapt_cmd():
	# Use the AAPT environment variable if it is set.
	aapt_cmd = os.environ.get('AAPT')
	if aapt_cmd:
		return aapt_cmd

	# If the ANDROID_HOME environment variable is set, look in $ANDROID_HOME/platform-tools/aapt.
	# This is also the convention used in the default build.xml.
	android_home = os.environ.get('ANDROID_HOME')
	if android_home:
		try:
			build_tools, = glob.glob(os.path.join(android_home,'build-tools','*'))
			aapt_cmd = os.path.join(build_tools, 'aapt')
			return aapt_cmd
		except ValueError:
			pass

		aapt_cmd = os.path.join(android_home,'platform-tools','aapt')
		return aapt_cmd

	# Otherwise, just run "aapt", searching for it in $PATH
	aapt_cmd = 'aapt'

def writeimages(outpath,dir,crunchdir=None,outclass='Images'):
	INVALID_ID_CHARS = re.compile(r'[^A-Za-z0-9]')
	DPIs = {
		'-ldpi'  :120, # DENSITY_LOW
		'-mdpi'  :160, # DENSITY_MEDIUM, DENSITY_DEFAULT
		'-hdpi'  :240, # DENSITY_HIGH
		'-xhdpi' :320, # DENSITY_XHIGH
		'-xxhdpi':480, # DENSITY_XXHIGH

		# "openspace-marker-hires1.png" at 929 DPI ~= "marker_red.png" at 160 DPI (DENSITY_DEFAULT).
		# We round this to 960 which evenly divides all built-in DPI levels.
		'-hires1':960,
	}
	images = {}
	for filename in os.listdir(dir):
		if filename.startswith('.'):
			# "aapt -c" ignores dotfiles.
			continue
		for suffix in ('.9.png','.png'):
			# os.path.splitext() ignores leading dots, but we handle that above anyway.
			if filename.endswith(suffix):
				filebase,ext = filename[:-len(suffix)], filename[-len(suffix):]
				break
		else:
			continue

		image = readfile(os.path.join(dir,filename))

		# Strip any DPI suffix, setting the corresponding DPI.
		for suffix,sdpi in DPIs.iteritems():
			if filebase.endswith(suffix):
				if len(suffix):
					filebase = filebase[:-len(suffix)]
				dpi = sdpi
				break
		else:
			dpi = 0

		image_identifier = INVALID_ID_CHARS.sub('_',filebase)
		if image_identifier in images:
			raise ValueError("Duplicate image identifier %r, filename: %r"%(image_identifier, filename))
		imgtype = None
		images[image_identifier] = (image,filename,dpi,imgtype)

	# Hash the input.
	def hash_hex(s):
		return "SHA384:" + hashlib.sha384(s).hexdigest()
	h = hashlib.sha384("<Image List Magic String>")
	h.update("ThisFile(hash=%s)"%hash_hex(readfile(__file__)))
	h.update("Class(%r)"%outclass)
	h.update("CrunchEnabled(%r)"%(crunchdir is not None))
	for (k,(image,filename,dpi,imgtype)) in sorted(images.iteritems()):
		h.update("Image(%r,filename=%r,hash=%r,dpi=%d)"%(k,filename,hash_hex(image),dpi))
	h = h.hexdigest()
	h = '// Image hash: %s\n'%h

	# Avoid writing to the file if it looks correct.
	if os.path.exists(outpath):
		with open(outpath,'rb') as f:
			line = f.next()
		if line == h:
			print(" Hash is correct, skipping")
			return

	if crunchdir is not None:
		# Crunch images!
		subprocess.check_call([get_aapt_cmd(),'c','-S', safepath(dir), '-C',safepath(crunchdir)])

		# Replace image data with the crunched image data.
		for k in list(images):
			(image,filename,dpi,imgtype) = images[k]
			image = readfile(os.path.join(crunchdir,filename))
			# We don't support nine-patch conversion unless we run aapt.
			# Assume aapt performed nine-patch conversion if the filename ends with ".9.png"
			if filename.endswith('.9.png'):
				imgtype = '.9.png'
			images[k] = (image,filename,dpi,imgtype)

	try:
		package,outclass = outclass.rsplit('.',1)
	except ValueError:
		package = None

	with open(outpath,'wb') as f:
		f.write(h)
		if package:
			f.write('package %s;\n'%package);
		f.write('''
import java.io.ByteArrayInputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Base64;
import android.util.DisplayMetrics;

final class %s {
	final static class NinePatchWrapper {
		final Bitmap mBitmap;
		final Rect mPadding;
		NinePatchWrapper(Bitmap bitmap, Rect padding) {
			mBitmap = bitmap;
			mPadding = padding;
		}
		NinePatchDrawable newDrawable(Resources resources) {
			// The default padding is 0; we need to do this the long way:
			//    https://github.com/android/platform_frameworks_base/blob/jb-mr1-release/graphics/java/android/graphics/drawable/NinePatchDrawable.java
			// NinePatch is also not thread-safe due to the write to mRect:
			//    https://github.com/android/platform_frameworks_base/blob/jb-mr1-release/graphics/java/android/graphics/NinePatch.java
			NinePatchDrawable drawable = new NinePatchDrawable(resources, mBitmap, mBitmap.getNinePatchChunk(), mPadding, null);
			return drawable;
		}
	}
	private static Bitmap scaleAndRecycle(Bitmap bmp, int width, int height) {
		Bitmap ret = Bitmap.createScaledBitmap(bmp, width, height, true);
		if (ret != bmp) {
			bmp.recycle();
		}
		return ret;
	}
	private static Bitmap smoothScaleAndRecycle(Bitmap bmp, int width, int height) {
		boolean wouldOverflow = ((width|height)*2>>>1) != (width|height);
		if (wouldOverflow || bmp.getWidth()/width < 4 || bmp.getHeight()/height < 4) {
			return scaleAndRecycle(bmp, width, height);
		}
		bmp = smoothScaleAndRecycle(bmp, width*2, height*2);
		bmp = scaleAndRecycle(bmp, width,height);
		return bmp;
	}
	private static Bitmap makeImmutableAndRecycle(Bitmap bmp) {
		if (!bmp.isMutable()) {
			return bmp;
		}
		if (bmp.getNinePatchChunk() != null) {
			// Bitmap.createBitmap() doesn't appear to preserve the nine-patch chunk.
			//   https://github.com/android/platform_frameworks_base/blob/android-4.2.2_r1/graphics/java/android/graphics/Bitmap.java#L540
			if (BuildConfig.DEBUG) {
				assert Bitmap.createBitmap(bmp).getNinePatchChunk() == null;
				assert bmp.getConfig() == null || bmp.copy(bmp.getConfig(),false).getNinePatchChunk() == null;
			}
			return bmp;
		}
		// Bitmap.createBitmap(Bitmap) doesn't seem to produce an immutable bitmap either!
		if (BuildConfig.DEBUG) {
			assert Bitmap.createBitmap(bmp).isMutable();
		}
		// Some bitmaps have NULL configs, e.g. GIFs:
		//   https://github.com/android/platform_frameworks_base/blob/android-4.2.2_r1/graphics/java/android/graphics/Bitmap.java#L569
		Bitmap.Config config = bmp.getConfig();
		Bitmap ret = (config == null ? null : bmp.copy(config, false));
		if (ret == null) {
			return bmp;
		}
		if (ret != bmp) {
			bmp.recycle();
		}
		assert !ret.isMutable();
		return ret;
	}
	private static Bitmap bitmapFromUrlsafeBase64(String s, int srcDensity, int targetDensity, Rect paddingOut) {
		byte[] a = Base64.decode(s,Base64.URL_SAFE);
		Bitmap bmp;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inDither = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		if (paddingOut == null) {
			// If we don't need the padding, call decodeByteArray() which will probably be faster.
			//    https://github.com/android/platform_frameworks_base/blob/master/graphics/java/android/graphics/BitmapFactory.java#L428
			bmp = BitmapFactory.decodeByteArray(a,0,a.length,options);
		} else {
			// If we do need the padding, we have to wrap it in a stream.
			ByteArrayInputStream is = new ByteArrayInputStream(a);
			bmp = BitmapFactory.decodeStream(is, paddingOut, options);
			// Not much point closing the stream, since it shouldn't be using resources other than memory.
		}
		if (bmp == null) {
			return bmp;
		}
		assert !bmp.isMutable() : "We expect freshly-loaded bitmaps to be immutable";
		bmp.setDensity(srcDensity);
		boolean needsScale = srcDensity > 0 && targetDensity > 0 && srcDensity != targetDensity;
		if (needsScale) {
			bmp = smoothScaleAndRecycle(bmp,bmp.getScaledWidth(targetDensity), bmp.getScaledHeight(targetDensity));
			// The returned bitmap isn't immutable on Galaxy S3/4.1.2.
			bmp = makeImmutableAndRecycle(bmp);
			assert !bmp.isMutable() || bmp.getNinePatchChunk() != null : "We should only fail to make it immutable if it has a nine-patch chunk";
			bmp.setDensity(targetDensity);
		}
		assert bmp.getDensity() == (needsScale ? targetDensity : srcDensity);
		return bmp;
	}
	private static NinePatchWrapper ninePatchWrapperFromUrlsafeBase64(String s, int srcDensity) {
		Rect padding = new Rect();
		Bitmap bmp = bitmapFromUrlsafeBase64(s, srcDensity, 0, padding);
		byte[] ninePatchChunk = bmp.getNinePatchChunk();
		if (ninePatchChunk == null || !NinePatch.isNinePatchChunk(ninePatchChunk))
		{
			return null;
		}
		return new NinePatchWrapper(bmp, padding);
	}
'''%(outclass));

		for image_identifier in sorted(images):
			image,filename,dpi,imgtype = images[image_identifier]
			#f.write(' public static final Bitmap %s = BitmapFactory.decodeByteArray(new byte[]{\n%s\n },0,%d);\n'%(image_identifier, bytedumpjava(image,indent='  '), len(image)))
			if imgtype == '.9.png':
				f.write('''
	private static volatile NinePatchWrapper %(staticvar)s;
	private static NinePatchWrapper %(gettername)sWrapper (DisplayMetrics metrics) {
		NinePatchWrapper wrapper = %(staticvar)s;
		if (wrapper != null) {
			return wrapper;
		}
		int srcDensityDpi = %(srcDensityDpi)d;
		wrapper = ninePatchWrapperFromUrlsafeBase64(%(imageLiteralB64)s, srcDensityDpi);
		%(staticvar)s = wrapper;
		return wrapper;
	}
	public static NinePatchDrawable %(gettername)sDrawable(Resources resources) {
		return %(gettername)sWrapper(null).newDrawable(resources);
	}\n'''%{
					'staticvar':camelcase(image_identifier),
					'gettername':camelcase('get_' + image_identifier),
					'imageLiteralB64':JavaStringLiteral(unicode(b64url_encode(image))),
					'srcDensityDpi':dpi,
				})
				continue

			f.write('''
	private static volatile Bitmap %(staticvar)s;
	public static Bitmap %(gettername)s (DisplayMetrics metrics) {
		Bitmap bmp = %(staticvar)s;
		int srcDensityDpi = %(srcDensityDpi)d;
		int targetDensityDpi = srcDensityDpi;
		if(metrics != null){
			targetDensityDpi = metrics.densityDpi;
		}
		if (bmp != null && bmp.getDensity() == targetDensityDpi) {
			return bmp;
		}
		bmp = bitmapFromUrlsafeBase64(%(imageLiteralB64)s, srcDensityDpi, targetDensityDpi, null);
		%(staticvar)s = bmp;
		return bmp;
	}\n'''%{
				'staticvar':camelcase(image_identifier),
				'gettername':camelcase('get_' + image_identifier),
				'imageLiteralB64':JavaStringLiteral(unicode(b64url_encode(image))),
				'srcDensityDpi':dpi,
			})
		f.write('}\n');

def mkdir_p(path):
	try:
		os.makedirs(os.path.abspath(path))
	except OSError:
		pass

def main():
	BOOL_MAP = {
		'true':True,
		'false':False,
	}
	import argparse
	parser = argparse.ArgumentParser(description='Generate shaders.java')
	parser.add_argument('--debug', action='store', default=None, choices=BOOL_MAP, help='"true"/"false", should correspond to BuildConfig.DEBUG')
	parser.add_argument('--eclipse', action='store_true', help='Pass this when building from Eclipse')
	parser.add_argument('--crunchdir', action='store', help='Tempdir for use by "aapt c"; required for NinePatch (.9.png) support')
	parser.add_argument('dest_dir', help='base directory for output (e.g. "gen")')
	parser.add_argument('classname',help='Class to output (e.g. "com.example.myapp.Shaders')
	args = parser.parse_args()

	in_dir = os.path.dirname(os.path.join(os.path.curdir,__file__))
	outclasspath = args.dest_dir
	outclass = args.classname
	crunchdir = args.crunchdir
	package = outclass.rsplit('.',1)[0]
	outpath = os.path.join(outclasspath,*outclass.split('.')) + '.java'
	mkdir_p(os.path.dirname(outpath))
	if crunchdir is not None:
		mkdir_p(crunchdir)
	print('Writing images to %r/%r...'%(outclasspath,outclass))
	writeimages(outpath,in_dir,crunchdir=crunchdir,outclass=outclass)

if __name__ == '__main__':
	main()
