angular.module("wust").config(function($provide) {
    let schema = angular.copy(window.globals.schema);
    $provide.constant("SchemaInfo", _(schema.models).map(model => {
        return {
            [model.name]: {
                label: model.label,
                state: model.path
            }
        };
    }).reduce(_.merge));
    _.each(schema.models, model => {
        $provide.factory(model.name, restmod => restmod.model(model.path).mix(_(model.subs).map(sub => {
            return {
                [sub.path]: {
                    [sub.type]: restmod.model()
                }
            };
        }).reduce(_.merge)));
    });
});
