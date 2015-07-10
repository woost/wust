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

    let node = Post.$buildRaw(vm.component.rootNode.$encode());
    //TODO: how can we get updates here?
    _.each(_.reject(vm.component.rootNode.predecessors, "hyperEdge"), n => node.connectsFrom.$buildRaw(n.$encode()).$reveal());
    _.each(_.reject(vm.component.rootNode.successors, "hyperEdge"), n => node.connectsTo.$buildRaw(n.$encode()).$reveal());
    vm.top = DiscourseNodeList.write.Post(node.connectsFrom, "From");
        // .nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        // .nested(DiscourseNodeList.write.Post, "connectsTo", "To");
    vm.bottom = DiscourseNodeList.write.Post(node.connectsTo, "To");
        // .nested(DiscourseNodeList.write.Post, "connectsFrom", "From")
        // .nested(DiscourseNodeList.write.Post, "connectsTo", "To");
}
