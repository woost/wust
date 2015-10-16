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
create (_26:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"_83CuIyBTHibmMIkBTVJvA", `voteCount`:0})
create (_27:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`SCOPE` {`color`:15, `title`:"Arbeit", `uuid`:"fMJWvmz_S0-A99IJ-8gl6g"})
create (_28:`UUIDNODE`:`TIMESTAMP`:`EXPOSEDNODE`:`CONNECTABLE`:`HIDEABLE`:`POST` {`_locked`:false, `backupLabels`:"", `description`:"Es sind knapp 5km", `timestamp`:1445026441099, `title`:"Mit welchem Verkehrsmittttel fahre ich am besten zur Arbeit?", `uuid`:"_2QCNaskSYaMMQpVu4FV5w", `viewCount`:3})
create (_29:`UUIDNODE`:`TIMESTAMP`:`ACTION`:`CREATED` {`timestamp`:1445026441100, `uuid`:"YQcQC4A5SdSMgApR5TMjBQ"})
create (_30:`UUIDNODE`:`VOTABLE`:`REFERENCE`:`TAGS` {`_locked`:false, `uuid`:"khz_WC3vQAOe2sjZD9SFdQ", `voteCount`:0})
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
create _12-[:`CLASSIFIES`]->_36
create _12-[:`CLASSIFIES`]->_37
create _12-[:`CLASSIFIES`]->_46
create _15-[:`CLASSIFIES`]->_26
create _15-[:`CLASSIFIES`]->_30
create _20-[:`HASPASSWORD`]->_22
create _21-[:`HASLOGIN`]->_20
create _21-[:`MEMBEROF`]->_19
create _21-[:`VIEWED` {`timestamp`:1445026845941}]->_24
create _21-[:`VIEWED` {`timestamp`:1445026836838}]->_28
create _21-[:`VIEWED` {`timestamp`:1445026666377}]->_34
create _21-[:`VIEWED` {`timestamp`:1445026829829}]->_44
create _21-[:`VOTES` {`timestamp`:1445026654329, `weight`:1}]->_36
create _21-[:`VOTES` {`timestamp`:1445026732933, `weight`:33}]->_48
create _21-[:`VOTES` {`timestamp`:1445026812265, `weight`:1}]->_46
create _21-[:`CREATEDSTART`]->_25
create _21-[:`CREATEDSTART`]->_29
create _21-[:`UPDATEDSTART`]->_48
create _23-[:`TAGSSTART`]->_26
create _25-[:`CREATEDEND`]->_24
create _26-[:`TAGSEND`]->_24
create _27-[:`TAGSSTART`]->_30
create _29-[:`CREATEDEND`]->_28
create _30-[:`TAGSEND`]->_28
create _31-[:`HASPASSWORD`]->_33
create _32-[:`HASLOGIN`]->_31
create _32-[:`MEMBEROF`]->_19
create _32-[:`VIEWED` {`timestamp`:1445026508882}]->_28
create _32-[:`VIEWED` {`timestamp`:1445026507849}]->_24
create _32-[:`VIEWED` {`timestamp`:1445026521071}]->_34
create _32-[:`HASKARMA` {`karma`:1}]->_23
create _32-[:`HASKARMA` {`karma`:1}]->_27
create _32-[:`CREATEDSTART`]->_35
create _32-[:`KARMALOGSTART`]->_47
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
create _39-[:`HASKARMA` {`karma`:1}]->_27
create _39-[:`CREATEDSTART`]->_45
create _39-[:`KARMALOGSTART`]->_49
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
;
commit
