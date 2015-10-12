angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["StreamService", "ContextService", "$scope"];

function DashboardCtrl(StreamService, ContextService, $scope) {
    let vm = this;

    StreamService.showDashboard();

    vm.streams = StreamService.streams;
    vm.removeStream = StreamService.remove;
    vm.refreshStream = StreamService.refreshStream;

    vm.newStream = newStream;

    vm.sortableOptions = {
        containment: "#stream_list",
        orderChanged: StreamService.persist,
        accept: acceptDrop
    };

    function acceptDrop(sourceItemHandleScope, destSortableScope, destItemScope) {
        return destItemScope !== undefined && sourceItemHandleScope.$parent.$parent.$id === destSortableScope.$id;
    }

    function newStream() {
        StreamService.push();
    }
}
