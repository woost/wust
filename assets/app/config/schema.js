angular.module("wust.config").config(SchemaConfig);

SchemaConfig.$inject = ["SchemaProvider", "DiscourseNodeProvider", "DiscourseNodeListProvider", "restmodProvider", "LiveProvider"];

function SchemaConfig(SchemaProvider, DiscourseNodeProvider, DiscourseNodeListProvider, restmodProvider, LiveProvider) {
    let schema = window.globals.schema;

    LiveProvider.setBaseUrl(schema.api.websocketRoot);

    restmodProvider.rebase({
        $config: {
            urlPrefix: schema.api.restRoot,
            style: "wust",
            primaryKey: "id"
        },
        $extend: {
            Model: {
                encodeUrlName: _.kebabCase
            },
            Record: {
                encode: function() {
                    return this.$encode();
                }
            },
        }
    });

    _.each(schema.models, model => {
        DiscourseNodeProvider.setLabel(model.name, model.label);
        DiscourseNodeListProvider.setList(model.name, model.path);
    });

    SchemaProvider.setup(schema);
}
