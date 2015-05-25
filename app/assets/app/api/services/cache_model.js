angular.module("wust.api").provider("CacheModel", CacheModel);

CacheModel.$inject = [];

function CacheModel() {
    this.$get = get;
    this.setCache = setCache;

    let defaultCache = {};
    let cache = {
        set: (k,v) => defaultCache[k] = v,
        get: _.propertyOf(defaultCache)
    };

    function setCache(get, set) {
        cache.get = get;
        cache.set = set;
    }

    get.$inject = ["restmod", "$rootScope"];
    function get(restmod, $rootScope) {
        return {
            $extend: {
                Model: {
                    "$find": function(_pk) {
                        let url = `${this.$url()}/${_pk}`;
                        return cachedResponse.bind(this)(url, arguments);
                    }
                },
                Collection: {
                    "$fetch": function() {
                        return cachedResponse.bind(this)(this.$url(), arguments);
                    }
                }
            }
        };

        function cachedResponse(url, args) {
            let cached = cache.get(url);
            if (cached === undefined) {
                cached = this.$super.apply(this, args);
                subscribe(cached);
                cache.set(url, cached);
            }

            return cached;
        }

        function subscribe(response) {
            if (response.$subscribeToLiveEvent === undefined)
                return;

            response.$subscribeToLiveEvent(_.partial(_.isArray(response) ? collectionCallback : modelCallback, response));
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
    }
}
