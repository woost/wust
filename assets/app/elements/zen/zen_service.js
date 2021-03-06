angular.module("wust.elements").service("ZenService", ZenService);

ZenService.$inject = [];

function ZenService() {
    this.hide = hide;
    this.show = show;

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
}
