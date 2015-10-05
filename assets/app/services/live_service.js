angular.module("wust.services").provider("LiveService", LiveService);

LiveService.$inject = [];

function LiveService() {
    let baseUrl = "";
    this.setBaseUrl = url => baseUrl = url;

    this.$get = get;

    get.$inject = ["$rootScope", "HistoryService"];
    function get($rootScope, HistoryService) {
        let url = currentUrl();
        let nodesSocket = new WebSocket(`${url}${baseUrl}/nodes`);
        nodesSocket.onmessage = readEvent;

        return {
            registerNodes
        };

        function readEvent(message) {
            let event = JSON.parse(message.data);
            $rootScope.$apply(() => {
                switch (event.kind) {
                    case "edit":
                        HistoryService.updateCurrentView(event.data);
                        break;
                    case "connects":
                        HistoryService.addConnectToCurrentView(event.data);
                        break;
                    case "delete":
                        HistoryService.removeFromCurrentView(event.data);
                        break;
                }
            });
        }

        function registerNodes(nodes) {
            nodesSocket.send(JSON.stringify({nodes: nodes.map(n => n.id)}));
        }
    }

    function currentUrl() {
        let loc = window.location;
        let newUri = loc.protocol === "https:" ? "wss:" : "ws:";
        newUri += "//" + loc.host;

        return newUri;
    }
}
