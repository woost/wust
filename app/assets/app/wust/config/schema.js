angular.module("wust").config(function(SchemaProvider, DiscourseNodeProvider) {
    let schema = window.globals.schema;
    _.each(schema.models, model => DiscourseNodeProvider.setLabel(model.name, model.label));
    SchemaProvider.setup(schema);
});
