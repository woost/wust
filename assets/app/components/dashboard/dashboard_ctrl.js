angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["StreamService", "ContextService", "$scope"];

function DashboardCtrl(StreamService, ContextService, $scope) {
    let vm = this;

    vm.streams = StreamService.streams;
    vm.removeStream = StreamService.remove;
    vm.refreshStream = StreamService.refreshStream;

    vm.newStream = newStream;

    vm.sortableOptions = {
        containment: "#stream_list",
        orderChanged: StreamService.persist,
        accept: acceptDrop
    };

    vm.recentPosts = StreamService.recentPosts;

    $scope.$on("context.changed", () => {
        vm.recentPosts.$refresh({
            tagsAll: ContextService.currentContexts.map(c => c.id),
            startPost: true
        });
    });

    function acceptDrop(sourceItemHandleScope, destSortableScope, destItemScope) {
        return sourceItemHandleScope.$parent.$parent.$id === destSortableScope.$id;
    }

    function newStream() {
        StreamService.push();
    }
}
