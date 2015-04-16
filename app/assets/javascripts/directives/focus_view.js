angular.module("wust").directive("focusView", function($state, NodeHistory, Live, DiscourseNode) {
    return {
        restrict: "A",
        templateUrl: "assets/partials/focus_view.html",
        scope: true,
        link: function($scope) {
            $scope.node.$then(data => {
                Live.subscribe(`${$scope.nodeInfo.state}/${data.id}`, _.wrap($scope, onMessage));
                NodeHistory.add($scope.node);
            });
            $scope.removeFocused = _.wrap($scope.node, removeFocused);
            $scope.updateFocused = _.wrap($scope.node, updateFocused);
        }
    };

    function onMessage(scope, message) {
        scope.$apply(() => {
            let list;
            switch (message.type) {
                case "connect":
                    list = listOf(message.data);
                    scope[list].addNode(message.data);
                    break;

                case "disconnect":
                    list = listOf(message.data);
                    scope[list].removeNode(message.data.id);
                    break;

                default:
            }
        });
    }

    function listOf(node) {
        switch (node.label) {
            case DiscourseNode.Goal.label:
                return "goals";
            case DiscourseNode.Problem.label:
                return "problems";
            case DiscourseNode.Idea.label:
                return "ideas";
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
