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

NeighboursCtrl.$inject = ["Connectable", "DiscourseNodeList"];

function NeighboursCtrl(Post, DiscourseNodeList) {
    let vm = this;

    vm.sourceA = "hallo du da";
    vm.sourceB = "hell das da";
    vm.references = DiscourseNodeList.write.Connectable.successors(vm.component, vm.component.rootNode, "connectsTo");
        // .nested(DiscourseNodeList.write.Connectable.predecessors, "connectsFrom");
    vm.replies = DiscourseNodeList.write.Connectable.predecessors(vm.component, vm.component.rootNode, "connectsFrom");
        // .nested(DiscourseNodeList.write.Connectable.predecessors, "connectsFrom");
}
