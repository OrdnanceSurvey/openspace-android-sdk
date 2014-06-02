#!/usr/bin/python

from __future__ import print_function,division
import os,os.path,re,sys,hashlib

def filedump(path,width=200,indent=' '):
	ret = []
	with open(path,'rb') as f:
		for c in f.read():
			s = str(ord(c)) + ','
			if ret and len(ret[-1]) + len(s) <= width:
				ret[-1] += s
			else:
				ret.append(indent+s)
	return '\n'.join(ret)

def readfile(path):
	with open(path,'rb') as f:
		return f.read()

def compress_glsl(shader,compress_whitespace=True):
	# Convert all line endings to "\n".
	# We ought to support \r\n and \n\r and \r, but it doesn't matter since we're not preserving line numbers.
	shader = shader.replace('\r','\n')
	assert '\r' not in shader

	# Replace comments with a single space.
	COMMENT = re.compile(ur'//.*|/\*(?:[^*]|\*[^/])*\*/')
	if compress_whitespace:
		shader = COMMENT.sub(u' ',shader)
	else:
		shader = COMMENT.sub((lambda match:u' '*(match.end()-match.start())),shader)

	if not compress_whitespace:
		# At least we want to strip trailing whitespace, which should have no effect on error messages.
		shader = '\n'.join(line.rstrip() for line in shader.split('\n'))
		return shader

	# Remove strippable WS from preprocessing statements.
	PREPROC = re.compile(ur'^[ \x09\x0b\x0c]*\#[ \x09\x0b\x0c]*(.*)$',re.MULTILINE)
	shader = PREPROC.sub(r'#\1',shader)

	# Linear WS is space, tab, vertical tab, form feed (a.k.a. "np").
	# Replace runs of WS with a single space.
	MULTI_WS = re.compile(ur'[ \x09\x0b\x0c]+')
	shader = MULTI_WS.sub(u' ', shader)

	# "Unneeded" WS is WS that does not separate identifiers from other identifiers or potentially merge operators
	SURROUNDED_WS = re.compile(ur'. .')
	def repl(match):
		match = match.group(0)
		left,space,right = match
		assert space == ' '
		IDENTIFIER = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz'
		if left in IDENTIFIER and right in IDENTIFIER:
			# This is fairly obvious.
			return match
		if left in '0123456789' and right == '.' or left == '.' and right in '0123456789ef':
			# We shouldn't merge "1 . 2" into "1.2". There's also "1.e+1" and possibly others.
			# This is unlikely to be correct, but should be good enough.
			return match
		joined = left+right
		if joined in ('++','--','<<','>>','<=','>=','==','!=','&&','^^','||','+=','-=','^=','/=','%=','&=','^=','|=','//','/*'):
			# Don't merge chars that might turn into another operator, or comment-starters "//" and "/*".
			return match
		return joined
	def strip_unneeded(s):
		s = SURROUNDED_WS.sub(repl,s)
		s = SURROUNDED_WS.sub(repl,s)
		return s

	lines = []
	for line in shader.split('\n'):
		if line.startswith('#'):
			lines.append(line)
			continue
		# If the previous line isn't a preprocessor statement and doesn't contain a comment...
		if lines and not lines[-1].startswith('#') and not '//' in lines[-1]:
			line = lines.pop() + ' ' + line

		# Delete whitespace at the edges.
		line = line.strip()
		# Remove other "unneeded" whitespace.
		line = strip_unneeded(line)
		if line:
			lines.append(line)
	
	shader = '\n'.join(lines)
	EDGE_WS = re.compile(ur'^ | $',re.MULTILINE)
	shader = EDGE_WS.sub(u'', shader)
	#
	# Replace multiple newlines with a single newline and strip leading/trailing newlines.
	shader = shader.replace('\n\n','\n').strip('\n')
	
	return shader

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

def writeshaders(outpath,dir,outclass='Shaders',debug=False):
	VALID_ID_CHARS = re.compile(r'[^A-Za-z0-9]')
	shaders = {}
	for filename in os.listdir(dir):
		ext = os.path.splitext(filename)[1]
		if ext not in ('.fsh','.vsh'):
			continue
		shader = readfile(os.path.join(dir,filename))
		shader_identifier = VALID_ID_CHARS.sub('_',filename)
		if shader_identifier in shaders:
			raise ValueError("Duplicate shader identifier %r, filename: %r"%(shader_identifier, filename))
		shaders[shader_identifier] = shader

	def hash_hex(s):
		return "SHA384:" + hashlib.sha384(s).hexdigest()
	# Hash the input.
	h = hashlib.sha384("<Shader List Magic String>")
	h.update("ThisFile(hash=%s)"%hash_hex(readfile(__file__)))
	h.update("Debug(%r"%debug);
	h.update("Class(%r)"%outclass)
	for (k,v) in sorted(shaders.iteritems()):
		h.update("Shader(%r,hash=%s)"%(k,hash_hex(v)))
	h = h.hexdigest()
	h = '// Shader hash: %s\n'%h

	# Avoid writing to the file if it looks correct.
	if os.path.exists(outpath):
		with open(outpath,'rb') as f:
			line = f.next()
		if line == h:
			print(" Hash is correct, skipping")
			return

	try:
		package,outclass = outclass.rsplit('.',1)
	except ValueError:
		package = None

	with open(outpath,'wb') as f:
		f.write(h)
		if package:
			f.write('package %s;\n\n'%package);
		f.write('final class %s {\n'%(outclass));
		for shader_identifier in sorted(shaders):
			shader = unicode(shaders[shader_identifier])
			if isinstance(debug,bool):
				if debug:
					shader = compress_glsl(shader, compress_whitespace=False)
				else:
					shader = compress_glsl(shader)
				shader_expr = JavaStringLiteral(shader)
			elif debug == 'Eclipse':
				# In Eclipse, there's no easy way to tell the build type.
				# See ProjectHelper.compileInReleaseMode() at
				#   https://android.googlesource.com/platform/sdk/+/tools_r21.1/eclipse/plugins/com.android.ide.eclipse.adt/src/com/android/ide/eclipse/adt/internal/project/ProjectHelper.java
				# Instead, produce both outputs and hope it gets optimized away by the compiler.
				shader = compress_glsl(shader, compress_whitespace=False)
				cshader = compress_glsl(shader)
				shader_expr = '(BuildConfig.DEBUG ? %s : %s)'%(JavaStringLiteral(shader),JavaStringLiteral(cshader))
			f.write('  public static final String %s = %s;\n'%(shader_identifier, shader_expr))
		f.write('}\n');

def main():
	BOOL_MAP = {
		'true':True,
		'false':False,
	}
	import argparse
	parser = argparse.ArgumentParser(description='Generate shaders.java')
	parser.add_argument('dest_dir', help='base directory for output (e.g. "gen")')
	parser.add_argument('classname',help='Class to output (e.g. "com.example.myapp.Shaders')
	parser.add_argument('--debug', action='store', default=None, choices=BOOL_MAP, help='"true"/"false", should correspond to BuildConfig.DEBUG')
	parser.add_argument('--eclipse', action='store_true', help='Pass this when building from Eclipse')
	args = parser.parse_args()

	in_dir = os.path.dirname(os.path.join(os.path.curdir,__file__))
	outclasspath = args.dest_dir
	outclass = args.classname
	package = outclass.rsplit('.',1)[0]
	outpath = os.path.join(outclasspath,*outclass.split('.')) + '.java'
	debug = ('Eclipse' if args.eclipse else BOOL_MAP[args.debug])
	try:
		os.makedirs(os.path.abspath(os.path.dirname(outpath)))
	except OSError:
		pass
	print('Writing shaders to %r/%r...'%(outclasspath,outclass))
	writeshaders(outpath,in_dir,outclass=outclass,debug=debug)

if __name__ == '__main__':
	main()
