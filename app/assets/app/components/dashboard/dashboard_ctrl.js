angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$scope", "$state", "$modal", "Tag", "DiscourseNode"];

function DashboardCtrl($scope, $state, $modal, Tag, DiscourseNode) {
    let vm = this;
    vm.streams = [];

    vm.open = function(size) {
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

        modalInstance.result.then(function(selectedItems) {
            // $scope.$apply(() =>
            vm.streams.push({
                "tags": selectedItems,
                "posts": Tag.$buildRaw(selectedItems[0]).posts.$search()
            });
            // );
        }, function() {
            console.log("Modal dismissed at: " + new Date());
        });
    };
}
