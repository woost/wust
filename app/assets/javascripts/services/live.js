angular.module("wust").provider("Live", function() {
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

    this.$get = $rootScope => {
        function subscribe(url, handler) {
            let newRequest = _.merge({
                url: location.origin + `${baseUrl}/${url}`,
                onMessage: response => {
                    let json = JSON.parse(response.responseBody);
                    console.log(json);
                    handler(json);
                },
                onOpen: response => {
                    console.log("Atmosphere connected on " + url + " (" + response.transport + ")");
                }
            }, request);

            console.log("subscribing to " + url);
            atmosphere.subscribe(newRequest);

            let deregisterEvent = $rootScope.$on("$stateChangeSuccess", () => {
                console.log("unsubscribing " + url);
                atmosphere.unsubscribe(newRequest);
                deregisterEvent();
            });
        }

        return {
            subscribe
        };
    };
});
