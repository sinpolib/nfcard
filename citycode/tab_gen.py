#! /usr/bin/python
#-*- coding: UTF-8 -*-
'''\
NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7


生成NFCard程序需要的城市ZIP码与城市名称对照表

输入：
zip*.txt 城市ZIP码与城市名称对照表
输出：
out/raw*/zip.tab 二进制对照表文件

文件格式：
	输入 zip*.txt：
		每行格式为：4位十进制数ZIP码 '\t'(分隔符) 城市名称字符串(utf8)
		每个字符串的原始字节数不超过200，最多65535行
	输出 zip.tab：
		使用前缀树存储所有信息，所有的地址24位，大端存储（高位在低字节）
		文件开始处是前缀树根节点
		每个节点头部31字节：
			0-30字节：3字节一组，存放地址，表示[0-9]子节点在文件中的绝对偏移
					 如果地址为0表示对应的子节点不存在
			31字节：8位无符号整数，表示后续城市名称字符串的长度，0表示没有信息
			后续N字节： UTF8字符串，存放城市名称
'''

import os
import re
import sys
from array import array
from collections import defaultdict

def main():
	if len(sys.argv) < 2:
		print "useage: ", sys.argv[0], " DATA_DIR"
		sys.exit()

	wkdir = sys.argv[1]
	if not os.path.isdir(wkdir):
		print wkdir, " is not a dir"
		sys.exit()

	p = re.compile(r"zip(.*)\.txt")
	for txt in os.listdir(wkdir):
		m = p.match(txt)
		if m:
			out = os.path.join(wkdir, "out", "raw" + m.group(1))
			if not os.path.isdir(out) : os.makedirs(out)

			src = os.path.join(wkdir, txt)
			dst = os.path.join(out, "zip.tab")

			gentab(src, dst)

def gentab(src, dst):

	ttree = lambda: defaultdict(ttree)
	tree = ttree()

	# 读取 zip*.txt
	p = re.compile(r"([0-9]{4})\t([^\t\r\n]+)")
	inf = open(src, "r")
	while True:
		line = inf.readline()
		if not line: break

		m = p.match(line)
		if not m: raise Exception("not p.match(line): " + line)

		# 构造树节点
		node = reduce(lambda t, k: t[k], m.group(1), tree)
		node["city_name"] = m.group(2)

	inf.close()

	# 生成 zip.tab
	outf = open(dst, "wb")
	writetree(tree, outf)
	outf.close()

	print src, "======>>", dst

def writetree(tree, outf):
	my_start = outf.tell()

	city_name = tree.pop("city_name", "")
	name_len = len(city_name)

	my_len = 3 * 10 + 1 + name_len
	outf.seek(my_len, os.SEEK_CUR)

	# 写入子节点
	sub_address = [0 for x in range(10)]
	for k in sorted(tree.keys()):
		sub_address[int(k)] = writetree(tree[k], outf)

	my_end = outf.tell()
	outf.seek(my_start, os.SEEK_SET)
	for a in sub_address:
		outf.write(array("B", ((a>>16) & 0xFF, (a>>8) & 0xFF, a & 0xFF)))

	outf.write(array("B", (name_len, )))
	if name_len > 0 : outf.write(city_name)
	outf.seek(my_end, os.SEEK_SET)

	return my_start

################################################################

main()
