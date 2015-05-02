angular.module("wust.api").provider("Schema", function($provide, LiveProvider, restmodProvider) {
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
                function subscribe(handler) {
                    let url = this.$url().slice(schema.api.restRoot.length + 1);
                    return Live.subscribe(url, handler);
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
        return schema => {
            restmodProvider.rebase({
                $config: {
                    urlPrefix: schema.api.restRoot
                }
            });

            _.each(schema.models, model => {
                $provide.factory(model.name, (restmod, Live) => {
                    function subscribe(handler) {
                        let url = this.$url().slice(schema.api.restRoot.length + 1);
                        return Live.subscribe(url, handler);
                    }

                    return restmod.model(model.path).mix(_(model.subs).map((sub, path) => {
                        return {
                            [path]: {
                                [sub.cardinality]: restmod.model().mix({
                                    $extend: {
                                        Collection: {
                                            $subscribeToLiveEvent: subscribe
                                        }
                                    }
                                })
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
        };
    }
});
