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

    vm.references = DiscourseNodeList.write.Post.successors(vm.component, vm.component.rootNode, "connectsTo");
        // .nested(DiscourseNodeList.write.Post.predecessors, "connectsFrom");
    vm.replies = DiscourseNodeList.write.Post.predecessors(vm.component, vm.component.rootNode, "connectsFrom");
        // .nested(DiscourseNodeList.write.Post.predecessors, "connectsFrom");
}
