angular.module("wust").config(function($provide, DiscourseNodeProvider) {
    let schema = angular.copy(window.globals.schema);
    _.each(schema.models, model => {
        DiscourseNodeProvider[model.name].label = model.label;

        $provide.factory(model.name, restmod => restmod.model(model.path).mix(_(model.subs).map(sub => {
            return {
                [sub.path]: {
                    [sub.type]: restmod.model()
                }
            };
        }).reduce(_.merge)));
    });
});
