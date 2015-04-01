angular.module("wust").service("DiscourseNodeView", function($state, $stateParams, NodeHistory, DiscourseNodeList, Live, DiscourseNode) {
    this.setScope = setScope;

    function setScope(scope, nodeInfo, service) {
        var id = $stateParams.id;

        scope.nodeCss = nodeInfo.css;
        scope.node = service.get(id);
        scope.goals = new DiscourseNodeList.Goal(id, service);
        scope.problems = new DiscourseNodeList.Problem(id, service);
        scope.ideas = new DiscourseNodeList.Idea(id, service);
        scope.removeFocused = _.partial(removeFocused, id, service.remove);
        scope.updateFocused = _.partial(updateFocused, id, scope.node, service.update);
        NodeHistory.add(scope.node);

        scope.messages = [];

        Live.subscribe(`/live/v1/${nodeInfo.state}/${id}`, _.wrap(scope, onMessage));
    }

    function onMessage(scope, message) {
        scope.$apply(() => {
            var list;
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
            case DiscourseNode.goal.label:
                return "goals";
            case DiscourseNode.problem.label:
                return "problems";
            case DiscourseNode.idea.label:
                return "ideas";
        }
    }

    function removeFocused(id, removeFunc) {
        removeFunc(id).$promise.then(() => {
            NodeHistory.remove(id);
            humane.success("Removed node");
            $state.go("browse");
        });
    }

    function updateFocused(id, node, updateFunc) {
        console.log(node);
        updateFunc(id, node).$promise.then(() => {
            humane.success("Updated node");
        });
    }
});
