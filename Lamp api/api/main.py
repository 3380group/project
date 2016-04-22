#!/usr/bin/env python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import webapp2
import os
import json
import jinja2
from datetime import datetime
from google.appengine.ext import db
from google.appengine.api import memcache
from google.appengine.api import search
import logging
template_dir = os.path.join(os.path.dirname(__file__), 'templates')
jinja_env = jinja2.Environment(loader = jinja2.FileSystemLoader(template_dir),
                               autoescape = True)

def render_str(template, **params):
    t = jinja_env.get_template(template)
    return t.render(params)

class BaseHandler(webapp2.RequestHandler):
    def render(self, template, **kw):
        self.response.out.write(self.render_str(template, **kw))

    def write(self, *a, **kw):
        self.response.out.write(*a, **kw)

    def render_str(self, template, **params):
        return render_str(template, **params)

    def initialize(self, *a, **kw):
        webapp2.RequestHandler.initialize(self, *a, **kw)

class User(db.Model):
    status = db.IntegerProperty()
    status_text = db.StringProperty()
    friends = db.ListProperty(int)
    current_location = db.GeoPtProperty(required = True)
    name = db.StringProperty() 
    lastUpdated = db.DateTimeProperty(auto_now=True)
class NewUser(BaseHandler):
    def get(self):
        u = User(
            current_location = "0,0",
            name = "Unknown",
            status = 1,
            status_text = "Just Joined"
            )
        u.put()
        self.write(u.key().id())
class UpdateLocation(BaseHandler):  
    def post(self):
        json_data = self.request.body
        json_data = json.loads(json_data)
        name = json_data["name"]
        uid = json_data["uid"]
        longitude = json_data["longitude"]
        latitude = json_data["latitude"]
        user = User.get_by_id(int(uid))
        if user==None:
            return None
        user.name = name
        user.current_location = str(latitude)+","+str(longitude)
        user.put()
class UpdateStatus(BaseHandler):  
    def post(self):
        json_data = self.request.body
        json_data = json.loads(json_data)
        status_text = json_data["statusText"]
        status = json_data["status"]
        uid = json_data["uid"]
        name = json_data["name"]
        user = User.get_by_id(int(uid))
        if user==None:
            return None
        user.status = status
        user.name = name
        user.status_text = status_text
        user.put()
class GetFriends(BaseHandler):
    def get(self,uid=""):
        user = User.get_by_id(int(uid))
        if user==None:
            return None
        p = {"users":[]}
        for uid in user.friends:
            user = User.get_by_id(int(uid))
            if user != None:
                output = {}
                output["name"] = user.name
                output["status"] = user.status
                output["statusText"] = user.status_text
                output["api_id"] = uid
                output["longitude"] = user.current_location.lon
                output["latitude"] = user.current_location.lat
                output["datetime"] = (user.lastUpdated - datetime(1970,1,1)).total_seconds()
                p["users"].append(output)
        self.write(json.dumps(p))            
class AddUserToFriends(BaseHandler):
    def post(self):
        json_data = self.request.body
        json_data = json.loads(json_data)
        friend = int(json_data["friend"])
        friend = User.get_by_id(int(friend))
        user = int(json_data["user"])
        user = User.get_by_id(int(user))
        if user==None or friend == None:
            return None
        if friend.key().id() not in user.friends: 
            user.friends.append(friend.key().id())
            user.put()
        if user.key().id() not in friend.friends: 
            friend.friends.append(user.key().id())
            friend.put()
class AddFriend(BaseHandler):
    def get(self,Meeting_id=""):
        self.render('base.html', ID = Meeting_id)
app = webapp2.WSGIApplication([
    ('/newuser/',NewUser),
    ('/addusertofriends/',AddUserToFriends),
    ('/addfriend/(.+)',AddFriend),
    ('/getfriends/(.+)',GetFriends),
    ('/updatelocation/',UpdateLocation),
    ('/updatestatus/',UpdateStatus)
], debug=True)
