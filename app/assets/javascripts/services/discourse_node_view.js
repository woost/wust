angular.module("wust").service("DiscourseNodeView", function($state, $stateParams, NodeHistory, DiscourseNodeList, Live) {
    this.setScope = setScope;

    function setScope(scope, nodeInfo, service) {
        var id = $stateParams.id;

        scope.nodeCss = nodeInfo.css;
        scope.node = service.get(id);
        scope.goals = new DiscourseNodeList.Goal(id, service);
        scope.problems = new DiscourseNodeList.Problem(id, service);
        scope.ideas = new DiscourseNodeList.Idea(id, service);
        scope.removeFocused = _.partial(removeFocused, id, service.remove);
        NodeHistory.add(scope.node);

        scope.messages = [];

        // Live.subscribe(_.wrap(scope, onMessage));
        Live.subscribe("/live/v1", response => console.log("received message: " + response.responseBody));
    }

    function onMessage(scope, response) {
        scope.messages.push(response);
    }

    function removeFocused(id, removeFunc) {
        removeFunc(id).$promise.then(() => {
            NodeHistory.remove(id);
            toastr.success("Removed node");
            $state.go("browse");
        });
    }
});
