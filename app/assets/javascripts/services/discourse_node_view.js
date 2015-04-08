angular.module("wust").service("DiscourseNodeView", function($state, $stateParams, NodeHistory, DiscourseNodeList, Live, DiscourseNode) {
    this.setScope = setScope;

    function setScope(scope, nodeInfo, service) {
        var id = $stateParams.id;

        scope.nodeCss = nodeInfo.css;
        var nodePath = service.$new(id);
        scope.node = nodePath.$fetch();
        scope.goals = new DiscourseNodeList.Goal(nodePath);
        scope.problems = new DiscourseNodeList.Problem(nodePath);
        scope.ideas = new DiscourseNodeList.Idea(nodePath);
        scope.removeFocused = _.partial(removeFocused, scope.node);
        scope.updateFocused = _.partial(updateFocused, scope.node);
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
