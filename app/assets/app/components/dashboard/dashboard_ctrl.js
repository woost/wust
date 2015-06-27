angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$scope", "$state", "$modal", "Tag", "DiscourseNode", "StreamService"];

function DashboardCtrl($scope, $state, $modal, Tag, DiscourseNode, StreamService) {
    let vm = this;
    vm.nodeInfo = DiscourseNode.Tag;
    vm.streams = StreamService.streams;
    vm.open = open;

    function open(size) {
        var modalInstance = $modal.open({
            animation: true,
            templateUrl: "assets/app/components/dashboard/new_stream.html",
            controller: "NewStreamCtrl as vm",
            size: size,
            resolve: {
                items: function() {
                    return vm.items;
                }
            }
        });

        modalInstance.result.then(selectedItems => {
            StreamService.push(selectedItems);
        }, () => console.log("Modal dismissed at: " + new Date()));
    }
}
