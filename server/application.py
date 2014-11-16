#!flask/bin/python

# This file is part of Flash Aid.
# Copyright (C) 2014 Siddharth Duddikunta, Steven Zhang, William Yang, Zain Rehmani
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import urllib3
from json import dumps
from flask import Flask
from flask import jsonify
from flask import request
from flask import abort
from flask import make_response
from flask.ext.pymongo import PyMongo
from pymongo import MongoClient
from gcm import GCM

application = Flask(__name__)
client = MongoClient('localhost', 27017)

db = client.quasimoto
http = urllib3.PoolManager()

gcm = GCM("AIzaSyDqU5liN4EmUb8Qil0WNVknH0UMQ5AcvGU")

# creates a user based on json passed in
# json object is in this format {
#   name : String
#   phone : String
#   email : String
#}
@application.route('/users', methods=['POST'])
def make_user():
    params = request.json
    collection = db.users

    if (params and len(params) == 3) :
        new_user = {
            "name" : params["name"],
            "email" : params["email"],
            "phone" : params["phone"]
        }

        new_id = collection.insert(new_user)

        return jsonify({"id" : str(new_id)})
    else :
        abort(400)

# fills out a user's profile information
@application.route('/user/profile', methods=['POST'])
def make_user_profile():
    params = request.json
    collection = db.users

    if (params) :
        email = params['email']

        update_params = {
                "first_responder" : params['first_responder'],
                "cpr" : params['cpr'],
                "aed" : params['aed'],
                "epipen" : params['epipen'],
                "emergency_1_name" : params["emergency_1_name"],
                "emergency_1_phone" : params["emergency_1_phone"],
                "emergency_2_name" : params["emergency_2_name"],
                "emergency_2_phone" : params["emergency_2_phone"]
        }

        ret = collection.update({"email" : email},
                {"$set" : update_params}, False)

        return jsonify(ret);
    else :
        abort(400)

# Obtains the user's GCM id and stores it in the database
@application.route('/user/gcm', methods=['POST'])
def make_gcm_id():
    params = request.json
    collection = db.users

    if (params):
        email = params['email']

        update_params = {
            'gcm_id' : params['id']
        }

        ret = collection.update({"email" : email},
                {"$set" : update_params}, False)

        return jsonify(ret)

# Alert will alert everybody that is relevant so that they can
# respond to the person in need
@application.route('/alerts', methods=['POST'])
def make_alert():
    # use gcm
    params = request.json
    collection = db.users

    alert_query = {}
    if (params['type'] == 'cpr' or params['type'] == 'aed'):
        alert_query = {'$or' : [{'cpr' : True}, {'aed' : True}]}
    elif (params['type'] == 'epipen'):
        alert_query = {'epipen' : True}

    alert_query['first_responder'] = True;
    alert_query['email'] = {'$ne' : params['email']}

    tmp_ids = collection.find(alert_query, {'gcm_id' : 1})
    reg_ids = [];

    for i in range(0, tmp_ids.count()):
        reg_ids.append(tmp_ids[i]['gcm_id'])

    print reg_ids
    if (len(reg_ids)):
        res = gcm.json_request(registration_ids=reg_ids, data=params)
        return jsonify(res)
    else:
        abort(404)

if __name__ == '__main__':
    application.run(debug=True)
