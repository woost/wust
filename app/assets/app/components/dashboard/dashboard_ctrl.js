angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$modal", "DiscourseNode", "StreamService", "Recent"];

function DashboardCtrl($modal, DiscourseNode, StreamService, Recent) {
    let vm = this;

    let modalInstance = $modal({
            show: false,
            templateUrl: "assets/app/components/dashboard/new_stream.html",
            controller: "NewStreamCtrl",
            controllerAs: "vm",
            animation: "am-fade-and-slide-top"
    });

    vm.nodeInfo = DiscourseNode.Tag;
    vm.streams = StreamService.streams;
    vm.removeStream = StreamService.remove;

    vm.editModal = editModal;
    vm.showModal = showModal;
    vm.hideModal = hideModal;

    vm.sortableOptions = {
        containment: "#stream_list",
        orderChanged: StreamService.persist,
        accept: acceptDrop
    };

    vm.recentPosts = Recent.$search({
        label: DiscourseNode.Post.label
    });

    function editModal(stream) {
        StreamService.currentEditStream = stream;
        showModal();
    }

    function acceptDrop(sourceItemHandleScope, destSortableScope, destItemScope) {
        return sourceItemHandleScope.$parent.$parent.$id === destSortableScope.$id;
    }

    function showModal() {
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}
