angular.module("wust.components").directive("focusNeighbours", focusNeighbours);

focusNeighbours.$inject = [];

function focusNeighbours() {
    return {
        restrict: "A",
        templateUrl: "components/focus/neighbours/neighbours.html",
        scope: {
            component: "=",
            isLoading: "="
        },
        controller: NeighboursCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

NeighboursCtrl.$inject = ["Post", "DiscourseNodeList"];

function NeighboursCtrl(Post, DiscourseNodeList) {
    let vm = this;

    vm.changes = Post.$buildRaw(vm.component.rootNode).changes.$search();
    vm.references = DiscourseNodeList.write.Connectable.successors(vm.component, vm.component.rootNode, "connectsTo");
        // .nested(DiscourseNodeList.write.Connectable.predecessors, "connectsFrom");
    vm.replies = DiscourseNodeList.write.Connectable.predecessors(vm.component, vm.component.rootNode, "connectsFrom");
        // .nested(DiscourseNodeList.write.Connectable.predecessors, "connectsFrom");
    vm.parallels = DiscourseNodeList.read.parallels(vm.component, vm.component.rootNode);
}
