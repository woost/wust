angular.module("wust.config").config(SchemaConfig);

SchemaConfig.$inject = ["SchemaProvider", "DiscourseNodeProvider", "DiscourseNodeListProvider", "restmodProvider", "LiveServiceProvider"];

function SchemaConfig(SchemaProvider, DiscourseNodeProvider, DiscourseNodeListProvider, restmodProvider, LiveServiceProvider) {
    let schema = window.globals.schema;

    LiveServiceProvider.setBaseUrl(schema.api.websocketRoot);

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
                    let encoded = this.$encode();
                    //TODO: more general solution, as encode loses all collection propertiees
                    if (this.tags !== undefined)
                        encoded.tags = angular.copy(this.tags.map(t => t.$encode ? t.$encode() : t));
                    if (this.classifications !== undefined)
                        encoded.classifications = angular.copy(this.classifications.map(t => t.$encode ? t.$encode() : t));

                    return encoded;
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
