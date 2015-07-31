angular.module("wust.services").service("ZenService", ZenService);

ZenService.$inject = [];

function ZenService() {
    this.hide = hide;
    this.show = show;
    this.create = create;

    this.hide();

    this.node = {
        description: ""
    };

    function hide() {
        this.visible = false;
    }

    function show(node) {
        this.node = node;
        this.visible = true;
    }

    function create() {
        return new ZenService();
    }
}
