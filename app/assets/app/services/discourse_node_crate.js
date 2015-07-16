angular.module("wust.services").factory("DiscourseNodeCrate", DiscourseNodeCrate);

DiscourseNodeCrate.$inject = ["$rootScope"];

function DiscourseNodeCrate($rootScope) {
    class NodeCrate {
        constructor(node) {
            this.model = node;
        }

        subscribe(unsubscribe = true) {
            let unsubscribeFunc = this.model.$subscribeToLiveEvent(m => onNodeChange(this.model, m));
            if (unsubscribe) {
                let deregisterEvent = $rootScope.$on("$stateChangeSuccess", () => {
                    unsubscribeFunc();
                    deregisterEvent();
                });
            }

            return unsubscribeFunc;
        }
    }

    return (node) => new NodeCrate(node);

    function onNodeChange(node, message) {
        $rootScope.$apply(() => {
            switch (message.type) {
                case "edit":
                    _.assign(node, message.data);
                    break;
                default:
            }
        });
    }
}
