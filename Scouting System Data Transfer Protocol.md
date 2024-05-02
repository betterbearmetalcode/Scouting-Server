Scouting System Data Transfer Protocol

The goal of this protocol is to be backwards and cross-season compatible. If one wanted to make this protocol much more concise one could have the data metrics be specified once and have the data transferred in arrays, but this requires that the whole of the data is transferred and is much more difficult for a human to read. If it is found that this protocol is ridiculous, this could be a solution. 


The protocol is as follows: 
    • Data is a string which is a JSON array. 
    • Each element of this array is a JSON object which represents a robot's performance in a specific match at a specific robot position. 
    • These JSON objects have elements for each field. 
    • The key is the name of the field and the value is another json object. 
        ◦ The key of this final json object is the datatype and the value is the value.
        ◦  The key is a string which represents the datatype. It will be either “0”, representing an integer, “1”, representing a string, or “2” representing a boolean. 

The reason the datatype is represented as a number instead of “Integer” etc. is to reduce the amount of data transferred. As defined here, this protocol is very redundant, but is flexible, backwards compatible and can sustain only a part of the data being transferred without corrupting.
	If you want to add different data metrics, all you have to do is add more stuff to the outer json objects. If for some reason other datatypes are required, there are more integers out there. If the protocol is updated, old data can be read if some of the old data metrics are still present in the new protocol. The ones which are in both transfer fine, if something is removed, then it will be ignored by the new version, and if something is added, a default value can be supplied from the configuration file.


Example of two data entries, for worlds division data there would be around 750, use code view to see formatted version

[
 {
  "TEAM_NUM": {"0": 33},
  "AUTO_AMP_MISSED": {"0": 0},
  "LOST_COMMS": {"2": 0},
  "MATCH_NUM": {"0": 1},
  "A_STOP": {"2": 0},
  "TELE_AMP_MISSED": {"0": 0},
  "AUTO_SPEAKER_MISSED": {"0": 1},
  "TELE_AMP": {"0": 3},
  "TELE_COMMENTS": {"1": "autopath1m2:climbed Maya DeFrance"},
  "TELE_SPEAKER": {"0": 7},
  "ALLIANCE_POS": {"0": 1},
  "SHUTTLED": {"0": 1},
  "AUTO_SPEAKER": {"0": 1},
  "AUTO_AMP": {"0": 0},
  "NOTE_7": {"1": "None"},
  "NOTE_8": {"1": "None"},
  "NOTE_5": {"1": "None"},
  "NOTE_6": {"1": "None"},
  "AMP_RECEIVED": {"0": 0},
  "SPEAKER_RECEIVED": {"0": 0},
  "NOTE_9": {"1": "None"},
  "TELE_SPEAKER_MISSED": {"0": 0},
  "NOTE_3": {"1": "None"},
  "NOTE_4": {"1": "None"},
  "TELE_TRAP": {"0": 0},
  "NOTE_1": {"1": "1m"},
  "NOTE_2": {"1": "2"}
 },
 {
  "TEAM_NUM": {"0": 5705},
  "AUTO_AMP_MISSED": {"0": 0},
  "LOST_COMMS": {"2": 0},
  "MATCH_NUM": {"0": 2},
  "A_STOP": {"2": 0},
  "TELE_AMP_MISSED": {"0": 0},
  "AUTO_SPEAKER_MISSED": {"0": 0},
  "TELE_AMP": {"0": 0},
  "TELE_COMMENTS": {"1": "autopath\nabsentBenjamin Thatcher"},
  "TELE_SPEAKER": {"0": 0},
  "ALLIANCE_POS": {"0": 1},
  "SHUTTLED": {"0": 0},
  "AUTO_SPEAKER": {"0": 0},
  "AUTO_AMP": {"0": 0},
  "NOTE_7": {"1": "None"},
  "NOTE_8": {"1": "None"},
  "NOTE_5": {"1": "None"},
  "NOTE_6": {"1": "None"},
  "AMP_RECEIVED": {"0": 0},
  "SPEAKER_RECEIVED": {"0": 0},
  "NOTE_9": {"1": "None"},
  "TELE_SPEAKER_MISSED": {"0": 0},
  "NOTE_3": {"1": "None"},
  "NOTE_4": {"1": "None"},
  "TELE_TRAP": {"0": 0},
  "NOTE_1": {"1": "None"},
  "NOTE_2": {"1": "None"}
 }]
