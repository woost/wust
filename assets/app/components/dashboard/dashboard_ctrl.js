angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["DiscourseNode", "StreamService", "Recent"];

function DashboardCtrl(DiscourseNode, StreamService, Recent) {
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

    vm.recentPosts = Recent.$search({
        label: DiscourseNode.Post.label
    });

    function acceptDrop(sourceItemHandleScope, destSortableScope, destItemScope) {
        return sourceItemHandleScope.$parent.$parent.$id === destSortableScope.$id;
    }

    function newStream() {
        StreamService.push([]);
    }
}
