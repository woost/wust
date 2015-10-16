begin
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
create (_10:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:90, `symbol`:"fa fa-flask", `title`:"Problem", `uuid`:"OsWcw6CWRUWm4oBXwsZwEw"})
create (_11:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:169, `symbol`:"fa fa-crosshairs", `title`:"Goal", `uuid`:"hw_akS_8RYiqk38BNj-f0A"})
create (_12:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:260, `symbol`:"fa fa-lightbulb-o", `title`:"Idea", `uuid`:"SgA5_nScQGCVsBvdRizTWw"})
create (_13:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:135, `symbol`:"fa fa-thumbs-o-up", `title`:"Pro", `uuid`:"lWMoGqCORvK6SYDDvxZbdA"})
create (_14:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:21, `symbol`:"fa fa-thumbs-o-down", `title`:"Contra", `uuid`:"-U2zvSsjSfmqaxmb3wfxow"})
create (_15:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:301, `symbol`:"fa fa-question", `title`:"Question", `uuid`:"rnIdRqUZQYitADLpBW9_fg"})
create (_16:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:280, `symbol`:"fa fa-list-ul", `title`:"Task", `uuid`:"AS1RUShiSlmB0p_BmChL-w"})
create (_17:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:70, `symbol`:"fa fa-arrow-left", `title`:"Cause", `uuid`:"C54Lc-VeSTSz9cbJNjA1OA"})
create (_18:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:150, `symbol`:"fa fa-check", `title`:"Done", `uuid`:"WqholPfjRdKo2ZfSbFmoMA"})
create (_19:`USERGROUP`:`UUIDNODE` {`name`:"everyone", `uuid`:"X3sjyvwdSymeX_5ni5NO2A"})
create (_20:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Bob"})
create (_21:`USER`:`UUIDNODE` {`name`:"Bob", `uuid`:"MzvTiT8tRr6KeMXIHfExfA"})
create (_22:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$Y.9958U7jlzj.yRzUvWBPunGAG.eqO32MRnZ2KXTPxjQJdGwBE/lS"})
create (_23:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:95, `title`:"Urlaub", `uuid`:"oGuMSHatTHOVckzIEqaPsg"})
create (_24:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026364143, `title`:"Mit welchem Verkehrsmittel soll ich in den Urlaub von Deutschland nach Australien reisen?", `uuid`:"T3TDqpioQ4yOFlM342YvWg", `viewCount`:2})
create (_25:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026364149, `uuid`:"I87QidTCTZeBBKJ-XxaKUg"})
create (_26:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"_83CuIyBTHibmMIkBTVJvA", `voteCount`:1})
create (_27:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:15, `title`:"Arbeit", `uuid`:"fMJWvmz_S0-A99IJ-8gl6g"})
create (_28:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Es sind knapp 5km", `timestamp`:1445026441099, `title`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `uuid`:"_2QCNaskSYaMMQpVu4FV5w", `viewCount`:3})
create (_29:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026441100, `uuid`:"YQcQC4A5SdSMgApR5TMjBQ"})
create (_30:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"khz_WC3vQAOe2sjZD9SFdQ", `voteCount`:1})
create (_31:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Greta"})
create (_32:`USER`:`UUIDNODE` {`name`:"Greta", `uuid`:"gMDkFJoLTUqMruv8sHBIGg"})
create (_33:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$h7azF7eb1MF58dUnADB7Q.R/wHi5w1/6Ip84XzFse9SQy5YGHjWQK"})
create (_34:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026521069, `title`:"Mit dem Fahrrad", `uuid`:"jDJhPRu2QKqwq4geetfKPQ", `viewCount`:2})
create (_35:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026521070, `uuid`:"Thlzs5vqTaOTCIsfxv7ddw"})
create (_36:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026521075, `uuid`:"6w4XtunITkK7Pu5GYmuWYg", `voteCount`:1})
create (_37:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026529553, `uuid`:"OHeSdKJIRBu6vxWcn_3K_w", `voteCount`:0})
create (_38:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Barbera"})
create (_39:`USER`:`UUIDNODE` {`name`:"Barbera", `uuid`:"QmVPPDP1Rq22I9Iby638hg"})
create (_40:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$zz73dPUaBUZ9lXgq13sDZOAUa7z40ib4RMIFwcAwmN8EDbkeFi3ry"})
create (_41:`LOGININFO` {`providerID`:"credentials", `providerKey`:"John"})
create (_42:`USER`:`UUIDNODE` {`name`:"John", `uuid`:"Q1Egz56yTpWX8am3tHDG7A"})
create (_43:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$nQvv7Cmz8cjTGafc8WCSZepxejuX6WEhYPt02B/HOHnYZTI6Qet3O"})
create (_44:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026616909, `title`:"Mit dem Bus", `uuid`:"gM2jrcl9TZmPOLHscw6TQQ", `viewCount`:2})
create (_45:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026616910, `uuid`:"RCUwjyqyRQiC7Qamj9fkxA"})
create (_46:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026616913, `uuid`:"n-rPl-r4QeqRAiZpMo6f_A", `voteCount`:1})
create (_47:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026654389, `uuid`:"02uCVb6STLiEo7jm7j2eVQ"})
create (_48:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:3, `approvalSum`:33, `newDescription`:"Es sind knapp 5km", `newTitle`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `oldDescription`:"Es sind knapp 5km", `oldTitle`:"Mit welchem Verkehrsmittel fahre ich am besten zur Arbeit?", `status`:2, `timestamp`:1445026732925, `uuid`:"MYbyARYoSa-dNZAkvFuI0A"})
create (_49:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026812280, `uuid`:"g6MA0ADTQnusnnr6c1OP-Q"})
create (_50:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:47, `title`:"Wust", `uuid`:"Vq73bRudRsey5mePOA35rQ"})
create (_51:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Wie jedes Jahr wollen wir wieder gemeinsam unseren Betriebsausflug planen. Ich hoffe auf eine rege Beteiligung.", `timestamp`:1445027390947, `title`:"Planung des Betriebsausflugs der Firma Wust", `uuid`:"pu4Uv0yETU6mOPyah5VKTQ", `viewCount`:3})
create (_52:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027390947, `uuid`:"dFeNCOC5Qg6py-on0eSEEA"})
create (_53:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"CfPh_5btT4-8_YfqkvzejQ", `voteCount`:1})
create (_54:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"66CVMHLFRaKW6-kDWULjww", `voteCount`:0})
create (_55:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027452776, `title`:"Zeitrahmen vereinbaren", `uuid`:"f6MfrKRGRH23XUqF2HG0hQ", `viewCount`:3})
create (_56:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027452776, `uuid`:"EXKWE372SNyljK693A7nJQ"})
create (_57:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027452778, `uuid`:"dQeKEezkQ2-d8exYo8hyHw", `voteCount`:1})
create (_58:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027465953, `title`:"Veranstaltungsort finden", `uuid`:"tQ97mn7aQtO_keYZVb9DMQ", `viewCount`:3})
create (_59:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027465953, `uuid`:"ByGKO6eNQ0Cd8oX5ePbOZQ"})
create (_60:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027465954, `uuid`:"SWZFjXzkRMWt-m_Vo6sxaA", `voteCount`:0})
create (_61:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027488568, `title`:"Unterbringung von ca. 50 Personen", `uuid`:"MpEoD90wS6OwVNDBeaT_3w", `viewCount`:3})
create (_62:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027488568, `uuid`:"zbLrfEO1SPixVExrbz_bSg"})
create (_63:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027488569, `uuid`:"eZK--TqNQ7OhbMR6WxRGjQ", `voteCount`:0})
create (_64:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027582496, `title`:"Vor der Weihnachtsfeier", `uuid`:"fIlt2rfPRGi3jMbWVEE5yg", `viewCount`:2})
create (_65:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027582496, `uuid`:"MjBXtWkkQi-9KSp2ApqY0w"})
create (_66:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027582497, `uuid`:"5wJals92R7CA-ZasFwRBZw", `voteCount`:1})
create (_67:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann haben auch die Programmierer Zeit.", `timestamp`:1445027627490, `title`:"Nach dem 18.11.2015", `uuid`:"9nZGpPKkS6ezJoulZY8LTg", `viewCount`:2})
create (_68:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027627490, `uuid`:"y9YZFm0IT6atdVB9RC7sPw"})
create (_69:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027627491, `uuid`:"JjCdOEYLSYOZ11w6WrKLAw", `voteCount`:1})
create (_70:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann wären es nämlich 60 Personen.", `timestamp`:1445027670437, `title`:"Fehlen bei den 50 Personen nicht die Mitarbeiter der Außenstelle?", `uuid`:"Tvacpm03SU2r7fGx5plCKg", `viewCount`:2})
create (_71:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027670437, `uuid`:"RPDzaIE-Raqg869FfBK3NQ"})
create (_72:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027670438, `uuid`:"WmVHJHvsR5OMPoyMIsvoSQ", `voteCount`:0})
create (_73:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027803521, `uuid`:"vgegYx7UR2yH9PT2DJkFaA"})
create (_74:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027810605, `uuid`:"WkkM8ZzUQE26HFhAvIbgaQ"})
create (_75:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027841962, `uuid`:"3X0cixOtQNmgXEv-bOIZQw"})
create (_76:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027854927, `uuid`:"hyzMzuJORGKAFT37gpBPnA"})
create (_77:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027884049, `uuid`:"jRnJI8qqRGeeMoqPisrH_A"})
create (_78:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027913085, `uuid`:"RiSxpI2lRS-Gdus2V1o9rw"})
create (_79:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:96, `title`:"Haushalt", `uuid`:"gOjgDSXMTaOTsUqcP97z4g"})
create (_80:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445028527972, `title`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `uuid`:"rLNvZMajTvCg3X1kWw7pQw", `viewCount`:2})
create (_81:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445028527972, `uuid`:"nL62rPZNSgi46fZMrqP7XA"})
create (_82:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"yeaTofcWQ-CyUJrLRIzRhA", `voteCount`:0})
create (_83:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:33, `newTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `status`:2, `timestamp`:1445028536394, `uuid`:"Wa1Qo9NZTtmfk4LhqOgocg"})
create (_84:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Alice"})
create (_85:`USER`:`UUIDNODE` {`name`:"Alice", `uuid`:"EwixTQPwQeC-_QrdI5hWlw"})
create (_86:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$yPQphYhdKZdEuIQZq4gvaeTLrkXWc4cAuRgdqB2FHjn9YY6jHSBSy"})
create (_87:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `status`:0, `timestamp`:1445028591239, `uuid`:"K1mVV4svQgaB6ZSqWJnQGg"})
create _11-[:`CLASSIFIES`]->_53
create _11-[:`CLASSIFIES`]->_54
create _11-[:`CLASSIFIES`]->_63
create _11-[:`CLASSIFIES`]->_66
create _12-[:`CLASSIFIES`]->_36
create _12-[:`CLASSIFIES`]->_37
create _12-[:`CLASSIFIES`]->_46
create _12-[:`CLASSIFIES`]->_69
create _15-[:`CLASSIFIES`]->_26
create _15-[:`CLASSIFIES`]->_30
create _15-[:`CLASSIFIES`]->_72
create _15-[:`CLASSIFIES`]->_82
create _16-[:`CLASSIFIES`]->_57
create _16-[:`CLASSIFIES`]->_60
create _20-[:`HASPASSWORD`]->_22
create _21-[:`HASLOGIN`]->_20
create _21-[:`MEMBEROF`]->_19
create _21-[:`VIEWED` {`timestamp`:1445028476278}]->_24
create _21-[:`VIEWED` {`timestamp`:1445028474800}]->_28
create _21-[:`VIEWED` {`timestamp`:1445027874563}]->_34
create _21-[:`VIEWED` {`timestamp`:1445026829829}]->_44
create _21-[:`VIEWED` {`timestamp`:1445027719725}]->_58
create _21-[:`VIEWED` {`timestamp`:1445027731262}]->_55
create _21-[:`VIEWED` {`timestamp`:1445027911545}]->_51
create _21-[:`VIEWED` {`timestamp`:1445027906346}]->_61
create _21-[:`VIEWED` {`timestamp`:1445027729730}]->_64
create _21-[:`VIEWED` {`timestamp`:1445027877222}]->_67
create _21-[:`VIEWED` {`timestamp`:1445028220917}]->_70
create _21-[:`VIEWED` {`timestamp`:1445028536405}]->_80
create _21-[:`HASKARMA` {`karma`:2}]->_23
create _21-[:`HASKARMA` {`karma`:1}]->_50
create _21-[:`HASKARMA` {`karma`:1}]->_27
create _21-[:`VOTES` {`timestamp`:1445026654329, `weight`:1}]->_36
create _21-[:`VOTES` {`timestamp`:1445026732933, `weight`:33}]->_48
create _21-[:`VOTES` {`timestamp`:1445026812265, `weight`:1}]->_46
create _21-[:`VOTES` {`timestamp`:1445027884039, `weight`:1}]->_69
create _21-[:`VOTES` {`timestamp`:1445027913075, `weight`:1}]->_53
create _21-[:`VOTES` {`timestamp`:1445028536396, `weight`:33}]->_83
create _21-[:`CREATEDSTART`]->_25
create _21-[:`CREATEDSTART`]->_29
create _21-[:`CREATEDSTART`]->_65
create _21-[:`CREATEDSTART`]->_81
create _21-[:`KARMALOGSTART`]->_74
create _21-[:`KARMALOGSTART`]->_75
create _21-[:`KARMALOGSTART`]->_76
create _21-[:`UPDATEDSTART`]->_48
create _21-[:`UPDATEDSTART`]->_83
create _23-[:`TAGSSTART`]->_26
create _23-[:`TAGSSTART`]->_54
create _25-[:`CREATEDEND`]->_24
create _26-[:`TAGSEND`]->_24
create _27-[:`TAGSSTART`]->_30
create _29-[:`CREATEDEND`]->_28
create _30-[:`TAGSEND`]->_28
create _31-[:`HASPASSWORD`]->_33
create _32-[:`HASLOGIN`]->_31
create _32-[:`MEMBEROF`]->_19
create _32-[:`VIEWED` {`timestamp`:1445027838835}]->_28
create _32-[:`VIEWED` {`timestamp`:1445027852480}]->_24
create _32-[:`VIEWED` {`timestamp`:1445026521071}]->_34
create _32-[:`VIEWED` {`timestamp`:1445027833984}]->_64
create _32-[:`VIEWED` {`timestamp`:1445027801301}]->_55
create _32-[:`VIEWED` {`timestamp`:1445027687729}]->_67
create _32-[:`VIEWED` {`timestamp`:1445027632553}]->_51
create _32-[:`VIEWED` {`timestamp`:1445027683738}]->_58
create _32-[:`VIEWED` {`timestamp`:1445027683092}]->_61
create _32-[:`VIEWED` {`timestamp`:1445027679182}]->_70
create _32-[:`HASKARMA` {`karma`:2}]->_23
create _32-[:`HASKARMA` {`karma`:1}]->_27
create _32-[:`HASKARMA` {`karma`:1}]->_50
create _32-[:`VOTES` {`timestamp`:1445027803506, `weight`:1}]->_57
create _32-[:`VOTES` {`timestamp`:1445027810595, `weight`:1}]->_66
create _32-[:`VOTES` {`timestamp`:1445027841947, `weight`:1}]->_30
create _32-[:`VOTES` {`timestamp`:1445027854916, `weight`:1}]->_26
create _32-[:`CREATEDSTART`]->_35
create _32-[:`CREATEDSTART`]->_68
create _32-[:`CREATEDSTART`]->_71
create _32-[:`KARMALOGSTART`]->_47
create _32-[:`KARMALOGSTART`]->_77
create _34-[:`CONNECTSSTART`]->_36
create _34-[:`CONNECTSSTART`]->_37
create _35-[:`CREATEDEND`]->_34
create _36-[:`CONNECTSEND`]->_28
create _37-[:`CONNECTSEND`]->_24
create _38-[:`HASPASSWORD`]->_40
create _39-[:`HASLOGIN`]->_38
create _39-[:`MEMBEROF`]->_19
create _39-[:`VIEWED` {`timestamp`:1445026606866}]->_28
create _39-[:`VIEWED` {`timestamp`:1445026616910}]->_44
create _39-[:`VIEWED` {`timestamp`:1445027747867}]->_51
create _39-[:`VIEWED` {`timestamp`:1445027452777}]->_55
create _39-[:`VIEWED` {`timestamp`:1445027503674}]->_58
create _39-[:`VIEWED` {`timestamp`:1445027488568}]->_61
create _39-[:`HASKARMA` {`karma`:1}]->_27
create _39-[:`HASKARMA` {`karma`:2}]->_50
create _39-[:`HASKARMA` {`karma`:1}]->_23
create _39-[:`CREATEDSTART`]->_45
create _39-[:`CREATEDSTART`]->_52
create _39-[:`CREATEDSTART`]->_56
create _39-[:`CREATEDSTART`]->_59
create _39-[:`CREATEDSTART`]->_62
create _39-[:`KARMALOGSTART`]->_49
create _39-[:`KARMALOGSTART`]->_73
create _39-[:`KARMALOGSTART`]->_78
create _41-[:`HASPASSWORD`]->_43
create _42-[:`HASLOGIN`]->_41
create _42-[:`MEMBEROF`]->_19
create _44-[:`CONNECTSSTART`]->_46
create _45-[:`CREATEDEND`]->_44
create _46-[:`CONNECTSEND`]->_28
create _47-[:`LOGONSCOPE` {`currentKarma`:1}]->_27
create _47-[:`LOGONSCOPE` {`currentKarma`:1}]->_23
create _47-[:`KARMALOGEND`]->_34
create _48-[:`UPDATEDEND`]->_28
create _49-[:`LOGONSCOPE` {`currentKarma`:1}]->_27
create _49-[:`KARMALOGEND`]->_44
create _50-[:`TAGSSTART`]->_53
create _52-[:`CREATEDEND`]->_51
create _53-[:`TAGSEND`]->_51
create _54-[:`TAGSEND`]->_51
create _55-[:`CONNECTSSTART`]->_57
create _56-[:`CREATEDEND`]->_55
create _57-[:`CONNECTSEND`]->_51
create _58-[:`CONNECTSSTART`]->_60
create _59-[:`CREATEDEND`]->_58
create _60-[:`CONNECTSEND`]->_51
create _61-[:`CONNECTSSTART`]->_63
create _62-[:`CREATEDEND`]->_61
create _63-[:`CONNECTSEND`]->_58
create _64-[:`CONNECTSSTART`]->_66
create _65-[:`CREATEDEND`]->_64
create _66-[:`CONNECTSEND`]->_55
create _67-[:`CONNECTSSTART`]->_69
create _68-[:`CREATEDEND`]->_67
create _69-[:`CONNECTSEND`]->_55
create _70-[:`CONNECTSSTART`]->_72
create _71-[:`CREATEDEND`]->_70
create _72-[:`CONNECTSEND`]->_61
create _73-[:`LOGONSCOPE` {`currentKarma`:1}]->_23
create _73-[:`LOGONSCOPE` {`currentKarma`:1}]->_50
create _73-[:`KARMALOGEND`]->_55
create _74-[:`LOGONSCOPE` {`currentKarma`:1}]->_50
create _74-[:`LOGONSCOPE` {`currentKarma`:1}]->_23
create _74-[:`KARMALOGEND`]->_64
create _75-[:`LOGONSCOPE` {`currentKarma`:1}]->_27
create _75-[:`KARMALOGEND`]->_28
create _76-[:`LOGONSCOPE` {`currentKarma`:2}]->_23
create _76-[:`KARMALOGEND`]->_24
create _77-[:`LOGONSCOPE` {`currentKarma`:1}]->_50
create _77-[:`LOGONSCOPE` {`currentKarma`:2}]->_23
create _77-[:`KARMALOGEND`]->_67
create _78-[:`LOGONSCOPE` {`currentKarma`:2}]->_50
create _78-[:`KARMALOGEND`]->_51
create _79-[:`TAGSSTART`]->_82
create _81-[:`CREATEDEND`]->_80
create _82-[:`TAGSEND`]->_80
create _83-[:`UPDATEDEND`]->_80
create _84-[:`HASPASSWORD`]->_86
create _85-[:`HASLOGIN`]->_84
create _85-[:`MEMBEROF`]->_19
create _85-[:`VIEWED` {`timestamp`:1445028605629}]->_80
create _85-[:`VOTES` {`timestamp`:1445028605622, `weight`:1}]->_87
create _85-[:`UPDATEDSTART`]->_87
create _87-[:`UPDATEDEND`]->_80
;
commit
