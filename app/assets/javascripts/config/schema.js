angular.module("wust").config(function($provide, DiscourseNodeProvider, LiveProvider, restmodProvider) {
    let schema = angular.copy(window.globals.schema);
    LiveProvider.setBaseUrl(schema.api.websocketRoot);

    restmodProvider.rebase({
        $config: {
            urlPrefix: schema.api.restRoot
        }
    });

    _.each(schema.models, model => {
        DiscourseNodeProvider.setLabel(model.name, model.label);

        $provide.factory(model.name, (restmod, Live) => {
            function subscribe(handler) {
                let url = this.$url().slice(schema.api.restRoot.length + 1);
                return Live.subscribe(url, handler);
            }

            function subMixin(sub, path) {
                console.log(sub, path);
            }

            return restmod.model(model.path).mix(_(model.subs).map((sub, path) => {
                return {
                    [path]: {
                        [sub.cardinality]: restmod.model().mix(_.merge({
                            $extend: {
                                Collection: {
                                    $subscribeToLiveEvent: subscribe
                                }
                            }
                        }, _(sub.subs).map((sub, path) => {
                            return {
                                [path]: {
                                    [sub.cardinality]: restmod.model()
                                }
                            };
                        }).reduce(_.merge)))
                    }
                };
            }).reduce(_.merge, {
                $extend: {
                    Record: {
                        $subscribeToLiveEvent: subscribe
                    }
                }
            }));
        });
    });
});
