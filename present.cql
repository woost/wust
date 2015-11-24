match (n) optional match (n)-[r]-() delete n,r;
begin
create (_985:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:90, `precedence`:20, `symbol`:"fa fa-flask", `title`:"Problem", `uuid`:"OsWcw6CWRUWm4oBXwsZwEw"})
create (_986:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:169, `precedence`:20, `symbol`:"fa fa-crosshairs", `title`:"Goal", `uuid`:"hw_akS_8RYiqk38BNj-f0A"})
create (_987:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:260, `precedence`:20, `symbol`:"fa fa-lightbulb-o", `title`:"Idea", `uuid`:"SgA5_nScQGCVsBvdRizTWw"})
create (_988:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:135, `precedence`:6, `symbol`:"fa fa-thumbs-o-up", `title`:"Pro", `uuid`:"lWMoGqCORvK6SYDDvxZbdA"})
create (_989:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:21, `precedence`:5, `symbol`:"fa fa-thumbs-o-down", `title`:"Contra", `uuid`:"-U2zvSsjSfmqaxmb3wfxow"})
create (_990:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:301, `precedence`:10, `symbol`:"fa fa-question", `title`:"Question", `uuid`:"rnIdRqUZQYitADLpBW9_fg"})
create (_991:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:280, `precedence`:10, `symbol`:"fa fa-list-ul", `title`:"Task", `uuid`:"AS1RUShiSlmB0p_BmChL-w"})
create (_992:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:70, `precedence`:10, `symbol`:"fa fa-arrow-left", `title`:"Cause", `uuid`:"C54Lc-VeSTSz9cbJNjA1OA"})
create (_993:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:150, `precedence`:100, `symbol`:"fa fa-check", `title`:"Done", `uuid`:"WqholPfjRdKo2ZfSbFmoMA"})
create (_994:`USERGROUP`:`UUIDNODE` {`name`:"everyone", `uuid`:"X3sjyvwdSymeX_5ni5NO2A"})
create (_995:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Bob"})
create (_996:`USER`:`UUIDNODE` {`name`:"Bob", `uuid`:"MzvTiT8tRr6KeMXIHfExfA"})
create (_997:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$Y.9958U7jlzj.yRzUvWBPunGAG.eqO32MRnZ2KXTPxjQJdGwBE/lS"})
create (_998:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:95, `title`:"Holiday", `uuid`:"oGuMSHatTHOVckzIEqaPsg"})
create (_999:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026364143, `title`:"How to travel from Germany to Australia?", `uuid`:"T3TDqpioQ4yOFlM342YvWg", `viewCount`:2})
create (_1000:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026364149, `uuid`:"I87QidTCTZeBBKJ-XxaKUg"})
create (_1001:`UUIDNODE`:`VOTABLE`:`TAGS`:`REFERENCE` {`_locked`:false, `uuid`:"_83CuIyBTHibmMIkBTVJvA", `voteCount`:1})
create (_1002:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:15, `title`:"Work", `uuid`:"fMJWvmz_S0-A99IJ-8gl6g"})
create (_1003:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"About 5 miles", `timestamp`:1445026441099, `title`:"How to drive to work?", `uuid`:"_2QCNaskSYaMMQpVu4FV5w", `viewCount`:3})
create (_1004:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026441100, `uuid`:"YQcQC4A5SdSMgApR5TMjBQ"})
create (_1005:`UUIDNODE`:`VOTABLE`:`TAGS`:`REFERENCE` {`_locked`:false, `uuid`:"khz_WC3vQAOe2sjZD9SFdQ", `voteCount`:1})
create (_1006:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Greta"})
create (_1007:`USER`:`UUIDNODE` {`name`:"Greta", `uuid`:"gMDkFJoLTUqMruv8sHBIGg"})
create (_1008:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$h7azF7eb1MF58dUnADB7Q.R/wHi5w1/6Ip84XzFse9SQy5YGHjWQK"})
create (_1009:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026521069, `title`:"By bike", `uuid`:"jDJhPRu2QKqwq4geetfKPQ", `viewCount`:2})
create (_1010:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026521070, `uuid`:"Thlzs5vqTaOTCIsfxv7ddw"})
create (_1011:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445026521075, `uuid`:"6w4XtunITkK7Pu5GYmuWYg", `voteCount`:1})
create (_1012:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445026529553, `uuid`:"OHeSdKJIRBu6vxWcn_3K_w", `voteCount`:0})
create (_1013:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Barbera"})
create (_1014:`USER`:`UUIDNODE` {`name`:"Barbera", `uuid`:"QmVPPDP1Rq22I9Iby638hg"})
create (_1015:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$zz73dPUaBUZ9lXgq13sDZOAUa7z40ib4RMIFwcAwmN8EDbkeFi3ry"})
create (_1016:`LOGININFO` {`providerID`:"credentials", `providerKey`:"John"})
create (_1017:`USER`:`UUIDNODE` {`name`:"John", `uuid`:"Q1Egz56yTpWX8am3tHDG7A"})
create (_1018:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$nQvv7Cmz8cjTGafc8WCSZepxejuX6WEhYPt02B/HOHnYZTI6Qet3O"})
create (_1019:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026616909, `title`:"Take the bus", `uuid`:"gM2jrcl9TZmPOLHscw6TQQ", `viewCount`:2})
create (_1020:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026616910, `uuid`:"RCUwjyqyRQiC7Qamj9fkxA"})
create (_1021:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445026616913, `uuid`:"n-rPl-r4QeqRAiZpMo6f_A", `voteCount`:1})
create (_1022:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026654389, `uuid`:"02uCVb6STLiEo7jm7j2eVQ"})
create (_1023:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`VOTABLE`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:3, `approvalSum`:33, `newDescription`:"Es sind knapp 5km", `newTitle`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `oldDescription`:"Es sind knapp 5km", `oldTitle`:"Mit welchem Verkehrsmittel fahre ich am besten zur Arbeit?", `status`:2, `timestamp`:1445026732925, `uuid`:"MYbyARYoSa-dNZAkvFuI0A"})
create (_1024:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026812280, `uuid`:"g6MA0ADTQnusnnr6c1OP-Q"})
create (_1025:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:47, `title`:"Wust", `uuid`:"Vq73bRudRsey5mePOA35rQ"})
create (_1026:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Wie jedes Jahr wollen wir wieder gemeinsam unseren Betriebsausflug planen. Ich hoffe auf eine rege Beteiligung.", `timestamp`:1445027390947, `title`:"Planung des Betriebsausflugs der Firma Wust", `uuid`:"pu4Uv0yETU6mOPyah5VKTQ", `viewCount`:3})
create (_1027:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027390947, `uuid`:"dFeNCOC5Qg6py-on0eSEEA"})
create (_1028:`UUIDNODE`:`VOTABLE`:`TAGS`:`REFERENCE` {`_locked`:false, `uuid`:"CfPh_5btT4-8_YfqkvzejQ", `voteCount`:1})
create (_1029:`UUIDNODE`:`VOTABLE`:`TAGS`:`REFERENCE` {`_locked`:false, `uuid`:"66CVMHLFRaKW6-kDWULjww", `voteCount`:0})
create (_1030:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027452776, `title`:"Zeitrahmen vereinbaren", `uuid`:"f6MfrKRGRH23XUqF2HG0hQ", `viewCount`:3})
create (_1031:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027452776, `uuid`:"EXKWE372SNyljK693A7nJQ"})
create (_1032:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445027452778, `uuid`:"dQeKEezkQ2-d8exYo8hyHw", `voteCount`:1})
create (_1033:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027465953, `title`:"Veranstaltungsort finden", `uuid`:"tQ97mn7aQtO_keYZVb9DMQ", `viewCount`:3})
create (_1034:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027465953, `uuid`:"ByGKO6eNQ0Cd8oX5ePbOZQ"})
create (_1035:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445027465954, `uuid`:"SWZFjXzkRMWt-m_Vo6sxaA", `voteCount`:0})
create (_1036:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027488568, `title`:"Unterbringung von ca. 50 Personen", `uuid`:"MpEoD90wS6OwVNDBeaT_3w", `viewCount`:3})
create (_1037:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027488568, `uuid`:"zbLrfEO1SPixVExrbz_bSg"})
create (_1038:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445027488569, `uuid`:"eZK--TqNQ7OhbMR6WxRGjQ", `voteCount`:0})
create (_1039:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027582496, `title`:"Vor der Weihnachtsfeier", `uuid`:"fIlt2rfPRGi3jMbWVEE5yg", `viewCount`:2})
create (_1040:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027582496, `uuid`:"MjBXtWkkQi-9KSp2ApqY0w"})
create (_1041:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445027582497, `uuid`:"5wJals92R7CA-ZasFwRBZw", `voteCount`:1})
create (_1042:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann haben auch die Programmierer Zeit.", `timestamp`:1445027627490, `title`:"Nach dem 18.11.2015", `uuid`:"9nZGpPKkS6ezJoulZY8LTg", `viewCount`:2})
create (_1043:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027627490, `uuid`:"y9YZFm0IT6atdVB9RC7sPw"})
create (_1044:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445027627491, `uuid`:"JjCdOEYLSYOZ11w6WrKLAw", `voteCount`:1})
create (_1045:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann wären es nämlich 60 Personen.", `timestamp`:1445027670437, `title`:"Fehlen bei den 50 Personen nicht die Mitarbeiter der Außenstelle?", `uuid`:"Tvacpm03SU2r7fGx5plCKg", `viewCount`:2})
create (_1046:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027670437, `uuid`:"RPDzaIE-Raqg869FfBK3NQ"})
create (_1047:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445027670438, `uuid`:"WmVHJHvsR5OMPoyMIsvoSQ", `voteCount`:0})
create (_1048:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027803521, `uuid`:"vgegYx7UR2yH9PT2DJkFaA"})
create (_1049:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027810605, `uuid`:"WkkM8ZzUQE26HFhAvIbgaQ"})
create (_1050:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027841962, `uuid`:"3X0cixOtQNmgXEv-bOIZQw"})
create (_1051:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027854927, `uuid`:"hyzMzuJORGKAFT37gpBPnA"})
create (_1052:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027884049, `uuid`:"jRnJI8qqRGeeMoqPisrH_A"})
create (_1053:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027913085, `uuid`:"RiSxpI2lRS-Gdus2V1o9rw"})
create (_1054:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:96, `title`:"Household", `uuid`:"gOjgDSXMTaOTsUqcP97z4g"})
create (_1055:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445028527972, `title`:"How do I I remove grease stains?", `uuid`:"rLNvZMajTvCg3X1kWw7pQw", `viewCount`:2})
create (_1056:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445028527972, `uuid`:"nL62rPZNSgi46fZMrqP7XA"})
create (_1057:`UUIDNODE`:`VOTABLE`:`TAGS`:`REFERENCE` {`_locked`:false, `uuid`:"yeaTofcWQ-CyUJrLRIzRhA", `voteCount`:0})
create (_1058:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`VOTABLE`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:33, `newTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `status`:2, `timestamp`:1445028536394, `uuid`:"Wa1Qo9NZTtmfk4LhqOgocg"})
create (_1059:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Alice"})
create (_1060:`USER`:`UUIDNODE` {`name`:"Alice", `uuid`:"EwixTQPwQeC-_QrdI5hWlw"})
create (_1061:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$yPQphYhdKZdEuIQZq4gvaeTLrkXWc4cAuRgdqB2FHjn9YY6jHSBSy"})
create (_1062:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`VOTABLE`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `status`:0, `timestamp`:1445028591239, `uuid`:"K1mVV4svQgaB6ZSqWJnQGg"})
create (_1063:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:16, `title`:"Bike", `uuid`:"r8h_bOCFQJmV2HSowxI9Ag"})
create (_1064:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080609909, `title`:"How can I repair the gear of my bike?", `uuid`:"G-UHYqIuTcuBH94n3mDJZA", `viewCount`:4})
create (_1065:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080609911, `uuid`:"J8bmWYFnSXqkdKyA6tpIvw"})
create (_1066:`UUIDNODE`:`VOTABLE`:`TAGS`:`REFERENCE` {`_locked`:false, `uuid`:"BvJr6X65RhCMHt7MLVWybQ", `voteCount`:0})
create (_1067:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080673124, `title`:"derailleur or hub gear?", `uuid`:"oDNVqbATTJ2m71sJF_3JUQ", `viewCount`:2})
create (_1068:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080673125, `uuid`:"Ea1nQmi1QbSQU9TNl8eUXw"})
create (_1069:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445080673129, `uuid`:"BjaCpVrjQw2Hf-V55mmjuw", `voteCount`:0})
create (_1070:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`VOTABLE`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Ketten- oder Nabenschaltung?", `oldTitle`:"Käten- oder Nabenschaltung?", `status`:0, `timestamp`:1445080704405, `uuid`:"z4TtlUHPTa6AycS_UbHyDA"})
create (_1071:`LOGININFO` {`providerID`:"credentials", `providerKey`:"SpamBot"})
create (_1072:`USER`:`UUIDNODE` {`name`:"SpamBot", `uuid`:"URfbHW2pSe2yyN4W3aXq8w"})
create (_1073:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$TqLo6AvqjfcJo9qQTS4QFuHWhIN4ugDQJJSSzQuQsQ32ap1L9Fptq"})
create (_1074:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080823847, `title`:"Earn 1000 dollars every day without doing anything.", `uuid`:"gfsOdPonTbC62WCD3IWRFA", `viewCount`:2})
create (_1075:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080823848, `uuid`:"s4zwmHJ9QUaHd_oqJnnEsA"})
create (_1076:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`REFERENCE`:`CONNECTS` {`_locked`:false, `timestamp`:1445080823849, `uuid`:"bsC4qrmCSNi_qb33qJIM5w", `voteCount`:0})
create _986-[:`CLASSIFIES`]->_1029
create _986-[:`CLASSIFIES`]->_1038
create _986-[:`CLASSIFIES`]->_1041
create _986-[:`CLASSIFIES`]->_1028
create _987-[:`CLASSIFIES`]->_1012
create _987-[:`CLASSIFIES`]->_1021
create _987-[:`CLASSIFIES`]->_1044
create _987-[:`CLASSIFIES`]->_1011
create _990-[:`CLASSIFIES`]->_1005
create _990-[:`CLASSIFIES`]->_1001
create _990-[:`CLASSIFIES`]->_1057
create _990-[:`CLASSIFIES`]->_1047
create _990-[:`CLASSIFIES`]->_1066
create _990-[:`CLASSIFIES`]->_1069
create _991-[:`CLASSIFIES`]->_1035
create _991-[:`CLASSIFIES`]->_1032
create _995-[:`HASPASSWORD`]->_997
create _996-[:`HASLOGIN`]->_995
create _996-[:`MEMBEROF`]->_994
create _996-[:`VIEWED` {`timestamp`:1445027731262}]->_1030
create _996-[:`VIEWED` {`timestamp`:1445028536405}]->_1055
create _996-[:`VIEWED` {`timestamp`:1445027906346}]->_1036
create _996-[:`VIEWED` {`timestamp`:1445027719725}]->_1033
create _996-[:`VIEWED` {`timestamp`:1445028474800}]->_1003
create _996-[:`VIEWED` {`timestamp`:1445027911545}]->_1026
create _996-[:`VIEWED` {`timestamp`:1445026829829}]->_1019
create _996-[:`VIEWED` {`timestamp`:1445028476278}]->_999
create _996-[:`VIEWED` {`timestamp`:1445080795873}]->_1064
create _996-[:`VIEWED` {`timestamp`:1445027874563}]->_1009
create _996-[:`VIEWED` {`timestamp`:1445080704411}]->_1067
create _996-[:`VIEWED` {`timestamp`:1445027729730}]->_1039
create _996-[:`VIEWED` {`timestamp`:1445027877222}]->_1042
create _996-[:`VIEWED` {`timestamp`:1445028220917}]->_1045
create _996-[:`VOTES` {`timestamp`:1445028536396, `weight`:33}]->_1058
create _996-[:`VOTES` {`timestamp`:1445026732933, `weight`:33}]->_1023
create _996-[:`VOTES` {`timestamp`:1445026812265, `weight`:1}]->_1021
create _996-[:`VOTES` {`timestamp`:1445026654329, `weight`:1}]->_1011
create _996-[:`VOTES` {`timestamp`:1445080704408, `weight`:1}]->_1070
create _996-[:`VOTES` {`timestamp`:1445027913075, `weight`:1}]->_1028
create _996-[:`VOTES` {`timestamp`:1445027884039, `weight`:1}]->_1044
create _996-[:`HASKARMA` {`karma`:2}]->_998
create _996-[:`HASKARMA` {`karma`:1}]->_1025
create _996-[:`HASKARMA` {`karma`:1}]->_1002
create _996-[:`CREATEDSTART`]->_1004
create _996-[:`CREATEDSTART`]->_1056
create _996-[:`CREATEDSTART`]->_1000
create _996-[:`CREATEDSTART`]->_1040
create _996-[:`UPDATEDSTART`]->_1058
create _996-[:`UPDATEDSTART`]->_1070
create _996-[:`UPDATEDSTART`]->_1023
create _996-[:`KARMALOGSTART`]->_1051
create _996-[:`KARMALOGSTART`]->_1049
create _996-[:`KARMALOGSTART`]->_1050
create _998-[:`TAGSSTART`]->_1029
create _998-[:`TAGSSTART`]->_1001
create _1000-[:`CREATEDEND`]->_999
create _1001-[:`TAGSEND`]->_999
create _1002-[:`TAGSSTART`]->_1005
create _1004-[:`CREATEDEND`]->_1003
create _1005-[:`TAGSEND`]->_1003
create _1006-[:`HASPASSWORD`]->_1008
create _1007-[:`HASLOGIN`]->_1006
create _1007-[:`MEMBEROF`]->_994
create _1007-[:`VIEWED` {`timestamp`:1445027632553}]->_1026
create _1007-[:`VIEWED` {`timestamp`:1445027687729}]->_1042
create _1007-[:`VIEWED` {`timestamp`:1445027683738}]->_1033
create _1007-[:`VIEWED` {`timestamp`:1445027679182}]->_1045
create _1007-[:`VIEWED` {`timestamp`:1445027852480}]->_999
create _1007-[:`VIEWED` {`timestamp`:1445027683092}]->_1036
create _1007-[:`VIEWED` {`timestamp`:1445027833984}]->_1039
create _1007-[:`VIEWED` {`timestamp`:1445027838835}]->_1003
create _1007-[:`VIEWED` {`timestamp`:1445027801301}]->_1030
create _1007-[:`VIEWED` {`timestamp`:1445026521071}]->_1009
create _1007-[:`VOTES` {`timestamp`:1445027810595, `weight`:1}]->_1041
create _1007-[:`VOTES` {`timestamp`:1445027803506, `weight`:1}]->_1032
create _1007-[:`VOTES` {`timestamp`:1445027841947, `weight`:1}]->_1005
create _1007-[:`VOTES` {`timestamp`:1445027854916, `weight`:1}]->_1001
create _1007-[:`HASKARMA` {`karma`:1}]->_1002
create _1007-[:`HASKARMA` {`karma`:1}]->_1025
create _1007-[:`HASKARMA` {`karma`:2}]->_998
create _1007-[:`CREATEDSTART`]->_1046
create _1007-[:`CREATEDSTART`]->_1043
create _1007-[:`CREATEDSTART`]->_1010
create _1007-[:`KARMALOGSTART`]->_1052
create _1007-[:`KARMALOGSTART`]->_1022
create _1009-[:`CONNECTSSTART`]->_1012
create _1009-[:`CONNECTSSTART`]->_1011
create _1010-[:`CREATEDEND`]->_1009
create _1011-[:`CONNECTSEND`]->_1003
create _1012-[:`CONNECTSEND`]->_999
create _1013-[:`HASPASSWORD`]->_1015
create _1014-[:`HASLOGIN`]->_1013
create _1014-[:`MEMBEROF`]->_994
create _1014-[:`VIEWED` {`timestamp`:1445027452777}]->_1030
create _1014-[:`VIEWED` {`timestamp`:1445080610074}]->_1064
create _1014-[:`VIEWED` {`timestamp`:1445027503674}]->_1033
create _1014-[:`VIEWED` {`timestamp`:1445027488568}]->_1036
create _1014-[:`VIEWED` {`timestamp`:1445027747867}]->_1026
create _1014-[:`VIEWED` {`timestamp`:1445026606866}]->_1003
create _1014-[:`VIEWED` {`timestamp`:1445026616910}]->_1019
create _1014-[:`HASKARMA` {`karma`:1}]->_998
create _1014-[:`HASKARMA` {`karma`:2}]->_1025
create _1014-[:`HASKARMA` {`karma`:1}]->_1002
create _1014-[:`CREATEDSTART`]->_1020
create _1014-[:`CREATEDSTART`]->_1065
create _1014-[:`CREATEDSTART`]->_1031
create _1014-[:`CREATEDSTART`]->_1027
create _1014-[:`CREATEDSTART`]->_1037
create _1014-[:`CREATEDSTART`]->_1034
create _1014-[:`KARMALOGSTART`]->_1053
create _1014-[:`KARMALOGSTART`]->_1048
create _1014-[:`KARMALOGSTART`]->_1024
create _1016-[:`HASPASSWORD`]->_1018
create _1017-[:`HASLOGIN`]->_1016
create _1017-[:`MEMBEROF`]->_994
create _1017-[:`HASKARMA` {`karma`:51}]->_1063
create _1019-[:`CONNECTSSTART`]->_1021
create _1020-[:`CREATEDEND`]->_1019
create _1021-[:`CONNECTSEND`]->_1003
create _1022-[:`LOGONSCOPE` {`currentKarma`:1}]->_1002
create _1022-[:`LOGONSCOPE` {`currentKarma`:1}]->_998
create _1022-[:`KARMALOGEND`]->_1009
create _1023-[:`UPDATEDEND`]->_1003
create _1024-[:`LOGONSCOPE` {`currentKarma`:1}]->_1002
create _1024-[:`KARMALOGEND`]->_1019
create _1025-[:`TAGSSTART`]->_1028
create _1027-[:`CREATEDEND`]->_1026
create _1028-[:`TAGSEND`]->_1026
create _1029-[:`TAGSEND`]->_1026
create _1030-[:`CONNECTSSTART`]->_1032
create _1031-[:`CREATEDEND`]->_1030
create _1032-[:`CONNECTSEND`]->_1026
create _1033-[:`CONNECTSSTART`]->_1035
create _1034-[:`CREATEDEND`]->_1033
create _1035-[:`CONNECTSEND`]->_1026
create _1036-[:`CONNECTSSTART`]->_1038
create _1037-[:`CREATEDEND`]->_1036
create _1038-[:`CONNECTSEND`]->_1033
create _1039-[:`CONNECTSSTART`]->_1041
create _1040-[:`CREATEDEND`]->_1039
create _1041-[:`CONNECTSEND`]->_1030
create _1042-[:`CONNECTSSTART`]->_1044
create _1043-[:`CREATEDEND`]->_1042
create _1044-[:`CONNECTSEND`]->_1030
create _1045-[:`CONNECTSSTART`]->_1047
create _1046-[:`CREATEDEND`]->_1045
create _1047-[:`CONNECTSEND`]->_1036
create _1048-[:`LOGONSCOPE` {`currentKarma`:1}]->_1025
create _1048-[:`LOGONSCOPE` {`currentKarma`:1}]->_998
create _1048-[:`KARMALOGEND`]->_1030
create _1049-[:`LOGONSCOPE` {`currentKarma`:1}]->_1025
create _1049-[:`LOGONSCOPE` {`currentKarma`:1}]->_998
create _1049-[:`KARMALOGEND`]->_1039
create _1050-[:`LOGONSCOPE` {`currentKarma`:1}]->_1002
create _1050-[:`KARMALOGEND`]->_1003
create _1051-[:`LOGONSCOPE` {`currentKarma`:2}]->_998
create _1051-[:`KARMALOGEND`]->_999
create _1052-[:`LOGONSCOPE` {`currentKarma`:1}]->_1025
create _1052-[:`LOGONSCOPE` {`currentKarma`:2}]->_998
create _1052-[:`KARMALOGEND`]->_1042
create _1053-[:`LOGONSCOPE` {`currentKarma`:2}]->_1025
create _1053-[:`KARMALOGEND`]->_1026
create _1054-[:`TAGSSTART`]->_1057
create _1056-[:`CREATEDEND`]->_1055
create _1057-[:`TAGSEND`]->_1055
create _1058-[:`UPDATEDEND`]->_1055
create _1059-[:`HASPASSWORD`]->_1061
create _1060-[:`HASLOGIN`]->_1059
create _1060-[:`MEMBEROF`]->_994
create _1060-[:`VIEWED` {`timestamp`:1445028605629}]->_1055
create _1060-[:`VIEWED` {`timestamp`:1445080856365}]->_1064
create _1060-[:`VIEWED` {`timestamp`:1445080855569}]->_1067
create _1060-[:`VIEWED` {`timestamp`:1445080850901}]->_1074
create _1060-[:`VOTES` {`timestamp`:1445028605622, `weight`:1}]->_1062
create _1060-[:`CREATEDSTART`]->_1068
create _1060-[:`UPDATEDSTART`]->_1062
create _1062-[:`UPDATEDEND`]->_1055
create _1063-[:`TAGSSTART`]->_1066
create _1065-[:`CREATEDEND`]->_1064
create _1066-[:`TAGSEND`]->_1064
create _1067-[:`CONNECTSSTART`]->_1069
create _1068-[:`CREATEDEND`]->_1067
create _1069-[:`CONNECTSEND`]->_1064
create _1070-[:`UPDATEDEND`]->_1067
create _1071-[:`HASPASSWORD`]->_1073
create _1072-[:`HASLOGIN`]->_1071
create _1072-[:`MEMBEROF`]->_994
create _1072-[:`VIEWED` {`timestamp`:1445080823848}]->_1074
create _1072-[:`VIEWED` {`timestamp`:1445080817847}]->_1064
create _1072-[:`CREATEDSTART`]->_1075
create _1074-[:`CONNECTSSTART`]->_1076
create _1075-[:`CREATEDEND`]->_1074
create _1076-[:`CONNECTSEND`]->_1064
;
commit
create index on :`CHANGEREQUEST`(`status`)
create index on :`CREATEREQUEST`(`applied`)
create index on :`LOGININFO`(`providerID`)
create index on :`TIMESTAMP`(`timestamp`)
create constraint on (n:`LOGININFO`) assert n.`providerKey` is unique
create constraint on (n:`TAGLIKE`) assert n.`title` is unique
create constraint on (n:`TEST`) assert n.`title` is unique
create constraint on (n:`USERGROUP`) assert n.`name` is unique
create constraint on (n:`USER`) assert n.`name` is unique
create constraint on (n:`UUIDNODE`) assert n.`uuid` is unique
