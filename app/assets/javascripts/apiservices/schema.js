angular.module("wust").config(function($provide) {
    var schema = angular.copy(window.globals.schema);
    _.each(schema.models, (model) => {
        $provide.factory(model.name, (restmod) => restmod.model(model.path, _(model.subs).map((sub) => {
            return {
                [sub.path]: {
                    [sub.type]: restmod.model()
                }
            };
        }).reduce(_.merge)));
    });
});
