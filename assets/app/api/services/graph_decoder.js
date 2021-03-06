angular.module("wust.api").service("GraphDecoder", GraphDecoder);

GraphDecoder.$inject = ["$q", "UniqArr", "Helpers"];

function GraphDecoder($q, UniqArr, Helpers) {
    // expose restmod mixin
    this.mixin = {
        $extend: {
            Record: {
                $rawGraphPromise: rawGraphPromise
            }
        }
    };

    function rawGraphPromise() {
        let deferred = $q.defer();
        this.$then(graph => {
            let g = renesca.js.GraphFactory().fromRecord(graph);
            deferred.resolve(g);
        });

        return deferred.promise;
    }
}
