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

NeighboursCtrl.$inject = ["Post", "DiscourseNodeList"];

function NeighboursCtrl(Post, DiscourseNodeList) {
    let vm = this;

    vm.references = DiscourseNodeList.write.Post.successors(vm.component, vm.component.rootNode, "References", "connectsTo");
        // .nested(DiscourseNodeList.write.Post.successors, "References", "connectsTo")
        // .nested(DiscourseNodeList.write.Post.predecessors, "Replies", "connectsFrom");
    vm.replies = DiscourseNodeList.write.Post.predecessors(vm.component, vm.component.rootNode, "Replies", "connectsFrom")
        .nested(DiscourseNodeList.write.Post.successors, "References", "connectsTo")
        .nested(DiscourseNodeList.write.Post.predecessors, "Replies", "connectsFrom");
}
