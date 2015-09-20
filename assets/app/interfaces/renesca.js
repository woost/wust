declare class rjsNode {
    id:string;
    endNode: rjsNode;
    startNode: rjsNode;
    successors: Array<rjsNode>;
    predecessors: Array<rjsNode>;
    parallels: Array<rjsNode>;
}

declare class rjsRelation {
}

declare class RawGraphChanges {
    newNodes:Array<rjsNode>;
}
declare class Graph {
    onCommit(f: (changes:RawGraphChanges) => any):void;
    relationByIds(startId: string, endId: string):rjsRelation
}
declare class HyperGraph {}

declare class RawGraph {
    wrap(name:string):Graph;
    hyperWrap(name:string):HyperGraph;
}

declare class RecordGraph {
}

declare class GraphFactory {
    fromRecord(record: RecordGraph):RawGraph;
}

declare class RenescaJs {
    GraphFactory():GraphFactory;
}

declare class Renesca {
    js: RenescaJs;
}

declare var renesca:Renesca;
