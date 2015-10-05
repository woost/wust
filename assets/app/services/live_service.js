angular.module("wust.services").provider("LiveService", LiveService);

LiveService.$inject = [];

function LiveService() {
    let baseUrl = "";
    this.setBaseUrl = url => baseUrl = url;

    this.$get = get;

    get.$inject = [];
    function get() {
        let url = currentUrl();
        let nodesSocket = new WebSocket(`${url}${baseUrl}/nodes`);

        return {
            registerNodes
        };

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
