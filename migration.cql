// gitref: first-part -> master
// run with:  bin/neo4j-shell -file ~/projects/wust/migration.cql -path data/graph.db

match (u:USER) REMOVE u:DUMMYUSER:REALUSER return u;

match (p:POST) REMOVE p:CONTENTNODE:INHERITABLE:TAGGABLE:VOTABLE
SET p:EXPOSEDNODE:HIDEABLE return p;

match (t:CATEGORIZATION) REMOVE t:CATEGORIZATION:CONTENTNODE:VOTEDIMENSION
SET t:EXPOSEDNODE:SCOPE return t;

match (t:CLASSIFICATION) REMOVE t:CONTENTNODE:VOTEDIMENSION
SET t:EXPOSEDNODE return t;

match (n:DIMENSIONIZES)-[r]-() delete n,r;

match (c:CONNECTS) remove c:TAGGABLE set c:REFERENCE:EXPOSEDNODE return c;

match (:USER)-[:USERTOUPDATED]-(u:UPDATED)-[r:UPDATEDTOCONTENTNODE]-(p:POST) delete r create (u)-[:UPDATEDTOPOST]->(p) return u;

match (u:UPDATED) set u:CHANGEREQUEST return u;

match (a:POST)-[r:CONNECTABLETOCONNECTS]->(b) delete r create (a)-[:POSTTOCONNECTS]->(b) return a,b;

match (t:TAGLIKE {title: "StartPost"})-[r:TAGLIKETOTAGS]->(s:TAGS)-[r2:TAGSTOTAGGABLE]->(:POST) delete t,r,s,r2;

match (p:POST)-[ptc:POSTTOCONNECTS]->(c:CONNECTS), (p)<-[tagstotaggable:TAGSTOTAGGABLE]-(tags:TAGS)<-[tagliketotags:TAGLIKETOTAGS]-(classification:CLASSIFICATION) delete tagstotaggable,tags,tagliketotags create (c)<-[:CLASSIFIES]-(classification);

match (u:USER)-[r]->(created)-[r2]->(p:POST) delete r2 create (created)-[:CREATEDTOPOST]->(p);

match (t)-[r1:TAGLIKETOTAGS]->(tags)-[r2:TAGSTOTAGGABLE]->(p:POST) delete r1,r2 create (t)-[:SCOPETOTAGS]->(tags)-[:TAGSTOPOST]->(p);

match (t:CLASSIFICATION {title:"repliesTo"}) optional match (t)-[r1:TAGLIKETOTAGS]->(tags:TAGS)-[r2:TAGSTOTAGGABLE]->(:CONNECTS) optional match (t)-[k]-() delete r1,r2,tags,t,k;

match (s:SCOPE)-[r1:TAGLIKETOTAGS]->(tags:TAGS)-[r2:TAGSTOTAGGABLE]->(c:CONNECTS)<-[:POSTTOCONNECTS]-(p:POST) delete r1, r2 create (s)-[:SCOPETOTAGS]->(tags)-[:TAGSTOPOST]->(p);

match (c:CLASSIFICATION)-[r1:TAGLIKETOTAGS]->(tags:TAGS) optional match (tags)-[r3:TAGSTOTAGGABLE]-() delete r1,tags,r3;

match (a)-[r:TAGLIKETOTAGS]-(b:TAGS) delete r,b;

match (u:USER)-[:USERTOUPDATED]->(updated:UPDATED)-[cr:CREATEDTOPOST]->(p:POST) delete cr create (updated)-[:UPDATEDTOPOST]->(p);


match (p:POST) set p.backuplabels = "", p.viewCount = 0, p._locked = false;

match (p:REFERENCE) set p.voteCount = 0, p._locked = false;

match (p:TAGS) set p:REFERENCE, p:VOTABLE, p.voteCount = 0, p._locked = false;

match (c:CLASSIFICATION)-[r1:SCOPETOTAGS]->(t:TAGS)-[r2:TAGSTOPOST]->(p:POST) delete r1,t,r2 create (c)-[:CLASSIFIES]->(p);

match (a:CLASSIFICATION), (b:SCOPE) where id(a) <> id(b) and lower(a.title) = lower(b.title) with a,b match (b)-[r1:SCOPETOTAGS]->(tags:TAGS)-[r2:TAGSTOPOST]->(p:POST) delete b,r1,tags,r2 create (a)-[:CLASSIFIES]->(p);

match (b:CLASSIFICATION {title: "bug"}) set b.title = "Bug";

merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Problem"}) set c.color  = 90  , c.uuid = "PQt1NQ14Ry273rY3LwuO-Q";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Goal"}) set c.color     = 169 , c.uuid = "MTCpdiVnR2ew9r22oYmQEA";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Idea"}) set c.color     = 260 , c.uuid = "RddhZfhpSMeXV0574XXCXA";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Pro"}) set c.color      = 135 , c.uuid = "kPM7hfAYRguRBQr86cLAxA";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Con"}) set c.color      = 21  , c.uuid = "xnotrDs0QHW5rZJqH4Z9pQ";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Bug"}) set c.color      = 57  , c.uuid = "gos26XlpSWmuCNS62By2dQ";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Question"}) set c.color = 301 , c.uuid = "1Ah3QTLZTkaAEhx8oHEG1w";
merge (c:CLASSIFICATION:EXPOSEDNODE:INHERITABLE:TAGLIKE:UUIDNODE {title: "Answer"}) set c.color   = 280 , c.uuid = "_MsUfHNBRreAjsBFqWOxDg";

match (c:CONNECTS)-[:CONNECTSTOCONNECTABLE]->(p:POST) set c.timestamp = p.timestamp;



match (anon:USER {name: "anonymous"}),(felix:USER {name: "felix"}),(johannes:USER {name: "johannes"}), (anon)-[r:USERTOCREATED]->(action)-[]->(:CONNECTABLE) where id(action) % 2 = 0 delete r create (felix)-[:USERTOCREATED]->(action);
match (anon:USER {name: "anonymous"}),(felix:USER {name: "felix"}),(johannes:USER {name: "johannes"}), (anon)-[r:USERTOCREATED]->(action)-[]->(:CONNECTABLE) where id(action) % 2 = 1 delete r create (johannes)-[:USERTOCREATED]->(action);
match (anon:USER {name: "anonymous"}),(felix:USER {name: "felix"}),(johannes:USER {name: "johannes"}), (anon)-[r:USERTOUPDATED]->(action)-[]->(:CONNECTABLE) where id(action) % 2 = 0 delete r create (felix)-[:USERTOUPDATED]->(action);
match (anon:USER {name: "anonymous"}),(felix:USER {name: "felix"}),(johannes:USER {name: "johannes"}), (anon)-[r:USERTOUPDATED]->(action)-[]->(:CONNECTABLE) where id(action) % 2 = 1 delete r create (johannes)-[:USERTOUPDATED]->(action);
match (anon:USER {name: "anonymous"}),(felix:USER {name: "felix"}),(johannes:USER {name: "johannes"}), (anon)-[r:USERTOUPDATED|USERTOCREATED]->(action) delete anon, r;


match (c:CLASSIFICATION)-[classifies:CLASSIFIES]->(p:POST)<-[r1]-(ref:REFERENCE)<-[r2]-(connection) WHERE ((connection:POST) OR (connection:SCOPE)) delete classifies create (c)-[:CLASSIFIES]->(ref);

match (wust:SCOPE {title: "wust"}), (c:CLASSIFICATION)-[classifies:CLASSIFIES]->(p:POST) delete classifies create (c)-[:CLASSIFIES]->(tags:REFERENCE:TAGS:UUIDNODE:VOTABLE {uuid: toString(id(c)) + toString(id(p)) + toString(id(wust)), _locked:false, voteCount: 0 }), (wust)-[:SCOPETOTAGS]->(tags)-[:TAGSTOPOST]->(p) return p;

