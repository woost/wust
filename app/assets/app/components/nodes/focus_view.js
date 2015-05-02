angular.module("wust.components").directive("focusView", function($state, $rootScope, $q, NodeHistory) {
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

            // register for events for the current node, as well as all
            // connected lists
            let unsubscribe = getUnsubscribePromise();
            unsubscribe.then($scope.node.$subscribeToLiveEvent(m => $scope.$apply(_.partial(onNodeChange, $scope.node, m))));
            _.each(_.compact([$scope.left, $scope.right, $scope.bottom, $scope.top]), list => {
                unsubscribe.then(list.subscribe((l,m) => $scope.$apply(_.partial(onConnectionChange, l, m))));
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

    function onConnectionChange(list, message) {
        switch (message.type) {
            case "connect":
                list.addNode(message.data);
                break;
            case "disconnect":
                list.removeNode(message.data.id);
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
});
