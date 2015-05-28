angular.module("cachemod", ["restmod"]);

angular.module("cachemod").provider("CacheModel", CacheModel);

CacheModel.$inject = [];

function CacheModel() {
    this.$get = get;
    this.setCache = setCache;

    var cacheServiceName;
    var store = {};
    var defaultCache = {
        put: function(k, v) {
            store[k] = v;
        },
        get: function(k) {
            return store[k];
        }
    };

    function setCache(serviceName) {
        cacheServiceName = serviceName;
    }

    get.$inject = ["restmod", "$injector"];

    function get(restmod, $injector) {
        var cache = cacheServiceName ? $injector.get(cacheServiceName) : defaultCache;

        return restmod.mixin(function() {
            this.define("Model.$new", function(_key, _scope) {
                var url = this.$url() || (_scope ? _scope.$url() : undefined);
                if (_key !== undefined && url !== undefined) {
                    return cachedResponse.apply(this, [url + "/" + _key, this.$params, arguments]);
                }

                return this.$super.apply(this, arguments);
            }).define("Collection.$fetch", function() {
                var url = this.$url();
                console.log("FETCH", url);
                console.log("FETCH", this.$params);
                if (url !== undefined) {
                    return cachedResponse.apply(this, [url, this.$params, arguments]);
                }

                return this.$super.apply(this, arguments);
            }).define("Record.$fetch", function() {
                //TODO: properly distinguish between singletons and instances?
                if (this.$pk === "") {
                    console.log("SINGLE");
                    console.log(this.$url());
                    return cachedResponse.apply(this, [this.$url(), arguments]);
                }

                return this.$super.apply(this, arguments);
            }).define("Record.$decode", function() {
                var result = this.$super.apply(this, arguments);
                var cached = cache.get(this.$url);
                //TODO: this happens if we build a new object and then save it,
                // the result of the post gets send back to the client and
                // should be cached in this decode function but then
                // hasMany/belongsTo relations are not available. why?
                if (cached !== undefined) {
                    console.log("DECODE", result);
                    cache.put(this.$url(), this);
                }
                return result;
            });

            this.on("after-update", (data) => console.log("UPDATED", data));

            function cachedResponse(url, params, args) {
                var key = url + (this.$params !== undefined ? JSON.stringify(this.$params) : "");
                var cached = cache.get(key);
                console.log("CACHED", cached);
                if (cached === undefined) {
                    cached = this.$super.apply(this, args);
                    console.log("GOT", cached);
                    cache.put(key, cached);
                }

                return cached;
            }
        });
    }
}
