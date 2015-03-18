angular.module("wust").service("Live", function() {
    this.subscribe = subscribe;

    var request = {
        //TODO: current location
        url: "http://localhost:9000/live",
        contentType: "application/json",
        trackMessageLength: true,
        shared: true,
        transport: "websocket",
        fallbackTransport: "long-polling"
    };

    request.onOpen = function(response) {
        console.log("Atmosphere connected using " + response.transport);
    };

    request.onTransportFailure = function(errorMsg, request) {
        console.log(errorMsg);
    };

    request.onClose = function(response) {
        console.log("Atmosphere disconnected");
    };

    function subscribe(handler) {
        var newRequest = _.merge({
            onMessage: handler
        }, request);
        console.log("subscribing...");
        atmosphere.subscribe(newRequest);
    }
});
