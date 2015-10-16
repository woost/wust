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
create (_0:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:90, `symbol`:"fa fa-flask", `title`:"Problem", `uuid`:"l4zc3DtITz2SCxiBZG7R9w"})
create (_1:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:169, `symbol`:"fa fa-crosshairs", `title`:"Goal", `uuid`:"8CivdqSGT1ahfJELD7pssg"})
create (_2:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:260, `symbol`:"fa fa-lightbulb-o", `title`:"Idea", `uuid`:"NRFcX4tlRKyY9SVxjBNBHw"})
create (_3:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:135, `symbol`:"fa fa-thumbs-o-up", `title`:"Pro", `uuid`:"oV3HFMQxSji7iOstKusOlQ"})
create (_4:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:21, `symbol`:"fa fa-thumbs-o-down", `title`:"Contra", `uuid`:"zHVrjuMcRiK6Zv0P6N24Kg"})
create (_5:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:301, `symbol`:"fa fa-question", `title`:"Question", `uuid`:"3SPq-Z-JRfirb-MeNwmMZw"})
create (_6:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:280, `symbol`:"fa fa-list-ul", `title`:"Task", `uuid`:"scpUeOcrRem_ic2vp04TmQ"})
create (_7:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:70, `symbol`:"fa fa-arrow-left", `title`:"Cause", `uuid`:"o4QyU3HgRZe-nXYHevEZAQ"})
create (_8:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:150, `symbol`:"fa fa-check", `title`:"Done", `uuid`:"DGvwALVERjWtIfka_EZhzw"})
create (_9:`USERGROUP`:`UUIDNODE` {`name`:"everyone", `uuid`:"q60qzzpDTY-R6fbFP-mHEg"})
;
commit
