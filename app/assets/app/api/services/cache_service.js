angular.module("wust.api").factory("CacheService", CacheService);

CacheService.$inject = ["$rootScope"];

function CacheService($rootScope) {
    class Cache {
        constructor() {
            this.cache = {};
        }

        get(key) {
            return this.cache[key];
        }

        put(key, value) {
            this.cache[key] = value;
            value.$then(subscribe);
        }
    }

    class CacheScope {
        constructor() {
            this.cache = {};
        }

        get(key) {
            let cached = this.cache[key];
            if (cached === undefined) {
                cached = new Cache();
                this.cache[key] = cached;
            }

            return cached;
        }
    }

    return new Cache();

    function subscribe(response) {
        if (response.$subscribeToLiveEvent === undefined)
            return;

        response.$subscribeToLiveEvent(_.partial(_.isArray(response) ? collectionCallback : modelCallback, response));
    }

    //TODO: should not apply rootscope on any update in cache
    function modelCallback(node, message) {
        $rootScope.$apply(() => {
            switch (message.type) {
                case "edit":
                    _.assign(node, message.data);
                    break;
                default:
            }
        });
    }

    function collectionCallback(list, message) {
        $rootScope.$apply(() => {
            switch (message.type) {
                case "connect":
                    addLocally(list, message.data);
                    break;
                case "disconnect":
                    removeLocally(list, message.data.id);
                    break;
                default:
            }
        });
    }

    function removeLocally(list, id) {
        let elem = _.find(list, {
            id: id
        });

        list.$remove(elem);
    }

    function addLocally(list, elem) {
        if (_.any(list, {
            id: elem.id
        }))
            return;

        list.$buildRaw(elem).$reveal();
    }
}
