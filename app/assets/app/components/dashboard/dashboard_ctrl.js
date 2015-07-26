angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$modal", "DiscourseNode", "StreamService"];

function DashboardCtrl($modal, DiscourseNode, StreamService) {
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
    vm.showModal = showModal;
    vm.hideModal = hideModal;

    function showModal() {
        modalInstance.$promise.then(modalInstance.show);
    }

    function hideModal() {
        modalInstance.$promise.then(modalInstance.hide);
    }
}
