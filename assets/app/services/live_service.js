angular.module("wust.services").provider("LiveService", LiveService)
    .run(RunLive);

RunLive.$inject = ["LiveService"];

function RunLive(LiveService, Session) {}

LiveService.$inject = [];

function LiveService() {
    let baseUrl = "";
    this.setBaseUrl = url => baseUrl = url;

    this.$get = get;

    get.$inject = ["$rootScope", "Auth", "HistoryService", "KarmaService", "StreamService", "Session"];
    function get($rootScope, Auth, HistoryService, KarmaService, StreamService, Session) {
        let url = currentUrl();
        let nodesSocket = new WebSocket(`${url}${baseUrl}/nodes`);
        nodesSocket.onmessage = readNodeEvent;

        if (Auth.current.userId) {
            let usersSocket = new WebSocket(`${url}${baseUrl}/users/${Auth.current.userId}`);
            usersSocket.onmessage = readUserEvent;
        }

        return {
            registerNodes
        };

        function readNodeEvent(message) {
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

        function readUserEvent(message) {
            let event = JSON.parse(message.data);
            $rootScope.$apply(() => {
                switch (event.kind) {
                    case "karmalog":
                        KarmaService.updateKarma(event.data);
                        break;
                    case "dashboard":
                        StreamService.refreshDashboard(event.data);
                        break;
                    case "notification":
                        Session.addNotification(event.data);
                        break;
                }
            });
        }

        function registerNodes(nodes) {
            //TODO: wait until open
            setTimeout(() => nodesSocket.send(JSON.stringify({nodes: nodes.map(n => n.id)})), 50);
        }
    }

    function currentUrl() {
        let loc = window.location;
        let newUri = loc.protocol === "https:" ? "wss:" : "ws:";
        newUri += "//" + loc.host;

        return newUri;
    }
}
