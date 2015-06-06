angular.module("wust.discourse").directive("bigPost", bigPost);

bigPost.$inject = [];

function bigPost() {
    return {
        restrict: "A",
        templateUrl: "assets/app/discourse/directives/post/big_post.html",
        scope: {
            node: "="
        },
        controller: bigPostCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

bigPostCtrl.$inject = ["$state", "HistoryService", "EditService", "DiscourseNode"];

function bigPostCtrl($state, HistoryService, EditService, DiscourseNode) {
    let vm = this;

    // we are viewing details about a node, so add it to the nodehistory
    HistoryService.add(vm.node);

    //TODO: we use ng-href instead of ui-sref because nodeInfo seems to be
    //filled to late for ui-router to recognize the state
    vm.nodeInfo = DiscourseNode.Post;

    // callbacks for removing/updating the focused node
    vm.removeFocused = removeFocused;
    vm.updateFocused = updateFocused;

    function removeFocused() {
        vm.node.$destroy().$then(() => {
            HistoryService.remove(vm.node.id);
            humane.success("Removed node");
            $state.go("browse");
        });
    }

    function updateFocused() {
        EditService.editExisting(vm.node);
    }
}
