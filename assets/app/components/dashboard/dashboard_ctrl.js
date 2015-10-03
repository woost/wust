angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["DiscourseNode", "StreamService", "Search", "ContextService", "$scope"];

function DashboardCtrl(DiscourseNode, StreamService, Search, ContextService, $scope) {
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

    vm.recentPosts = Search.$search({
        label: DiscourseNode.Post.label,
        tagsAll: ContextService.currentContexts.map(c => c.id),
        size: 30,
        page: 0,
        startPost: true
    });

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
