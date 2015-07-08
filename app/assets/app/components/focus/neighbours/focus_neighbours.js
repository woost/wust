angular.module("wust.components").directive("focusNeighbours", focusNeighbours);

focusNeighbours.$inject = [];

function focusNeighbours() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/neighbours/neighbours.html",
        scope: {
            component: "="
        },
        controller: NeighboursCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

NeighboursCtrl.$inject = ["Post", "DiscourseNodeList", "DiscourseNodeCrate"];

function NeighboursCtrl(Post, DiscourseNodeList, DiscourseNodeCrate) {
    let vm = this;

    let node = vm.component.rootNode;
    vm.top = DiscourseNodeList.write.Post(node.predecessors, "From");
        //.nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        //.nested(DiscourseNodeList.write.Post, "connectsTo", "To");
    vm.bottom = DiscourseNodeList.write.Post(node.successors, "To");
        //.nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        //.nested(DiscourseNodeList.write.Post, "connectsTo", "To");
}
