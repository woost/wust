begin
create index on :`CHANGEREQUEST`(`status`);
create index on :`LOGININFO`(`providerID`);
create index on :`TIMESTAMP`(`timestamp`);
create constraint on (n:`LOGININFO`) assert n.`providerKey` is unique;
create constraint on (n:`TAGLIKE`) assert n.`title` is unique;
create constraint on (n:`USERGROUP`) assert n.`name` is unique;
create constraint on (n:`USER`) assert n.`name` is unique;
create constraint on (n:`UUIDNODE`) assert n.`uuid` is unique;
commit
begin
create (_0:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:150, `precedence`:100, `symbol`:"fa fa-check", `title`:"Done", `uuid`:"2NE09YjCTveV3VnOsbuyoA"})
create (_1:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:90, `precedence`:20, `symbol`:"fa fa-flask", `title`:"Problem", `uuid`:"XWgoP9PVQSa0aC49trmuFA"})
create (_2:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:169, `precedence`:20, `symbol`:"fa fa-crosshairs", `title`:"Goal", `uuid`:"R1vBGv_RSB2r9-LQnETGlg"})
create (_3:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:260, `precedence`:20, `symbol`:"fa fa-lightbulb-o", `title`:"Idea", `uuid`:"2MDMaEDuRmuRDC6G4r4sPw"})
create (_4:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:301, `precedence`:10, `symbol`:"fa fa-question", `title`:"Question", `uuid`:"5zTuP7-IQ4COwVPXxl_D_Q"})
create (_5:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:280, `precedence`:10, `symbol`:"fa fa-list-ul", `title`:"Task", `uuid`:"B3Tl2Ue-SFGqcgP1eCAK1w"})
create (_6:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:70, `precedence`:10, `symbol`:"fa fa-arrow-left", `title`:"Cause", `uuid`:"68Ijg-aSR-SFhcOioCKmRA"})
create (_7:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:135, `precedence`:6, `symbol`:"fa fa-thumbs-o-up", `title`:"Pro", `uuid`:"d7wClFsNQd2GtKMzwXXgcQ"})
create (_8:`UUIDNODE`:`TAGLIKE`:`EXPOSEDNODE`:`CLASSIFICATION` {`color`:21, `precedence`:5, `symbol`:"fa fa-thumbs-o-down", `title`:"Contra", `uuid`:"0aP_kUb4SVO-hxvd6Bge4w"})
create (_9:`USERGROUP`:`UUIDNODE` {`name`:"everyone", `uuid`:"LTrLEM-JRQu8ArouepOU3Q"})
;
commit
