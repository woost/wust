angular.module("wust").factory('DiscourseNodeView', function($state, $stateParams, NodeHistory, DiscourseNodeList) {
    return function(scope, nodeInfo, service) {
        var id = $stateParams.id;

        scope.nodeCss = nodeInfo.css;
        scope.node = service.get(id);
        scope.goals = new DiscourseNodeList.Goal(id, service);
        scope.problems = new DiscourseNodeList.Problem(id, service);
        scope.ideas = new DiscourseNodeList.Idea(id, service);
        scope.removeFocused = _.partial(removeFocused, id, service.remove);
        NodeHistory.add(scope.node);
    };

    function removeFocused(id, removeFunc) {
        removeFunc(id).$promise.then(function(data) {
            NodeHistory.remove(id);
            toastr.success("Removed node");
            $state.go('browse');
        });
    }
});
