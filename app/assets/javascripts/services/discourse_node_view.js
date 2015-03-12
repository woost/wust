app.factory('DiscourseNodeView', function($state, $stateParams, DiscourseNodeList) {
    return function(scope, nodeInfo, service) {
        var id = $stateParams.id;

        scope.nodeCss = nodeInfo.css;
        scope.node = service.get(id);
        scope.goals = new DiscourseNodeList.Goal(id, service);
        scope.problems = new DiscourseNodeList.Problem(id, service);
        scope.ideas = new DiscourseNodeList.Idea(id, service);
        scope.removeFocused = removeFocused(id, service.remove);
    };

    function removeFocused(id, removeFunc) {
        return function() {
            removeFunc(id).$promise.then(function(data) {
                toastr.success("Removed node");
                $state.go('home');
            });
        };
    }
});
