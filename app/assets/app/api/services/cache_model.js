angular.module("wust.api").provider("CacheModel", CacheModel);

CacheModel.$inject = [];

function CacheModel() {
    this.$get = get;
    this.setCache = setCache;

    let cacheServiceName;
    let store = {};
    let defaultCache = {
        set: (k,v) => store[k] = v,
        get: _.propertyOf(store)
    };

    function setCache(serviceName) {
        cacheServiceName = serviceName;
    }

    get.$inject = ["restmod", "$injector"];
    function get(restmod, $injector) {
        let cacheService = cacheServiceName ? $injector.get(cacheServiceName) : defaultCache;

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
            let cached = cacheService.get(url);
            if (cached === undefined) {
                cached = this.$super.apply(this, args);
                cacheService.set(url, cached);
            }

            return cached;
        }
    }
}
