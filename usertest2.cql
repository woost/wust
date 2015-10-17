match (n) optional match (n)-[r]-() delete n,r;
begin
create (_1109:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:90, `symbol`:"fa fa-flask", `title`:"Problem", `uuid`:"OsWcw6CWRUWm4oBXwsZwEw"})
create (_1110:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:169, `symbol`:"fa fa-crosshairs", `title`:"Goal", `uuid`:"hw_akS_8RYiqk38BNj-f0A"})
create (_1111:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:260, `symbol`:"fa fa-lightbulb-o", `title`:"Idea", `uuid`:"SgA5_nScQGCVsBvdRizTWw"})
create (_1112:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:135, `symbol`:"fa fa-thumbs-o-up", `title`:"Pro", `uuid`:"lWMoGqCORvK6SYDDvxZbdA"})
create (_1113:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:21, `symbol`:"fa fa-thumbs-o-down", `title`:"Contra", `uuid`:"-U2zvSsjSfmqaxmb3wfxow"})
create (_1114:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:301, `symbol`:"fa fa-question", `title`:"Question", `uuid`:"rnIdRqUZQYitADLpBW9_fg"})
create (_1115:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:280, `symbol`:"fa fa-list-ul", `title`:"Task", `uuid`:"AS1RUShiSlmB0p_BmChL-w"})
create (_1116:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:70, `symbol`:"fa fa-arrow-left", `title`:"Cause", `uuid`:"C54Lc-VeSTSz9cbJNjA1OA"})
create (_1117:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:150, `symbol`:"fa fa-check", `title`:"Done", `uuid`:"WqholPfjRdKo2ZfSbFmoMA"})
create (_1118:`USERGROUP`:`UUIDNODE` {`name`:"everyone", `uuid`:"X3sjyvwdSymeX_5ni5NO2A"})
create (_1119:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Bob"})
create (_1120:`USER`:`UUIDNODE` {`name`:"Bob", `uuid`:"MzvTiT8tRr6KeMXIHfExfA"})
create (_1121:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$Y.9958U7jlzj.yRzUvWBPunGAG.eqO32MRnZ2KXTPxjQJdGwBE/lS"})
create (_1122:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:95, `title`:"Urlaub", `uuid`:"oGuMSHatTHOVckzIEqaPsg"})
create (_1123:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026364143, `title`:"Mit welchem Verkehrsmittel soll ich in den Urlaub von Deutschland nach Australien reisen?", `uuid`:"T3TDqpioQ4yOFlM342YvWg", `viewCount`:2})
create (_1124:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026364149, `uuid`:"I87QidTCTZeBBKJ-XxaKUg"})
create (_1125:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"_83CuIyBTHibmMIkBTVJvA", `voteCount`:1})
create (_1126:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:15, `title`:"Arbeit", `uuid`:"fMJWvmz_S0-A99IJ-8gl6g"})
create (_1127:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Es sind knapp 5km", `timestamp`:1445026441099, `title`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `uuid`:"_2QCNaskSYaMMQpVu4FV5w", `viewCount`:3})
create (_1128:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026441100, `uuid`:"YQcQC4A5SdSMgApR5TMjBQ"})
create (_1129:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"khz_WC3vQAOe2sjZD9SFdQ", `voteCount`:1})
create (_1130:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Greta"})
create (_1131:`USER`:`UUIDNODE` {`name`:"Greta", `uuid`:"gMDkFJoLTUqMruv8sHBIGg"})
create (_1132:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$h7azF7eb1MF58dUnADB7Q.R/wHi5w1/6Ip84XzFse9SQy5YGHjWQK"})
create (_1133:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026521069, `title`:"Mit dem Fahrrad", `uuid`:"jDJhPRu2QKqwq4geetfKPQ", `viewCount`:2})
create (_1134:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026521070, `uuid`:"Thlzs5vqTaOTCIsfxv7ddw"})
create (_1135:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026521075, `uuid`:"6w4XtunITkK7Pu5GYmuWYg", `voteCount`:1})
create (_1136:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026529553, `uuid`:"OHeSdKJIRBu6vxWcn_3K_w", `voteCount`:0})
create (_1137:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Barbera"})
create (_1138:`USER`:`UUIDNODE` {`name`:"Barbera", `uuid`:"QmVPPDP1Rq22I9Iby638hg"})
create (_1139:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$zz73dPUaBUZ9lXgq13sDZOAUa7z40ib4RMIFwcAwmN8EDbkeFi3ry"})
create (_1140:`LOGININFO` {`providerID`:"credentials", `providerKey`:"John"})
create (_1141:`USER`:`UUIDNODE` {`name`:"John", `uuid`:"Q1Egz56yTpWX8am3tHDG7A"})
create (_1142:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$nQvv7Cmz8cjTGafc8WCSZepxejuX6WEhYPt02B/HOHnYZTI6Qet3O"})
create (_1143:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026616909, `title`:"Mit dem Bus", `uuid`:"gM2jrcl9TZmPOLHscw6TQQ", `viewCount`:2})
create (_1144:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026616910, `uuid`:"RCUwjyqyRQiC7Qamj9fkxA"})
create (_1145:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026616913, `uuid`:"n-rPl-r4QeqRAiZpMo6f_A", `voteCount`:1})
create (_1146:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026654389, `uuid`:"02uCVb6STLiEo7jm7j2eVQ"})
create (_1147:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:3, `approvalSum`:33, `newDescription`:"Es sind knapp 5km", `newTitle`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `oldDescription`:"Es sind knapp 5km", `oldTitle`:"Mit welchem Verkehrsmittel fahre ich am besten zur Arbeit?", `status`:2, `timestamp`:1445026732925, `uuid`:"MYbyARYoSa-dNZAkvFuI0A"})
create (_1148:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026812280, `uuid`:"g6MA0ADTQnusnnr6c1OP-Q"})
create (_1149:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:47, `title`:"Wust", `uuid`:"Vq73bRudRsey5mePOA35rQ"})
create (_1150:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Wie jedes Jahr wollen wir wieder gemeinsam unseren Betriebsausflug planen. Ich hoffe auf eine rege Beteiligung.", `timestamp`:1445027390947, `title`:"Planung des Betriebsausflugs der Firma Wust", `uuid`:"pu4Uv0yETU6mOPyah5VKTQ", `viewCount`:3})
create (_1151:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027390947, `uuid`:"dFeNCOC5Qg6py-on0eSEEA"})
create (_1152:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"CfPh_5btT4-8_YfqkvzejQ", `voteCount`:1})
create (_1153:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"66CVMHLFRaKW6-kDWULjww", `voteCount`:0})
create (_1154:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027452776, `title`:"Zeitrahmen vereinbaren", `uuid`:"f6MfrKRGRH23XUqF2HG0hQ", `viewCount`:3})
create (_1155:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027452776, `uuid`:"EXKWE372SNyljK693A7nJQ"})
create (_1156:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027452778, `uuid`:"dQeKEezkQ2-d8exYo8hyHw", `voteCount`:1})
create (_1157:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027465953, `title`:"Veranstaltungsort finden", `uuid`:"tQ97mn7aQtO_keYZVb9DMQ", `viewCount`:3})
create (_1158:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027465953, `uuid`:"ByGKO6eNQ0Cd8oX5ePbOZQ"})
create (_1159:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027465954, `uuid`:"SWZFjXzkRMWt-m_Vo6sxaA", `voteCount`:0})
create (_1160:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027488568, `title`:"Unterbringung von ca. 50 Personen", `uuid`:"MpEoD90wS6OwVNDBeaT_3w", `viewCount`:3})
create (_1161:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027488568, `uuid`:"zbLrfEO1SPixVExrbz_bSg"})
create (_1162:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027488569, `uuid`:"eZK--TqNQ7OhbMR6WxRGjQ", `voteCount`:0})
create (_1163:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027582496, `title`:"Vor der Weihnachtsfeier", `uuid`:"fIlt2rfPRGi3jMbWVEE5yg", `viewCount`:2})
create (_1164:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027582496, `uuid`:"MjBXtWkkQi-9KSp2ApqY0w"})
create (_1165:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027582497, `uuid`:"5wJals92R7CA-ZasFwRBZw", `voteCount`:1})
create (_1166:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann haben auch die Programmierer Zeit.", `timestamp`:1445027627490, `title`:"Nach dem 18.11.2015", `uuid`:"9nZGpPKkS6ezJoulZY8LTg", `viewCount`:2})
create (_1167:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027627490, `uuid`:"y9YZFm0IT6atdVB9RC7sPw"})
create (_1168:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027627491, `uuid`:"JjCdOEYLSYOZ11w6WrKLAw", `voteCount`:1})
create (_1169:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann wären es nämlich 60 Personen.", `timestamp`:1445027670437, `title`:"Fehlen bei den 50 Personen nicht die Mitarbeiter der Außenstelle?", `uuid`:"Tvacpm03SU2r7fGx5plCKg", `viewCount`:2})
create (_1170:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027670437, `uuid`:"RPDzaIE-Raqg869FfBK3NQ"})
create (_1171:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027670438, `uuid`:"WmVHJHvsR5OMPoyMIsvoSQ", `voteCount`:0})
create (_1172:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027803521, `uuid`:"vgegYx7UR2yH9PT2DJkFaA"})
create (_1173:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027810605, `uuid`:"WkkM8ZzUQE26HFhAvIbgaQ"})
create (_1174:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027841962, `uuid`:"3X0cixOtQNmgXEv-bOIZQw"})
create (_1175:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027854927, `uuid`:"hyzMzuJORGKAFT37gpBPnA"})
create (_1176:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027884049, `uuid`:"jRnJI8qqRGeeMoqPisrH_A"})
create (_1177:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027913085, `uuid`:"RiSxpI2lRS-Gdus2V1o9rw"})
create (_1178:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:96, `title`:"Haushalt", `uuid`:"gOjgDSXMTaOTsUqcP97z4g"})
create (_1179:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445028527972, `title`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `uuid`:"rLNvZMajTvCg3X1kWw7pQw", `viewCount`:2})
create (_1180:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445028527972, `uuid`:"nL62rPZNSgi46fZMrqP7XA"})
create (_1181:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"yeaTofcWQ-CyUJrLRIzRhA", `voteCount`:0})
create (_1182:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:33, `newTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `status`:2, `timestamp`:1445028536394, `uuid`:"Wa1Qo9NZTtmfk4LhqOgocg"})
create (_1183:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Alice"})
create (_1184:`USER`:`UUIDNODE` {`name`:"Alice", `uuid`:"EwixTQPwQeC-_QrdI5hWlw"})
create (_1185:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$yPQphYhdKZdEuIQZq4gvaeTLrkXWc4cAuRgdqB2FHjn9YY6jHSBSy"})
create (_1186:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `status`:0, `timestamp`:1445028591239, `uuid`:"K1mVV4svQgaB6ZSqWJnQGg"})
create (_1187:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:16, `title`:"Fahrrad", `uuid`:"r8h_bOCFQJmV2HSowxI9Ag"})
create (_1188:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080609909, `title`:"Wie kann ich bei meinem Fahrrad die Schaltung reparieren?", `uuid`:"G-UHYqIuTcuBH94n3mDJZA", `viewCount`:4})
create (_1189:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080609911, `uuid`:"J8bmWYFnSXqkdKyA6tpIvw"})
create (_1190:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"BvJr6X65RhCMHt7MLVWybQ", `voteCount`:0})
create (_1191:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080673124, `title`:"Käten- oder Nabenschaltung?", `uuid`:"oDNVqbATTJ2m71sJF_3JUQ", `viewCount`:2})
create (_1192:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080673125, `uuid`:"Ea1nQmi1QbSQU9TNl8eUXw"})
create (_1193:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445080673129, `uuid`:"BjaCpVrjQw2Hf-V55mmjuw", `voteCount`:0})
create (_1194:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Ketten- oder Nabenschaltung?", `oldTitle`:"Käten- oder Nabenschaltung?", `status`:0, `timestamp`:1445080704405, `uuid`:"z4TtlUHPTa6AycS_UbHyDA"})
create (_1195:`LOGININFO` {`providerID`:"credentials", `providerKey`:"SpamBot"})
create (_1196:`USER`:`UUIDNODE` {`name`:"SpamBot", `uuid`:"URfbHW2pSe2yyN4W3aXq8w"})
create (_1197:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$TqLo6AvqjfcJo9qQTS4QFuHWhIN4ugDQJJSSzQuQsQ32ap1L9Fptq"})
create (_1198:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080823847, `title`:"SPAM", `uuid`:"gfsOdPonTbC62WCD3IWRFA", `viewCount`:2})
create (_1199:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080823848, `uuid`:"s4zwmHJ9QUaHd_oqJnnEsA"})
create (_1200:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445080823849, `uuid`:"bsC4qrmCSNi_qb33qJIM5w", `voteCount`:0})
create _1110-[:`CLASSIFIES`]->_1152
create _1110-[:`CLASSIFIES`]->_1165
create _1110-[:`CLASSIFIES`]->_1162
create _1110-[:`CLASSIFIES`]->_1153
create _1111-[:`CLASSIFIES`]->_1135
create _1111-[:`CLASSIFIES`]->_1168
create _1111-[:`CLASSIFIES`]->_1145
create _1111-[:`CLASSIFIES`]->_1136
create _1114-[:`CLASSIFIES`]->_1193
create _1114-[:`CLASSIFIES`]->_1190
create _1114-[:`CLASSIFIES`]->_1125
create _1114-[:`CLASSIFIES`]->_1129
create _1114-[:`CLASSIFIES`]->_1171
create _1114-[:`CLASSIFIES`]->_1181
create _1115-[:`CLASSIFIES`]->_1159
create _1115-[:`CLASSIFIES`]->_1156
create _1119-[:`HASPASSWORD`]->_1121
create _1120-[:`HASLOGIN`]->_1119
create _1120-[:`MEMBEROF`]->_1118
create _1120-[:`VIEWED` {`timestamp`:1445080704411}]->_1191
create _1120-[:`VIEWED` {`timestamp`:1445080795873}]->_1188
create _1120-[:`VIEWED` {`timestamp`:1445027874563}]->_1133
create _1120-[:`VIEWED` {`timestamp`:1445026829829}]->_1143
create _1120-[:`VIEWED` {`timestamp`:1445028476278}]->_1123
create _1120-[:`VIEWED` {`timestamp`:1445028474800}]->_1127
create _1120-[:`VIEWED` {`timestamp`:1445027911545}]->_1150
create _1120-[:`VIEWED` {`timestamp`:1445027906346}]->_1160
create _1120-[:`VIEWED` {`timestamp`:1445027719725}]->_1157
create _1120-[:`VIEWED` {`timestamp`:1445027731262}]->_1154
create _1120-[:`VIEWED` {`timestamp`:1445028536405}]->_1179
create _1120-[:`VIEWED` {`timestamp`:1445028220917}]->_1169
create _1120-[:`VIEWED` {`timestamp`:1445027877222}]->_1166
create _1120-[:`VIEWED` {`timestamp`:1445027729730}]->_1163
create _1120-[:`HASKARMA` {`karma`:1}]->_1126
create _1120-[:`HASKARMA` {`karma`:1}]->_1149
create _1120-[:`HASKARMA` {`karma`:2}]->_1122
create _1120-[:`VOTES` {`timestamp`:1445027884039, `weight`:1}]->_1168
create _1120-[:`VOTES` {`timestamp`:1445027913075, `weight`:1}]->_1152
create _1120-[:`VOTES` {`timestamp`:1445080704408, `weight`:1}]->_1194
create _1120-[:`VOTES` {`timestamp`:1445026654329, `weight`:1}]->_1135
create _1120-[:`VOTES` {`timestamp`:1445026812265, `weight`:1}]->_1145
create _1120-[:`VOTES` {`timestamp`:1445026732933, `weight`:33}]->_1147
create _1120-[:`VOTES` {`timestamp`:1445028536396, `weight`:33}]->_1182
create _1120-[:`CREATEDSTART`]->_1124
create _1120-[:`CREATEDSTART`]->_1180
create _1120-[:`CREATEDSTART`]->_1128
create _1120-[:`CREATEDSTART`]->_1164
create _1120-[:`KARMALOGSTART`]->_1175
create _1120-[:`KARMALOGSTART`]->_1173
create _1120-[:`KARMALOGSTART`]->_1174
create _1120-[:`UPDATEDSTART`]->_1182
create _1120-[:`UPDATEDSTART`]->_1194
create _1120-[:`UPDATEDSTART`]->_1147
create _1122-[:`TAGSSTART`]->_1125
create _1122-[:`TAGSSTART`]->_1153
create _1124-[:`CREATEDEND`]->_1123
create _1125-[:`TAGSEND`]->_1123
create _1126-[:`TAGSSTART`]->_1129
create _1128-[:`CREATEDEND`]->_1127
create _1129-[:`TAGSEND`]->_1127
create _1130-[:`HASPASSWORD`]->_1132
create _1131-[:`HASLOGIN`]->_1130
create _1131-[:`MEMBEROF`]->_1118
create _1131-[:`VIEWED` {`timestamp`:1445027679182}]->_1169
create _1131-[:`VIEWED` {`timestamp`:1445027683092}]->_1160
create _1131-[:`VIEWED` {`timestamp`:1445027852480}]->_1123
create _1131-[:`VIEWED` {`timestamp`:1445027838835}]->_1127
create _1131-[:`VIEWED` {`timestamp`:1445027833984}]->_1163
create _1131-[:`VIEWED` {`timestamp`:1445026521071}]->_1133
create _1131-[:`VIEWED` {`timestamp`:1445027801301}]->_1154
create _1131-[:`VIEWED` {`timestamp`:1445027687729}]->_1166
create _1131-[:`VIEWED` {`timestamp`:1445027632553}]->_1150
create _1131-[:`VIEWED` {`timestamp`:1445027683738}]->_1157
create _1131-[:`HASKARMA` {`karma`:1}]->_1126
create _1131-[:`HASKARMA` {`karma`:2}]->_1122
create _1131-[:`HASKARMA` {`karma`:1}]->_1149
create _1131-[:`VOTES` {`timestamp`:1445027854916, `weight`:1}]->_1125
create _1131-[:`VOTES` {`timestamp`:1445027841947, `weight`:1}]->_1129
create _1131-[:`VOTES` {`timestamp`:1445027810595, `weight`:1}]->_1165
create _1131-[:`VOTES` {`timestamp`:1445027803506, `weight`:1}]->_1156
create _1131-[:`CREATEDSTART`]->_1170
create _1131-[:`CREATEDSTART`]->_1167
create _1131-[:`CREATEDSTART`]->_1134
create _1131-[:`KARMALOGSTART`]->_1146
create _1131-[:`KARMALOGSTART`]->_1176
create _1133-[:`CONNECTSSTART`]->_1135
create _1133-[:`CONNECTSSTART`]->_1136
create _1134-[:`CREATEDEND`]->_1133
create _1135-[:`CONNECTSEND`]->_1127
create _1136-[:`CONNECTSEND`]->_1123
create _1137-[:`HASPASSWORD`]->_1139
create _1138-[:`HASLOGIN`]->_1137
create _1138-[:`MEMBEROF`]->_1118
create _1138-[:`VIEWED` {`timestamp`:1445027452777}]->_1154
create _1138-[:`VIEWED` {`timestamp`:1445027747867}]->_1150
create _1138-[:`VIEWED` {`timestamp`:1445027488568}]->_1160
create _1138-[:`VIEWED` {`timestamp`:1445027503674}]->_1157
create _1138-[:`VIEWED` {`timestamp`:1445080610074}]->_1188
create _1138-[:`VIEWED` {`timestamp`:1445026616910}]->_1143
create _1138-[:`VIEWED` {`timestamp`:1445026606866}]->_1127
create _1138-[:`HASKARMA` {`karma`:1}]->_1126
create _1138-[:`HASKARMA` {`karma`:2}]->_1149
create _1138-[:`HASKARMA` {`karma`:1}]->_1122
create _1138-[:`CREATEDSTART`]->_1155
create _1138-[:`CREATEDSTART`]->_1189
create _1138-[:`CREATEDSTART`]->_1144
create _1138-[:`CREATEDSTART`]->_1158
create _1138-[:`CREATEDSTART`]->_1161
create _1138-[:`CREATEDSTART`]->_1151
create _1138-[:`KARMALOGSTART`]->_1177
create _1138-[:`KARMALOGSTART`]->_1148
create _1138-[:`KARMALOGSTART`]->_1172
create _1140-[:`HASPASSWORD`]->_1142
create _1141-[:`HASLOGIN`]->_1140
create _1141-[:`MEMBEROF`]->_1118
create _1141-[:`HASKARMA` {`karma`:51}]->_1187
create _1143-[:`CONNECTSSTART`]->_1145
create _1144-[:`CREATEDEND`]->_1143
create _1145-[:`CONNECTSEND`]->_1127
create _1146-[:`LOGONSCOPE` {`currentKarma`:1}]->_1122
create _1146-[:`LOGONSCOPE` {`currentKarma`:1}]->_1126
create _1146-[:`KARMALOGEND`]->_1133
create _1147-[:`UPDATEDEND`]->_1127
create _1148-[:`LOGONSCOPE` {`currentKarma`:1}]->_1126
create _1148-[:`KARMALOGEND`]->_1143
create _1149-[:`TAGSSTART`]->_1152
create _1151-[:`CREATEDEND`]->_1150
create _1152-[:`TAGSEND`]->_1150
create _1153-[:`TAGSEND`]->_1150
create _1154-[:`CONNECTSSTART`]->_1156
create _1155-[:`CREATEDEND`]->_1154
create _1156-[:`CONNECTSEND`]->_1150
create _1157-[:`CONNECTSSTART`]->_1159
create _1158-[:`CREATEDEND`]->_1157
create _1159-[:`CONNECTSEND`]->_1150
create _1160-[:`CONNECTSSTART`]->_1162
create _1161-[:`CREATEDEND`]->_1160
create _1162-[:`CONNECTSEND`]->_1157
create _1163-[:`CONNECTSSTART`]->_1165
create _1164-[:`CREATEDEND`]->_1163
create _1165-[:`CONNECTSEND`]->_1154
create _1166-[:`CONNECTSSTART`]->_1168
create _1167-[:`CREATEDEND`]->_1166
create _1168-[:`CONNECTSEND`]->_1154
create _1169-[:`CONNECTSSTART`]->_1171
create _1170-[:`CREATEDEND`]->_1169
create _1171-[:`CONNECTSEND`]->_1160
create _1172-[:`LOGONSCOPE` {`currentKarma`:1}]->_1149
create _1172-[:`LOGONSCOPE` {`currentKarma`:1}]->_1122
create _1172-[:`KARMALOGEND`]->_1154
create _1173-[:`LOGONSCOPE` {`currentKarma`:1}]->_1122
create _1173-[:`LOGONSCOPE` {`currentKarma`:1}]->_1149
create _1173-[:`KARMALOGEND`]->_1163
create _1174-[:`LOGONSCOPE` {`currentKarma`:1}]->_1126
create _1174-[:`KARMALOGEND`]->_1127
create _1175-[:`LOGONSCOPE` {`currentKarma`:2}]->_1122
create _1175-[:`KARMALOGEND`]->_1123
create _1176-[:`LOGONSCOPE` {`currentKarma`:2}]->_1122
create _1176-[:`LOGONSCOPE` {`currentKarma`:1}]->_1149
create _1176-[:`KARMALOGEND`]->_1166
create _1177-[:`LOGONSCOPE` {`currentKarma`:2}]->_1149
create _1177-[:`KARMALOGEND`]->_1150
create _1178-[:`TAGSSTART`]->_1181
create _1180-[:`CREATEDEND`]->_1179
create _1181-[:`TAGSEND`]->_1179
create _1182-[:`UPDATEDEND`]->_1179
create _1183-[:`HASPASSWORD`]->_1185
create _1184-[:`HASLOGIN`]->_1183
create _1184-[:`MEMBEROF`]->_1118
create _1184-[:`VIEWED` {`timestamp`:1445028605629}]->_1179
create _1184-[:`VIEWED` {`timestamp`:1445080856365}]->_1188
create _1184-[:`VIEWED` {`timestamp`:1445080855569}]->_1191
create _1184-[:`VIEWED` {`timestamp`:1445080850901}]->_1198
create _1184-[:`VOTES` {`timestamp`:1445028605622, `weight`:1}]->_1186
create _1184-[:`CREATEDSTART`]->_1192
create _1184-[:`UPDATEDSTART`]->_1186
create _1186-[:`UPDATEDEND`]->_1179
create _1187-[:`TAGSSTART`]->_1190
create _1189-[:`CREATEDEND`]->_1188
create _1190-[:`TAGSEND`]->_1188
create _1191-[:`CONNECTSSTART`]->_1193
create _1192-[:`CREATEDEND`]->_1191
create _1193-[:`CONNECTSEND`]->_1188
create _1194-[:`UPDATEDEND`]->_1191
create _1195-[:`HASPASSWORD`]->_1197
create _1196-[:`HASLOGIN`]->_1195
create _1196-[:`MEMBEROF`]->_1118
create _1196-[:`VIEWED` {`timestamp`:1445080817847}]->_1188
create _1196-[:`VIEWED` {`timestamp`:1445080823848}]->_1198
create _1196-[:`CREATEDSTART`]->_1199
create _1198-[:`CONNECTSSTART`]->_1200
create _1199-[:`CREATEDEND`]->_1198
create _1200-[:`CONNECTSEND`]->_1188
;
commit
create index on :`CHANGEREQUEST`(`status`);
create index on :`CREATEREQUEST`(`applied`);
create index on :`LOGININFO`(`providerID`);
create index on :`TIMESTAMP`(`timestamp`);
create constraint on (n:`LOGININFO`) assert n.`providerKey` is unique;
create constraint on (n:`TAGLIKE`) assert n.`title` is unique;
create constraint on (n:`TEST`) assert n.`title` is unique;
create constraint on (n:`USERGROUP`) assert n.`name` is unique;
create constraint on (n:`USER`) assert n.`name` is unique;
create constraint on (n:`UUIDNODE`) assert n.`uuid` is unique;
