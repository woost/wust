angular.module("wust.api").provider("Schema", Schema);

Schema.$inject = ["$provide", "LiveProvider", "restmodProvider"];

function Schema($provide, LiveProvider, restmodProvider) {
    this.setup = setup;
    this.$get = _.constant({});

    function setup(schema) {
        LiveProvider.setBaseUrl(schema.api.websocketRoot);

        restmodProvider.rebase({
            $config: {
                urlPrefix: schema.api.restRoot
            }
        });

        _.each(schema.models, model => {
            $provide.factory(model.name, (restmod, Live) => {
                return restmod.model(model.path).mix(_(model.subs).map((sub, path) => {
                    return {
                        [path]: {
                            [sub.cardinality]: restmod.model().mix(_.merge({
                                $extend: {
                                    Collection: {
                                        $subscribeToLiveEvent: _.partial(subscribe, Live)
                                    }
                                }
                            }, _(sub.subs).map((sub, path) => {
                                return {
                                    [path]: {
                                        [sub.cardinality]: restmod.model().mix()
                                    }
                                };
                            }).reduce(_.merge)))
                        }
                    };
                }).reduce(_.merge, {
                    $extend: {
                        Record: {
                            $subscribeToLiveEvent: _.partial(subscribe, Live)
                        }
                    }
                }));
            });
        });

        function subscribe(liveService, handler, nested = "") {
            let url = this.$url().slice(schema.api.restRoot.length + 1) + nested;

            return liveService.subscribe(url, handler);
        }
    }
}
