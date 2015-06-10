angular.module("wust.services").service("ZenService", ZenService);

ZenService.$inject = [];

//TODO: caching the last parse result, so we can skip parsing if the same node
//is send to zen mode multiple times in a row.
function ZenService() {
    this.hide = hide;
    this.show = show;

    this.hide();

    function hide() {
        this.node = {
            description: ""
        };
        this.visible = false;
    }

    function show(node) {
        this.node = node;
        this.visible = true;
    }
}
