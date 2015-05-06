angular.module("wust.components").directive("focusView", focusView);

focusView.$inject = ["$state", "$rootScope", "$q", "NodeHistory"];

function focusView($state, $rootScope, $q, NodeHistory) {
    return {
        restrict: "A",
        replace: false,
        scope: true,
        link: function($scope) {
            $scope.node.$then(data => {
                NodeHistory.add(data);
            });

            // callbacks for removing/updating the focused node
            $scope.removeFocused = _.wrap($scope.node, removeFocused);
            $scope.updateFocused = _.wrap($scope.node, updateFocused);

            // register for events for the current node, as well as all connected lists
            let unsubscribe = getUnsubscribePromise();
            unsubscribe.then($scope.node.$subscribeToLiveEvent(m => $scope.$apply(_.partial(onNodeChange, $scope.node, m))));
            _.each(_.compact([$scope.left, $scope.right, $scope.bottom, $scope.top]), list => {
                unsubscribe.then(list.subscribe());
            });

        }
    };

    function getUnsubscribePromise() {
        let unsubscribe = $q.defer();
        let deregisterEvent = $rootScope.$on("$stateChangeSuccess", () => {
            unsubscribe.resolve();
            deregisterEvent();
        });

        return unsubscribe.promise;
    }

    function onNodeChange(node, message) {
        switch (message.type) {
            case "edit":
                _.assign(node, message.data);
                break;
            default:
        }
    }

    function removeFocused(node) {
        node.$destroy().$then(() => {
            NodeHistory.remove(node.id);
            humane.success("Removed node");
            $state.go("browse");
        });
    }

    function updateFocused(node) {
        node.$save().$then(() => {
            humane.success("Updated node");
        });
    }
}
