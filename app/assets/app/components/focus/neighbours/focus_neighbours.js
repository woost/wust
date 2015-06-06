angular.module("wust.components").directive("focusNeighbours", focusNeighbours);

focusNeighbours.$inject = [];

function focusNeighbours() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/neighbours/neighbours.html",
        scope: {
            component: "=",
            rootId: "="
        },
        controller: NeighboursCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

NeighboursCtrl.$inject = ["Post", "DiscourseNodeList", "DiscourseNodeCrate"];

function NeighboursCtrl(Post, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    let node = Post.$find(vm.rootId);
    vm.node = DiscourseNodeCrate(node);
    vm.top = DiscourseNodeList.write.Post(node.connectsFrom.$search(), "From")
        .nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        .nested(DiscourseNodeList.write.Post, "connectsTo", "To");
    vm.bottom = DiscourseNodeList.write.Post(node.connectsTo.$search(), "To")
        .nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        .nested(DiscourseNodeList.write.Post, "connectsTo", "To");
}
