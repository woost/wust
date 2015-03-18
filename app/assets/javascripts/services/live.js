angular.module("wust").service("Live", function() {
    this.subscribe = subscribe;

    var request = {
        //TODO: decide on atmosphere client configuration
        contentType: "application/json",
        trackMessageLength: true,
        shared: true,
        transport: "websocket",
        fallbackTransport: "long-polling"
    };

    request.onTransportFailure = function(errorMsg, request) {
        console.log("Atmosphere error: " + errorMsg);
    };

    request.onClose = function(response) {
        console.log("Atmosphere disconnected");
    };

    function subscribe(url, handler) {
        var newRequest = _.merge({
            url: location.origin + url,
            onMessage: handler,
            onOpen: response => {
                console.log("Atmosphere connected on " + url + " (" + response.transport + ")");
            }
        }, request);
        console.log("subscribing to " + url);
        atmosphere.subscribe(newRequest);
    }
});
