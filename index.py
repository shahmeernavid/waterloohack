#!/usr/bin/python
import os
import cherrypy
import shelve
import string
import random
import datetime
import math
import sha
import io
import copy
def distance(lat1, lon1, lat2, lon2):
	'returns distance in meters, assuming sphere'
	lat1*=math.pi/180
	lon1*=math.pi/180
	lat2*=math.pi/180
	lon1*=math.pi/180
	return(2*6372800)*math.asin(math.sqrt(math.sin((lat2-lat1)*.5)**2+math.cos(lat1)*math.cos(lat2)*math.sin((lon2-lon1)*.5)**2))
def digest(password, salt=sha.new('NSGbUvrdqwqHYFAQ')):
	hasher = salt.copy()
	hasher.update(password)
	return hasher.digest().encode('hex')
class Api(object):
	@cherrypy.expose
	@cherrypy.tools.allow(methods=('POST',))
	@cherrypy.tools.json_out()
	def plant(self, seed, latitude, longitude, title, expiration, password=None, question=None):
		now = datetime.datetime.utcnow()
		expiration = now+datetime.timedelta(seconds=max(0, min(3600*24*7*2, float(expiration))))
		while True:
			key = ''.join(random.choice(string.letters+string.digits) for _ in xrange(16))
			obj_key = 'obj_%s'%key
			if obj_key not in d:
				break
		d[obj_key] = {
			'created' : now,
			'filename' : seed.filename,
			'contents' : seed.file.read(),
			'content_type' : str(seed.content_type),
			'latitude' : float(latitude),
			'longitude' : float(longitude),
			'title' : title,
			'expiration' : expiration,
			'password' : None if password is None else digest(password),
			'question' : question,
		}
		d.sync()
		return obj_key[4:]
	@cherrypy.expose
	def get(self, key, password=None):
		obj = d['obj_%s'%str(key)]
		if obj['expiration'] < datetime.datetime.utcnow():
			raise KeyError
		if obj['password'] is not None and obj['password'] != digest(password):
			raise ValueError
		cherrypy.response.headers['Content-Type'] = obj['content_type']
		cherrypy.response.headers['Content-Disposition'] = 'inline; filename="'+obj['filename'].replace('\\','\\\\').replace('"','\\"')+'"'
		return cherrypy.lib.static.serve_fileobj(io.BytesIO(obj['contents']))
	@cherrypy.expose
	@cherrypy.tools.json_out()
	def list(self, latitude, longitude, radius='inf'):
		radius = float(radius)
		ret = []
		now = datetime.datetime.utcnow()
		for obj_key, obj in copy.deepcopy(d.items()):
			if obj['expiration'] < now:
				continue
			for field in 'expiration', 'created':
				obj[field] = obj[field].isoformat()
			obj['distance'] = distance(float(latitude), float(longitude), obj['latitude'], obj['longitude'])
			obj['key'] = obj_key[4:]
			del obj['contents']
			if obj['distance'] <= radius:
				ret.append(obj)
		ret.sort(key=lambda obj: obj['distance'])
		return ret
	@cherrypy.expose
	def gc(self):
		now = datetime.datetime.utcnow()
		for obj_key, obj in copy.deepcopy(d.items()):
			if obj['expiration'] < now:
				del d[obj_key]
		d.sync()
if __name__=='__main__':
	d = shelve.open('uploads.shelve')
	try:
		current_dir = os.path.dirname(os.path.abspath(__file__))
		config = {
			'/' : {
				'tools.staticdir.root' : current_dir,
				'tools.staticfile.root' : current_dir,
				'tools.staticfile.on' : True,
				'tools.staticfile.filename' : 'index.html',
				'tools.staticfile.match' : '^/$', # only match the exact url /
			},
			'/static' : {
				'tools.staticdir.on' : True,
				'tools.staticdir.dir' : 'static',
			},
			'/files' : {
				'tools.staticdir.on' : True,
				'tools.staticdir.dir' : 'files',
			},
		}
		#cherrypy.engine.timeout_monitor.unsubscribe()
		#cherrypy.engine.autoreload.unsubscribe()
		cherrypy.quickstart(Api(), config=config)
	finally:
		d.close()
