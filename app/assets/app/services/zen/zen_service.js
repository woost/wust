angular.module("wust.services").service("ZenService", ZenService);

ZenService.$inject = [];

function ZenService() {
    this.hide = hide;
    this.show = show;

    this.hide();

    function hide() {
        this.node = {};
        this.visible = false;
    }

    function show(node) {
        this.node = node;
        this.visible = true;
    }
}
