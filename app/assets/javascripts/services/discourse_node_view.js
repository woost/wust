angular.module("wust").factory("DiscourseNodeView", function($state, $stateParams, NodeHistory, DiscourseNodeList, Live, DiscourseNode) {
    return setScope;

    function setScope(scope, nodeInfo, service) {
        let id = $stateParams.id;

        scope.nodeCss = nodeInfo.css;
        scope.node = service.$find(id);
        scope.goals = new DiscourseNodeList.Goal(scope.node);
        scope.problems = new DiscourseNodeList.Problem(scope.node);
        scope.ideas = new DiscourseNodeList.Idea(scope.node);
        scope.removeFocused = _.wrap(scope.node, removeFocused);
        scope.updateFocused = _.wrap(scope.node, updateFocused);
        NodeHistory.add(scope.node);

        scope.messages = [];

        Live.subscribe(`/live/v1/${nodeInfo.state}/${id}`, _.wrap(scope, onMessage));
    }

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
