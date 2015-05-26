angular.module("wust.api").service("CacheService", CacheService);

CacheService.$inject = ["$rootScope"];

function CacheService($rootScope) {
    this.get = getCache;
    this.set = setCache;

    //TODO: use local storage
    let cache = {};

    function getCache(key) {
        return cache[key];
    }

    function setCache(key, value) {
        cache[key] = value;
        subscribe(value);
    }

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
