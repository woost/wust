match (n) optional match (n)-[r]-() delete n,r;
begin
create (_827:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:90, `symbol`:"fa fa-flask", `title`:"Problem", `uuid`:"OsWcw6CWRUWm4oBXwsZwEw"})
create (_828:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:169, `symbol`:"fa fa-crosshairs", `title`:"Goal", `uuid`:"hw_akS_8RYiqk38BNj-f0A"})
create (_829:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:260, `symbol`:"fa fa-lightbulb-o", `title`:"Idea", `uuid`:"SgA5_nScQGCVsBvdRizTWw"})
create (_830:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:135, `symbol`:"fa fa-thumbs-o-up", `title`:"Pro", `uuid`:"lWMoGqCORvK6SYDDvxZbdA"})
create (_831:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:21, `symbol`:"fa fa-thumbs-o-down", `title`:"Contra", `uuid`:"-U2zvSsjSfmqaxmb3wfxow"})
create (_832:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:301, `symbol`:"fa fa-question", `title`:"Question", `uuid`:"rnIdRqUZQYitADLpBW9_fg"})
create (_833:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:280, `symbol`:"fa fa-list-ul", `title`:"Task", `uuid`:"AS1RUShiSlmB0p_BmChL-w"})
create (_834:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:70, `symbol`:"fa fa-arrow-left", `title`:"Cause", `uuid`:"C54Lc-VeSTSz9cbJNjA1OA"})
create (_835:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:150, `symbol`:"fa fa-check", `title`:"Done", `uuid`:"WqholPfjRdKo2ZfSbFmoMA"})
create (_836:`USERGROUP`:`UUIDNODE` {`name`:"everyone", `uuid`:"X3sjyvwdSymeX_5ni5NO2A"})
create (_837:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Bob"})
create (_838:`USER`:`UUIDNODE` {`name`:"Bob", `uuid`:"MzvTiT8tRr6KeMXIHfExfA"})
create (_839:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$Y.9958U7jlzj.yRzUvWBPunGAG.eqO32MRnZ2KXTPxjQJdGwBE/lS"})
create (_840:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:95, `title`:"Urlaub", `uuid`:"oGuMSHatTHOVckzIEqaPsg"})
create (_841:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026364143, `title`:"Mit welchem Verkehrsmittel soll ich in den Urlaub von Deutschland nach Australien reisen?", `uuid`:"T3TDqpioQ4yOFlM342YvWg", `viewCount`:2})
create (_842:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026364149, `uuid`:"I87QidTCTZeBBKJ-XxaKUg"})
create (_843:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"_83CuIyBTHibmMIkBTVJvA", `voteCount`:1})
create (_844:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:15, `title`:"Arbeit", `uuid`:"fMJWvmz_S0-A99IJ-8gl6g"})
create (_845:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Es sind knapp 5km", `timestamp`:1445026441099, `title`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `uuid`:"_2QCNaskSYaMMQpVu4FV5w", `viewCount`:3})
create (_846:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026441100, `uuid`:"YQcQC4A5SdSMgApR5TMjBQ"})
create (_847:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"khz_WC3vQAOe2sjZD9SFdQ", `voteCount`:1})
create (_848:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Greta"})
create (_849:`USER`:`UUIDNODE` {`name`:"Greta", `uuid`:"gMDkFJoLTUqMruv8sHBIGg"})
create (_850:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$h7azF7eb1MF58dUnADB7Q.R/wHi5w1/6Ip84XzFse9SQy5YGHjWQK"})
create (_851:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026521069, `title`:"Mit dem Fahrrad", `uuid`:"jDJhPRu2QKqwq4geetfKPQ", `viewCount`:2})
create (_852:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026521070, `uuid`:"Thlzs5vqTaOTCIsfxv7ddw"})
create (_853:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026521075, `uuid`:"6w4XtunITkK7Pu5GYmuWYg", `voteCount`:1})
create (_854:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026529553, `uuid`:"OHeSdKJIRBu6vxWcn_3K_w", `voteCount`:0})
create (_855:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Barbera"})
create (_856:`USER`:`UUIDNODE` {`name`:"Barbera", `uuid`:"QmVPPDP1Rq22I9Iby638hg"})
create (_857:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$zz73dPUaBUZ9lXgq13sDZOAUa7z40ib4RMIFwcAwmN8EDbkeFi3ry"})
create (_858:`LOGININFO` {`providerID`:"credentials", `providerKey`:"John"})
create (_859:`USER`:`UUIDNODE` {`name`:"John", `uuid`:"Q1Egz56yTpWX8am3tHDG7A"})
create (_860:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$nQvv7Cmz8cjTGafc8WCSZepxejuX6WEhYPt02B/HOHnYZTI6Qet3O"})
create (_861:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445026616909, `title`:"Mit dem Bus", `uuid`:"gM2jrcl9TZmPOLHscw6TQQ", `viewCount`:2})
create (_862:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026616910, `uuid`:"RCUwjyqyRQiC7Qamj9fkxA"})
create (_863:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445026616913, `uuid`:"n-rPl-r4QeqRAiZpMo6f_A", `voteCount`:1})
create (_864:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026654389, `uuid`:"02uCVb6STLiEo7jm7j2eVQ"})
create (_865:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:3, `approvalSum`:33, `newDescription`:"Es sind knapp 5km", `newTitle`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `oldDescription`:"Es sind knapp 5km", `oldTitle`:"Mit welchem Verkehrsmittel fahre ich am besten zur Arbeit?", `status`:2, `timestamp`:1445026732925, `uuid`:"MYbyARYoSa-dNZAkvFuI0A"})
create (_866:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445026812280, `uuid`:"g6MA0ADTQnusnnr6c1OP-Q"})
create (_867:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:47, `title`:"Wust", `uuid`:"Vq73bRudRsey5mePOA35rQ"})
create (_868:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Wie jedes Jahr wollen wir wieder gemeinsam unseren Betriebsausflug planen. Ich hoffe auf eine rege Beteiligung.", `timestamp`:1445027390947, `title`:"Planung des Betriebsausflugs der Firma Wust", `uuid`:"pu4Uv0yETU6mOPyah5VKTQ", `viewCount`:3})
create (_869:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027390947, `uuid`:"dFeNCOC5Qg6py-on0eSEEA"})
create (_870:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"CfPh_5btT4-8_YfqkvzejQ", `voteCount`:1})
create (_871:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"66CVMHLFRaKW6-kDWULjww", `voteCount`:0})
create (_872:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027452776, `title`:"Zeitrahmen vereinbaren", `uuid`:"f6MfrKRGRH23XUqF2HG0hQ", `viewCount`:3})
create (_873:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027452776, `uuid`:"EXKWE372SNyljK693A7nJQ"})
create (_874:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027452778, `uuid`:"dQeKEezkQ2-d8exYo8hyHw", `voteCount`:1})
create (_875:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027465953, `title`:"Veranstaltungsort finden", `uuid`:"tQ97mn7aQtO_keYZVb9DMQ", `viewCount`:3})
create (_876:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027465953, `uuid`:"ByGKO6eNQ0Cd8oX5ePbOZQ"})
create (_877:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027465954, `uuid`:"SWZFjXzkRMWt-m_Vo6sxaA", `voteCount`:0})
create (_878:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027488568, `title`:"Unterbringung von ca. 50 Personen", `uuid`:"MpEoD90wS6OwVNDBeaT_3w", `viewCount`:3})
create (_879:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027488568, `uuid`:"zbLrfEO1SPixVExrbz_bSg"})
create (_880:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027488569, `uuid`:"eZK--TqNQ7OhbMR6WxRGjQ", `voteCount`:0})
create (_881:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445027582496, `title`:"Vor der Weihnachtsfeier", `uuid`:"fIlt2rfPRGi3jMbWVEE5yg", `viewCount`:2})
create (_882:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027582496, `uuid`:"MjBXtWkkQi-9KSp2ApqY0w"})
create (_883:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027582497, `uuid`:"5wJals92R7CA-ZasFwRBZw", `voteCount`:1})
create (_884:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann haben auch die Programmierer Zeit.", `timestamp`:1445027627490, `title`:"Nach dem 18.11.2015", `uuid`:"9nZGpPKkS6ezJoulZY8LTg", `viewCount`:2})
create (_885:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027627490, `uuid`:"y9YZFm0IT6atdVB9RC7sPw"})
create (_886:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027627491, `uuid`:"JjCdOEYLSYOZ11w6WrKLAw", `voteCount`:1})
create (_887:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Dann wären es nämlich 60 Personen.", `timestamp`:1445027670437, `title`:"Fehlen bei den 50 Personen nicht die Mitarbeiter der Außenstelle?", `uuid`:"Tvacpm03SU2r7fGx5plCKg", `viewCount`:2})
create (_888:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445027670437, `uuid`:"RPDzaIE-Raqg869FfBK3NQ"})
create (_889:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445027670438, `uuid`:"WmVHJHvsR5OMPoyMIsvoSQ", `voteCount`:0})
create (_890:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027803521, `uuid`:"vgegYx7UR2yH9PT2DJkFaA"})
create (_891:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027810605, `uuid`:"WkkM8ZzUQE26HFhAvIbgaQ"})
create (_892:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027841962, `uuid`:"3X0cixOtQNmgXEv-bOIZQw"})
create (_893:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027854927, `uuid`:"hyzMzuJORGKAFT37gpBPnA"})
create (_894:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027884049, `uuid`:"jRnJI8qqRGeeMoqPisrH_A"})
create (_895:`UUIDNODE`:`TIMESTAMP`:`KARMALOG` {`karmaChange`:1, `reason`:"Upvoted post", `timestamp`:1445027913085, `uuid`:"RiSxpI2lRS-Gdus2V1o9rw"})
create (_896:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:96, `title`:"Haushalt", `uuid`:"gOjgDSXMTaOTsUqcP97z4g"})
create (_897:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445028527972, `title`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `uuid`:"rLNvZMajTvCg3X1kWw7pQw", `viewCount`:2})
create (_898:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445028527972, `uuid`:"nL62rPZNSgi46fZMrqP7XA"})
create (_899:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"yeaTofcWQ-CyUJrLRIzRhA", `voteCount`:0})
create (_900:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:33, `newTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `status`:2, `timestamp`:1445028536394, `uuid`:"Wa1Qo9NZTtmfk4LhqOgocg"})
create (_901:`LOGININFO` {`providerID`:"credentials", `providerKey`:"Alice"})
create (_902:`USER`:`UUIDNODE` {`name`:"Alice", `uuid`:"EwixTQPwQeC-_QrdI5hWlw"})
create (_903:`PASSWORDINFO` {`hasher`:"bcrypt", `password`:"$2a$10$yPQphYhdKZdEuIQZq4gvaeTLrkXWc4cAuRgdqB2FHjn9YY6jHSBSy"})
create (_904:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Was mache ich bei Fettflecken auf meiner Hose?", `oldTitle`:"Was mache ich ich bei Fettflecken auf meiner Hose?", `status`:0, `timestamp`:1445028591239, `uuid`:"K1mVV4svQgaB6ZSqWJnQGg"})
create (_905:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:16, `title`:"Fahrrad", `uuid`:"r8h_bOCFQJmV2HSowxI9Ag"})
create (_906:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080609909, `title`:"Wie kann ich bei meinem Fahrrad die Schaltung reparieren?", `uuid`:"G-UHYqIuTcuBH94n3mDJZA", `viewCount`:2})
create (_907:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080609911, `uuid`:"J8bmWYFnSXqkdKyA6tpIvw"})
create (_908:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"BvJr6X65RhCMHt7MLVWybQ", `voteCount`:0})
create (_909:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `timestamp`:1445080673124, `title`:"Käten- oder Nabenschaltung?", `uuid`:"oDNVqbATTJ2m71sJF_3JUQ", `viewCount`:2})
create (_910:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445080673125, `uuid`:"Ea1nQmi1QbSQU9TNl8eUXw"})
create (_911:`UUIDNODE`:`EXPOSEDNODE`:`CONNECTABLE`:`VOTABLE`:`CONNECTS`:`REFERENCE` {`_locked`:false, `timestamp`:1445080673129, `uuid`:"BjaCpVrjQw2Hf-V55mmjuw", `voteCount`:0})
create (_912:`UUIDNODE`:`TIMESTAMP`:`VOTABLE`:`ACTION`:`CHANGEREQUEST`:`UPDATED` {`_locked`:false, `applyThreshold`:2, `approvalSum`:1, `newTitle`:"Ketten- oder Nabenschaltung?", `oldTitle`:"Käten- oder Nabenschaltung?", `status`:0, `timestamp`:1445080704405, `uuid`:"z4TtlUHPTa6AycS_UbHyDA"})
create _828-[:`CLASSIFIES`]->_883
create _828-[:`CLASSIFIES`]->_880
create _828-[:`CLASSIFIES`]->_871
create _828-[:`CLASSIFIES`]->_870
create _829-[:`CLASSIFIES`]->_886
create _829-[:`CLASSIFIES`]->_863
create _829-[:`CLASSIFIES`]->_854
create _829-[:`CLASSIFIES`]->_853
create _832-[:`CLASSIFIES`]->_899
create _832-[:`CLASSIFIES`]->_889
create _832-[:`CLASSIFIES`]->_847
create _832-[:`CLASSIFIES`]->_843
create _832-[:`CLASSIFIES`]->_908
create _832-[:`CLASSIFIES`]->_911
create _833-[:`CLASSIFIES`]->_874
create _833-[:`CLASSIFIES`]->_877
create _837-[:`HASPASSWORD`]->_839
create _838-[:`HASLOGIN`]->_837
create _838-[:`MEMBEROF`]->_836
create _838-[:`VIEWED` {`timestamp`:1445027729730}]->_881
create _838-[:`VIEWED` {`timestamp`:1445027877222}]->_884
create _838-[:`VIEWED` {`timestamp`:1445028220917}]->_887
create _838-[:`VIEWED` {`timestamp`:1445028536405}]->_897
create _838-[:`VIEWED` {`timestamp`:1445027719725}]->_875
create _838-[:`VIEWED` {`timestamp`:1445027731262}]->_872
create _838-[:`VIEWED` {`timestamp`:1445027911545}]->_868
create _838-[:`VIEWED` {`timestamp`:1445027906346}]->_878
create _838-[:`VIEWED` {`timestamp`:1445028476278}]->_841
create _838-[:`VIEWED` {`timestamp`:1445028474800}]->_845
create _838-[:`VIEWED` {`timestamp`:1445027874563}]->_851
create _838-[:`VIEWED` {`timestamp`:1445026829829}]->_861
create _838-[:`VIEWED` {`timestamp`:1445080704411}]->_909
create _838-[:`HASKARMA` {`karma`:1}]->_867
create _838-[:`HASKARMA` {`karma`:2}]->_840
create _838-[:`HASKARMA` {`karma`:1}]->_844
create _838-[:`VOTES` {`timestamp`:1445028536396, `weight`:33}]->_900
create _838-[:`VOTES` {`timestamp`:1445026812265, `weight`:1}]->_863
create _838-[:`VOTES` {`timestamp`:1445026732933, `weight`:33}]->_865
create _838-[:`VOTES` {`timestamp`:1445027913075, `weight`:1}]->_870
create _838-[:`VOTES` {`timestamp`:1445027884039, `weight`:1}]->_886
create _838-[:`VOTES` {`timestamp`:1445026654329, `weight`:1}]->_853
create _838-[:`VOTES` {`timestamp`:1445080704408, `weight`:1}]->_912
create _838-[:`CREATEDSTART`]->_898
create _838-[:`CREATEDSTART`]->_842
create _838-[:`CREATEDSTART`]->_882
create _838-[:`CREATEDSTART`]->_846
create _838-[:`KARMALOGSTART`]->_891
create _838-[:`KARMALOGSTART`]->_893
create _838-[:`KARMALOGSTART`]->_892
create _838-[:`UPDATEDSTART`]->_900
create _838-[:`UPDATEDSTART`]->_865
create _838-[:`UPDATEDSTART`]->_912
create _840-[:`TAGSSTART`]->_871
create _840-[:`TAGSSTART`]->_843
create _842-[:`CREATEDEND`]->_841
create _843-[:`TAGSEND`]->_841
create _844-[:`TAGSSTART`]->_847
create _846-[:`CREATEDEND`]->_845
create _847-[:`TAGSEND`]->_845
create _848-[:`HASPASSWORD`]->_850
create _849-[:`HASLOGIN`]->_848
create _849-[:`MEMBEROF`]->_836
create _849-[:`VIEWED` {`timestamp`:1445027852480}]->_841
create _849-[:`VIEWED` {`timestamp`:1445027838835}]->_845
create _849-[:`VIEWED` {`timestamp`:1445027833984}]->_881
create _849-[:`VIEWED` {`timestamp`:1445026521071}]->_851
create _849-[:`VIEWED` {`timestamp`:1445027801301}]->_872
create _849-[:`VIEWED` {`timestamp`:1445027687729}]->_884
create _849-[:`VIEWED` {`timestamp`:1445027632553}]->_868
create _849-[:`VIEWED` {`timestamp`:1445027683738}]->_875
create _849-[:`VIEWED` {`timestamp`:1445027683092}]->_878
create _849-[:`VIEWED` {`timestamp`:1445027679182}]->_887
create _849-[:`HASKARMA` {`karma`:2}]->_840
create _849-[:`HASKARMA` {`karma`:1}]->_844
create _849-[:`HASKARMA` {`karma`:1}]->_867
create _849-[:`VOTES` {`timestamp`:1445027803506, `weight`:1}]->_874
create _849-[:`VOTES` {`timestamp`:1445027810595, `weight`:1}]->_883
create _849-[:`VOTES` {`timestamp`:1445027841947, `weight`:1}]->_847
create _849-[:`VOTES` {`timestamp`:1445027854916, `weight`:1}]->_843
create _849-[:`CREATEDSTART`]->_852
create _849-[:`CREATEDSTART`]->_885
create _849-[:`CREATEDSTART`]->_888
create _849-[:`KARMALOGSTART`]->_894
create _849-[:`KARMALOGSTART`]->_864
create _851-[:`CONNECTSSTART`]->_854
create _851-[:`CONNECTSSTART`]->_853
create _852-[:`CREATEDEND`]->_851
create _853-[:`CONNECTSEND`]->_845
create _854-[:`CONNECTSEND`]->_841
create _855-[:`HASPASSWORD`]->_857
create _856-[:`HASLOGIN`]->_855
create _856-[:`MEMBEROF`]->_836
create _856-[:`VIEWED` {`timestamp`:1445026616910}]->_861
create _856-[:`VIEWED` {`timestamp`:1445026606866}]->_845
create _856-[:`VIEWED` {`timestamp`:1445027488568}]->_878
create _856-[:`VIEWED` {`timestamp`:1445027503674}]->_875
create _856-[:`VIEWED` {`timestamp`:1445027452777}]->_872
create _856-[:`VIEWED` {`timestamp`:1445027747867}]->_868
create _856-[:`VIEWED` {`timestamp`:1445080610074}]->_906
create _856-[:`HASKARMA` {`karma`:1}]->_840
create _856-[:`HASKARMA` {`karma`:1}]->_844
create _856-[:`HASKARMA` {`karma`:2}]->_867
create _856-[:`CREATEDSTART`]->_862
create _856-[:`CREATEDSTART`]->_876
create _856-[:`CREATEDSTART`]->_879
create _856-[:`CREATEDSTART`]->_869
create _856-[:`CREATEDSTART`]->_873
create _856-[:`CREATEDSTART`]->_907
create _856-[:`KARMALOGSTART`]->_895
create _856-[:`KARMALOGSTART`]->_866
create _856-[:`KARMALOGSTART`]->_890
create _858-[:`HASPASSWORD`]->_860
create _859-[:`HASLOGIN`]->_858
create _859-[:`MEMBEROF`]->_836
create _861-[:`CONNECTSSTART`]->_863
create _862-[:`CREATEDEND`]->_861
create _863-[:`CONNECTSEND`]->_845
create _864-[:`LOGONSCOPE` {`currentKarma`:1}]->_844
create _864-[:`LOGONSCOPE` {`currentKarma`:1}]->_840
create _864-[:`KARMALOGEND`]->_851
create _865-[:`UPDATEDEND`]->_845
create _866-[:`LOGONSCOPE` {`currentKarma`:1}]->_844
create _866-[:`KARMALOGEND`]->_861
create _867-[:`TAGSSTART`]->_870
create _869-[:`CREATEDEND`]->_868
create _870-[:`TAGSEND`]->_868
create _871-[:`TAGSEND`]->_868
create _872-[:`CONNECTSSTART`]->_874
create _873-[:`CREATEDEND`]->_872
create _874-[:`CONNECTSEND`]->_868
create _875-[:`CONNECTSSTART`]->_877
create _876-[:`CREATEDEND`]->_875
create _877-[:`CONNECTSEND`]->_868
create _878-[:`CONNECTSSTART`]->_880
create _879-[:`CREATEDEND`]->_878
create _880-[:`CONNECTSEND`]->_875
create _881-[:`CONNECTSSTART`]->_883
create _882-[:`CREATEDEND`]->_881
create _883-[:`CONNECTSEND`]->_872
create _884-[:`CONNECTSSTART`]->_886
create _885-[:`CREATEDEND`]->_884
create _886-[:`CONNECTSEND`]->_872
create _887-[:`CONNECTSSTART`]->_889
create _888-[:`CREATEDEND`]->_887
create _889-[:`CONNECTSEND`]->_878
create _890-[:`LOGONSCOPE` {`currentKarma`:1}]->_840
create _890-[:`LOGONSCOPE` {`currentKarma`:1}]->_867
create _890-[:`KARMALOGEND`]->_872
create _891-[:`LOGONSCOPE` {`currentKarma`:1}]->_840
create _891-[:`LOGONSCOPE` {`currentKarma`:1}]->_867
create _891-[:`KARMALOGEND`]->_881
create _892-[:`LOGONSCOPE` {`currentKarma`:1}]->_844
create _892-[:`KARMALOGEND`]->_845
create _893-[:`LOGONSCOPE` {`currentKarma`:2}]->_840
create _893-[:`KARMALOGEND`]->_841
create _894-[:`LOGONSCOPE` {`currentKarma`:1}]->_867
create _894-[:`LOGONSCOPE` {`currentKarma`:2}]->_840
create _894-[:`KARMALOGEND`]->_884
create _895-[:`LOGONSCOPE` {`currentKarma`:2}]->_867
create _895-[:`KARMALOGEND`]->_868
create _896-[:`TAGSSTART`]->_899
create _898-[:`CREATEDEND`]->_897
create _899-[:`TAGSEND`]->_897
create _900-[:`UPDATEDEND`]->_897
create _901-[:`HASPASSWORD`]->_903
create _902-[:`HASLOGIN`]->_901
create _902-[:`MEMBEROF`]->_836
create _902-[:`VIEWED` {`timestamp`:1445028605629}]->_897
create _902-[:`VIEWED` {`timestamp`:1445080640213}]->_906
create _902-[:`VIEWED` {`timestamp`:1445080673125}]->_909
create _902-[:`VOTES` {`timestamp`:1445028605622, `weight`:1}]->_904
create _902-[:`CREATEDSTART`]->_910
create _902-[:`UPDATEDSTART`]->_904
create _904-[:`UPDATEDEND`]->_897
create _905-[:`TAGSSTART`]->_908
create _907-[:`CREATEDEND`]->_906
create _908-[:`TAGSEND`]->_906
create _909-[:`CONNECTSSTART`]->_911
create _910-[:`CREATEDEND`]->_909
create _911-[:`CONNECTSEND`]->_906
create _912-[:`UPDATEDEND`]->_909
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
