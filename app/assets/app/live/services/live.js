angular.module("wust.live").provider("Live", Live);

Live.$inject = [];

function Live() {
    let request = {
        //TODO: decide on atmosphere client configuration
        contentType: "application/json",
        trackMessageLength: true,
        shared: true,
        transport: "websocket",
        fallbackTransport: "long-polling",
        onTransportFailure: (errorMsg, request) => console.log("Atmosphere error: " + errorMsg),
        onClose: response => console.log("Atmosphere disconnected")
    };

    let baseUrl = "";
    this.setBaseUrl = url => baseUrl = url;

    this.$get = get;

    get.$inject = [];
    function get() {
        function subscribe(url, handler) {
            let newRequest = _.merge({
                url: location.origin + `${baseUrl}/${url}`,
                onMessage,
                onOpen: response => {
                    console.log("Atmosphere connected on " + url + " (" + response.transport + ")");
                }
            }, request);

            console.log("subscribing to " + url);
            atmosphere.subscribe(newRequest);

            return unsubscribe;

            function unsubscribe() {
                console.log("unsubscribing " + url);
                atmosphere.unsubscribe(newRequest);
            }

            function onMessage(response) {
                try {
                    var json = JSON.parse(response.responseBody);
                    console.log("incoming message", json);
                    handler(json);
                } catch(e) {
                    console.warn(`illegal message received from atmosphere, will unsubscribe '${url}' now`, response, e);
                    unsubscribe();
                }
            }
        }

        return {
            subscribe
        };
    }
}
