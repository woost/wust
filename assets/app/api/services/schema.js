angular.module("wust.api").provider("Schema", Schema);

Schema.$inject = ["$provide"];

function Schema($provide) {
    this.setup = setup;
    this.$get = _.constant({});

    function setup(schema) {
        _.each(schema.models, model => {
            $provide.factory(model.name, ApiFactory);

            ApiFactory.$inject = ["restmod"];

            function ApiFactory(restmod) {
                return restmod.model(model.path).mix(_(model.subs).map((sub, path) => {
                    return {
                        [_.camelCase(path)]: {
                            [sub.cardinality]: restmod.model().mix(_.merge({
                                $extend: {
                                    Collection: {
                                    }
                                }
                            }, _(sub.subs).map((sub, path) => {
                                return {
                                    [_.camelCase(path)]: {
                                        [sub.cardinality]: restmod.model()
                                    }
                                };
                            }).reduce(_.merge)))
                        }
                    };
                }).reduce(_.merge, {
                    $extend: {
                        Record: {
                        }
                    }
                }));
            }
        });
    }
}
