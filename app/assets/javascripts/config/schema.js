angular.module("wust").config(function($provide, DiscourseNodeProvider) {
    let schema = angular.copy(window.globals.schema);
    _.each(schema.models, model => {
        DiscourseNodeProvider.setLabel(model.name, model.label);

        $provide.factory(model.name, (restmod, Live) => restmod.model(model.path).mix(_(model.subs).map((sub, path) => {
            return {
                [path]: {
                    [sub.cardinality]: restmod.model().mix({
                        $extend: {
                            Collection: {
                                $subscribeToLiveEvent: function(id, handler) {
                                    return Live.subscribe(`${model.path}/${id}/${path}`, handler);
                                }
                            }
                        }
                    })
                }
            };
        }).reduce(_.merge, {
            $extend: {
                Record: {
                    $subscribeToLiveEvent: function(handler) {
                        return Live.subscribe(`${model.path}/${this.id}`, handler);
                    }
                }
            }
        })));
    });
});
