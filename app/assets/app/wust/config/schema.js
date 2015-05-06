angular.module("wust").config(SchemaConfig);

SchemaConfig.$inject = ["SchemaProvider", "DiscourseNodeProvider", "DiscourseNodeListProvider"];

function SchemaConfig(SchemaProvider, DiscourseNodeProvider, DiscourseNodeListProvider) {
    let schema = window.globals.schema;

    _.each(schema.models, model => {
        DiscourseNodeProvider.setLabel(model.name, model.label);
        DiscourseNodeProvider.setCss(model.name, `discourse_${model.name.toLowerCase()}`);
        DiscourseNodeListProvider.setList(model.name, model.path);
    });

    SchemaProvider.setup(schema);
}
