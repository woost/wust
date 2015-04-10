angular.module("wust").config(function($provide) {
    let schema = angular.copy(window.globals.schema);
    $provide.constant("Labels", _(schema.models).map((model) => {
        return {
            [model.name]: model.label
        };
    }).reduce(_.merge));
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
