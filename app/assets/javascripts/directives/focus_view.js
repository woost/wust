angular.module("wust").directive("focusView", function($state, NodeHistory) {
    return {
        restrict: "A",
        templateUrl: "assets/partials/focus_view.html",
        scope: true,
        link: function($scope) {
            $scope.node.$then(data => {
                data.$subscribeToLiveEvent(m => $scope.$apply(_.partial(onNodeChange, $scope.node, m)));
                NodeHistory.add($scope.node);
                _.each(_.compact([$scope.left, $scope.right, $scope.bottom, $scope.top]), list => {
                    list.model.list.$subscribeToLiveEvent(data.id, m => $scope.$apply(_.partial(onConnectionChange, list, m)));
                });
            });
            $scope.removeFocused = _.wrap($scope.node, removeFocused);
            $scope.updateFocused = _.wrap($scope.node, updateFocused);
        }
    };

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
